package worst.woqued.client.features.modules.render.chinahat;

import worst.woqued.api.event.events.render.Render3DEvent;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class ChinaHatRenderer {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final ChinaHatGeometry geometry = new ChinaHatGeometry();
    private final ChinaHatMaterialResolver materialResolver = new ChinaHatMaterialResolver();
    private final ChinaHatLighting lighting = new ChinaHatLighting();
    private final CameraAdapter cameraAdapter = new DefaultCameraAdapter();

    public void render(Render3DEvent.Render3DEventData event, ChinaHatSettings settings, List<PlayerEntity> targets) {
        if (client.world == null || client.player == null || targets.isEmpty()) {
            return;
        }

        MatrixStack matrices = event.matrixStack();
        Vec3d cameraPos = cameraAdapter.getCameraPos(client);
        Identifier texture = materialResolver.resolve(settings.materialMode());

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        RenderSystem.setShaderTexture(0, texture);
        TextureFilter previousFilter = applyTextureFiltering(settings.antialiasing());
        boolean lineSmoothWasEnabled = GL11.glIsEnabled(GL11.GL_LINE_SMOOTH);

        if (settings.antialiasing()) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
        }

        try {
            for (PlayerEntity player : targets) {
                ChinaHatPose pose = resolvePose(player, event.partialTicks());
                if (pose == null) {
                    continue;
                }

                float distance = (float) cameraPos.distanceTo(pose.hatWorldPos());
                int countX = settings.highPolygonal() && distance < 24f ? 60 : 30;
                int countY = settings.highPolygonal() && distance < 24f ? 12 : 7;

                ChinaHatGeometry.MeshProfile profile = new ChinaHatGeometry.MeshProfile(
                        pose.hatWidth(), pose.hatHeight(), countX, countY
                );
                ChinaHatGeometry.ChinaHatMesh mesh = geometry.getMesh(profile);
                ChinaHatLighting.HatTint tint = lighting.resolve(player, pose.hatWorldPos(), 0.92f);

                renderPlayerHat(matrices, pose, mesh, tint, distance);
            }
        } finally {
            if (settings.antialiasing()) {
                if (lineSmoothWasEnabled) {
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                } else {
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                }
            }

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(1f);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            restoreTextureFiltering(previousFilter);
            matrices.pop();
        }
    }

    private void renderPlayerHat(MatrixStack matrices,
                                 ChinaHatPose pose,
                                 ChinaHatGeometry.ChinaHatMesh mesh,
                                 ChinaHatLighting.HatTint tint,
                                 float distance) {
        matrices.push();
        matrices.translate(pose.baseWorldPos().x, pose.baseWorldPos().y, pose.baseWorldPos().z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - pose.yaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitch()));
        matrices.translate(0f, pose.localYOffset(), pose.localZOffset());

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder surface = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
        int alpha = Math.max(0, Math.min(255, (int) (tint.alpha() * 255f)));

        for (ChinaHatGeometry.Triangle triangle : mesh.triangles()) {
            vertex(surface, matrix, triangle.first(), tint, alpha);
            vertex(surface, matrix, triangle.second(), tint, alpha);
            vertex(surface, matrix, triangle.third(), tint, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(surface.end());

        float lineWidth = Math.max(0.025f, 0.025f + 9.5f * MathHelper.clamp(1f - distance / 7f, 0f, 1f));
        RenderSystem.lineWidth(Math.min(lineWidth, 6.0f));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int outlineAlpha = Math.max(38, Math.min(255, alpha + 24));
        BufferBuilder outlines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (ChinaHatGeometry.LineLoop loop : mesh.loops()) {
            List<ChinaHatGeometry.MeshVertex> vertices = loop.vertices();
            for (int i = 0; i < vertices.size() - 1; i++) {
                line(outlines, matrix, vertices.get(i), vertices.get(i + 1), tint, outlineAlpha);
            }
        }
        for (ChinaHatGeometry.LineSegment spoke : mesh.spokes()) {
            line(outlines, matrix, spoke.start(), spoke.end(), tint, Math.max(24, outlineAlpha - 20));
        }
        BufferRenderer.drawWithGlobalProgram(outlines.end());

        matrices.pop();
    }

    private void vertex(BufferBuilder buffer,
                        Matrix4f matrix,
                        ChinaHatGeometry.MeshVertex vertex,
                        ChinaHatLighting.HatTint tint,
                        int alpha) {
        float sliceShade = 0.9f + (1f - vertex.slicePc()) * 0.1f;
        buffer.vertex(matrix, vertex.x(), vertex.y(), vertex.z())
                .texture(vertex.u(), vertex.v())
                .color(
                        Math.min(255, (int) (tint.red() * sliceShade * 255f)),
                        Math.min(255, (int) (tint.green() * sliceShade * 255f)),
                        Math.min(255, (int) (tint.blue() * sliceShade * 255f)),
                        alpha
                );
    }

    private void line(BufferBuilder buffer,
                      Matrix4f matrix,
                      ChinaHatGeometry.MeshVertex first,
                      ChinaHatGeometry.MeshVertex second,
                      ChinaHatLighting.HatTint tint,
                      int alpha) {
        int red = Math.min(255, (int) (tint.red() * 255f));
        int green = Math.min(255, (int) (tint.green() * 255f));
        int blue = Math.min(255, (int) (tint.blue() * 255f));
        buffer.vertex(matrix, first.x(), first.y(), first.z()).color(red, green, blue, alpha);
        buffer.vertex(matrix, second.x(), second.y(), second.z()).color(red, green, blue, alpha);
    }

    private TextureFilter applyTextureFiltering(boolean antialiasing) {
        TextureFilter previous = new TextureFilter(
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER),
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER)
        );
        int filter = antialiasing ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        return previous;
    }

    private void restoreTextureFiltering(TextureFilter filter) {
        if (filter == null) {
            return;
        }

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter.minFilter());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter.magFilter());
    }

    private ChinaHatPose resolvePose(PlayerEntity player, float tickDelta) {
        boolean self = player == client.player;
        boolean firstPersonSelf = self && cameraAdapter.isFirstPerson(client);
        boolean child = player.isBaby();

        float hatHeight = child ? 0.15f : 0.25f;
        float hatWidth = child ? 0.35f : 0.5f;
        float hatPitchExt = child ? 0.07f : 0.235f;

        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float rawPitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        float yaw = headYaw;
        float pitch = MathHelper.clamp(rawPitch, -60f, 60f) * 0.24f;

        float eyeHeight = player.getEyeHeight(player.getPose());
        float pitchRadians = rawPitch * MathHelper.RADIANS_PER_DEGREE;
        float upperHeadOffset = child ? 0.272f : 0.262f;
        float sneakOffset = player.isSneaking() ? (child ? -0.01f : -0.03f) : 0f;
        float headTopOffset = eyeHeight + upperHeadOffset + sneakOffset;
        headTopOffset += Math.abs(MathHelper.sin(pitchRadians)) * hatPitchExt * 0.16f;

        float hurtProgress = MathHelper.clamp((player.hurtTime - tickDelta) / 10f, 0f, 1f);
        float hurtEase = ChinaHatGeometry.easeInOutExpo(hurtProgress);
        float wobblePitch = !firstPersonSelf ? MathHelper.sin(hurtProgress * MathHelper.PI) * 7.5f * hurtEase : 0f;
        float wobbleY = !firstPersonSelf ? MathHelper.sin(hurtProgress * MathHelper.PI) * 0.045f * hurtEase : 0f;

        float localYOffset = child ? 0.016f : 0.03f;
        localYOffset += Math.abs(MathHelper.sin(pitchRadians)) * hatPitchExt * 0.12f;
        float localZOffset = -MathHelper.sin(pitchRadians) * hatPitchExt * 0.62f;
        if (player.isSneaking()) {
            localYOffset += 0.01f;
            localZOffset += 0.01f;
        }
        if (firstPersonSelf) {
            headTopOffset -= 0.012f;
            localYOffset -= 0.004f;
            localZOffset -= 0.008f;
        }

        Vec3d base = new Vec3d(x, y + headTopOffset + wobbleY, z);
        Vec3d hatWorldPos = base.add(0d, localYOffset, 0d);
        return new ChinaHatPose(base, hatWorldPos, hatWidth, hatHeight, yaw, pitch + wobblePitch, localYOffset, localZOffset);
    }

    private record ChinaHatPose(Vec3d baseWorldPos,
                                Vec3d hatWorldPos,
                                float hatWidth,
                                float hatHeight,
                                float yaw,
                                float pitch,
                                float localYOffset,
                                float localZOffset) {
    }

    private record TextureFilter(int minFilter, int magFilter) {
    }
}
