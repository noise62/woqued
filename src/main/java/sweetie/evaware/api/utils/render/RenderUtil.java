package sweetie.evaware.api.utils.render;

import lombok.experimental.UtilityClass;
import sweetie.evaware.api.utils.render.display.*;

@UtilityClass
public class RenderUtil {
    public RectRender RECT = new RectRender();
    public BlurRectRender BLUR_RECT = new BlurRectRender();
    public GradientRectRender GRADIENT_RECT = new GradientRectRender();
    public TextureRectRender TEXTURE_RECT = new TextureRectRender();

    public OtherRender OTHER = new OtherRender();
    public WorldRender WORLD = new WorldRender();
    public BoxRender BOX = new BoxRender();
}
