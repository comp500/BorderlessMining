package link.infra.borderlessmining.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.util.DXGLWindowHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public abstract class DXGLWindowMixin implements DXGLWindowHooks {
	// TODO: synthetic? prefixed?
	private DXGLWindow dxglWindow;

	@Override
	public DXGLWindow dxgl_getOffscreenContext() {
		return dxglWindow;
	}

	@Shadow private boolean vsync;

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
	private void beforeCreatingWindow(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		// Create invisible window with existing configured hints
		dxglWindow = new DXGLWindow((Window) (Object) this);
		// Reset hints; make primary window be created with no API
		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
	}

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
	private void onMakeContextCurrent(long window) {
		GLFW.glfwDefaultWindowHints();
		// TODO: check what these do!
//		GLFW.glfwWindowHint(139265, 196609);
//		GLFW.glfwWindowHint(139275, 221185);
//		GLFW.glfwWindowHint(139266, 3);
//		GLFW.glfwWindowHint(139267, 2);
//		GLFW.glfwWindowHint(139272, 204801);
//		GLFW.glfwWindowHint(139270, 1);

		dxglWindow.makeCurrent();
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void afterConstruction(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
		dxglWindow.initGL();
	}

	@Redirect(method = "swapBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V"))
	private void onFlipFrame(long window) {
		GLFW.glfwPollEvents();
		RenderSystem.replayQueue();
		Tessellator.getInstance().getBuffer().clear();
		dxglWindow.present(vsync);
		GLFW.glfwPollEvents();
	}

	@Inject(method = "onFramebufferSizeChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/WindowEventHandler;onResolutionChanged()V"))
	private void onOnFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
		dxglWindow.resize(width, height);
	}

	// TODO: teardown
}
