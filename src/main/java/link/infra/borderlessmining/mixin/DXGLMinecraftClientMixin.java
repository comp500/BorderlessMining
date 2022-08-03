package link.infra.borderlessmining.mixin;

import link.infra.borderlessmining.util.DXGLHandles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.WGLNVDXInterop;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class DXGLMinecraftClientMixin {
	@Shadow @Final private Window window;

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"))
	private void draw(Framebuffer instance, int width, int height) {
		// TODO: waitable object?

		// Register d3d backbuffer as an OpenGL renderbuffer
		DXGLHandles.d3dTargets.put(WGLNVDXInterop.wglDXRegisterObjectNV(
			DXGLHandles.d3dDeviceGl, DXGLHandles.dxColorBuffer, DXGLHandles.colorRenderbuffer,
			GL32C.GL_RENDERBUFFER, WGLNVDXInterop.WGL_ACCESS_WRITE_DISCARD_NV));
		DXGLHandles.d3dTargets.flip();

		// Set up framebuffer and attach d3d backbuffer
		WGLNVDXInterop.wglDXLockObjectsNV(DXGLHandles.d3dDeviceGl, DXGLHandles.d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, DXGLHandles.targetFramebuffer);
		GL32C.glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_RENDERBUFFER, DXGLHandles.colorRenderbuffer);

		// Draw frame
		instance.draw(width, height);

		// Unwind: unbind the framebuffer, unlock and unregister backbuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		WGLNVDXInterop.wglDXUnlockObjectsNV(DXGLHandles.d3dDeviceGl, DXGLHandles.d3dTargets);
		WGLNVDXInterop.wglDXUnregisterObjectNV(DXGLHandles.d3dDeviceGl, DXGLHandles.d3dTargets.get());
		DXGLHandles.d3dTargets.flip();
	}
}
