package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(Framebuffer.class)
public class DXGLFramebufferMixin {
	@Shadow protected int colorAttachment;

	@Shadow public int viewportWidth;

	@Shadow public int textureWidth;

	@Shadow public int viewportHeight;

	@Shadow public int textureHeight;

	/**
	 * @author me
	 */
	@Overwrite
	private void drawInternal(int width, int height, boolean disableBlend) {
		RenderSystem.assertOnRenderThread();
		GlStateManager._colorMask(true, true, true, false);
		GlStateManager._disableDepthTest();
		GlStateManager._depthMask(false);
		GlStateManager._viewport(0, 0, width, height);
		if (disableBlend) {
			GlStateManager._disableBlend();
		}
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		Shader shader = minecraftClient.gameRenderer.blitScreenShader;
		shader.addSampler("DiffuseSampler", this.colorAttachment);
		Matrix4f matrix4f = Matrix4f.projectionMatrix(width, -height, 1000.0f, 3000.0f);
		RenderSystem.setProjectionMatrix(matrix4f);
		if (shader.modelViewMat != null) {
			shader.modelViewMat.set(Matrix4f.translate(0.0f, 0.0f, -2000.0f));
		}
		if (shader.projectionMat != null) {
			shader.projectionMat.set(matrix4f);
		}
		shader.bind();
		float f = width;
		float g = height;
		float texWidth = (float)this.viewportWidth / (float)this.textureWidth;
		float texHeight = (float)this.viewportHeight / (float)this.textureHeight;
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		bufferBuilder.vertex(0.0, g, 0.0).texture(0.0f, texHeight).color(255, 255, 255, 255).next();
		bufferBuilder.vertex(f, g, 0.0).texture(texWidth, texHeight).color(255, 255, 255, 255).next();
		bufferBuilder.vertex(f, 0.0, 0.0).texture(texWidth, 0.0f).color(255, 255, 255, 255).next();
		bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
		bufferBuilder.end();
		BufferRenderer.postDraw(bufferBuilder);
		shader.unbind();
		GlStateManager._depthMask(true);
		GlStateManager._colorMask(true, true, true, true);
	}
}
