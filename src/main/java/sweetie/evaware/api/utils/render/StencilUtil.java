package sweetie.evaware.api.utils.render;

import lombok.experimental.UtilityClass;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.GL11;
import sweetie.evaware.api.system.interfaces.QuickImports;

@UtilityClass
public class StencilUtil implements QuickImports {
    public void push() {
        var framebuffer = mc.getFramebuffer();
        if (framebuffer.depthAttachment > -1) {
            mc.getFramebuffer().beginWrite(false);

            EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthAttachment);
            final int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, mc.getWindow().getWidth(), mc.getWindow().getHeight());
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID);
            framebuffer.depthAttachment = -1;
        }

        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColorMask(false, false, false, false);
    }

    public void read(int ref) {
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, ref, 1);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public void pop() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
}
