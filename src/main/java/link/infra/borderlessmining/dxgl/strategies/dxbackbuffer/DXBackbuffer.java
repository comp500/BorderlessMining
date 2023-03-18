package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import com.sun.jna.Pointer;
import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.WGLNVDXInterop;

public abstract class DXBackbuffer extends DXGLWindow {
	public int colorRenderbuffer;
	public int targetFramebuffer;
	public final PointerBuffer d3dTargets = PointerBuffer.allocateDirect(1);

	public DXBackbuffer(Window parent, DXGLWindowSettings settings) {
		super(parent, settings);
	}

	@Override
	protected void setupGlBuffers() {
		targetFramebuffer = GL32C.glGenFramebuffers();
		colorRenderbuffer = GL32C.glGenRenderbuffers();
	}

	@Override
	protected void freeGlBuffers() {
		GL32C.glDeleteRenderbuffers(colorRenderbuffer);
		GL32C.glDeleteFramebuffers(colorRenderbuffer);
	}

	@Override
	protected void registerBackbuffer() {
		// Register d3d backbuffer as an OpenGL renderbuffer
		// TODO: try registering fewer times; rolling buffer to mimic swapchain behaviour? check usage flags and how they change?
		d3dTargets.put(WGLNVDXInterop.wglDXRegisterObjectNV(
			d3dDeviceGl, Pointer.nativeValue(dxColorBackbuffer.getPointer()), colorRenderbuffer,
			GL32C.GL_RENDERBUFFER, WGLNVDXInterop.WGL_ACCESS_WRITE_DISCARD_NV));
		d3dTargets.flip();

		// Attach d3d backbuffer to OpenGL framebuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer);
		GL32C.glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_RENDERBUFFER, colorRenderbuffer);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
	}

	@Override
	protected void unregisterBackbuffer() {
		WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets.get());
		d3dTargets.flip();
	}

	@Override
	protected void bindBackbuffer() {
		WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer);
	}

	@Override
	protected void unbindBackbuffer() {
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets);
	}
}
