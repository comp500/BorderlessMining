package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import com.sun.jna.Pointer;
import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import link.infra.dxjni.DXGISwapchain3;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.WGLNVDXInterop;

public abstract class DXBackbuffer extends DXGLWindow {
	public int colorRenderbuffer1;
	public int targetFramebuffer1;
	public final PointerBuffer d3dTargets1 = PointerBuffer.allocateDirect(1);
	public int colorRenderbuffer2;
	public int targetFramebuffer2;
	public final PointerBuffer d3dTargets2 = PointerBuffer.allocateDirect(1);

	public DXBackbuffer(Window parent, DXGLWindowSettings settings) {
		super(parent, settings);
	}

	@Override
	protected void setupGlBuffers() {
		targetFramebuffer1 = GL32C.glGenFramebuffers();
		colorRenderbuffer1 = GL32C.glGenRenderbuffers();
		targetFramebuffer2 = GL32C.glGenFramebuffers();
		colorRenderbuffer2 = GL32C.glGenRenderbuffers();
	}

	@Override
	protected void freeGlBuffers() {
		GL32C.glDeleteRenderbuffers(colorRenderbuffer1);
		GL32C.glDeleteFramebuffers(targetFramebuffer1);
		GL32C.glDeleteRenderbuffers(colorRenderbuffer2);
		GL32C.glDeleteFramebuffers(targetFramebuffer2);
	}

	@Override
	protected void registerBackbuffer() {
		System.out.println("trying to register " + Pointer.nativeValue(dxColorBackbuffer1.getPointer()));

		// Register d3d backbuffer as an OpenGL renderbuffer
		// TODO: try registering fewer times; rolling buffer to mimic swapchain behaviour? check usage flags and how they change?
		d3dTargets1.put(WGLNVDXInterop.wglDXRegisterObjectNV(
			d3dDeviceGl, Pointer.nativeValue(dxColorBackbuffer1.getPointer()), colorRenderbuffer1,
			GL32C.GL_RENDERBUFFER, WGLNVDXInterop.WGL_ACCESS_WRITE_DISCARD_NV));
		d3dTargets1.flip();

		if (d3dTargets1.get(0) == 0) {
			throw new IllegalStateException("Failed to register backbuffer 1");
		}

		// Attach d3d backbuffer to OpenGL framebuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer1);
		GL32C.glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_RENDERBUFFER, colorRenderbuffer1);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);

		// Register d3d backbuffer as an OpenGL renderbuffer
		// TODO: try registering fewer times; rolling buffer to mimic swapchain behaviour? check usage flags and how they change?
		d3dTargets2.put(WGLNVDXInterop.wglDXRegisterObjectNV(
			d3dDeviceGl, Pointer.nativeValue(dxColorBackbuffer2.getPointer()), colorRenderbuffer2,
			GL32C.GL_RENDERBUFFER, WGLNVDXInterop.WGL_ACCESS_WRITE_DISCARD_NV));
		d3dTargets2.flip();

		if (d3dTargets2.get(0) == 0) {
			throw new IllegalStateException("Failed to register backbuffer 2");
		}

		// Attach d3d backbuffer to OpenGL framebuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer2);
		GL32C.glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, GL32C.GL_RENDERBUFFER, colorRenderbuffer2);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);

		System.out.println("registering: " + d3dTargets1.get(0) + " " + d3dTargets2.get(0));

		time = 0;
	}

	@Override
	protected void unregisterBackbuffer() {
		new Exception("unregistering, time=" + time).printStackTrace();
		WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets1.get());
		d3dTargets1.flip();
		WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets2.get());
		d3dTargets2.flip();
	}

	@Override
	protected void bindBackbuffer() {
		if (time++ % 2 == 0) {
			WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets1);
			GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer1);

			System.out.println(DXGISwapchain3.fromSwapchain(d3dSwapchain).GetCurrentBackBufferIndex() + " " + Pointer.nativeValue(dxColorBackbuffer1.getPointer()) + " " + d3dTargets1.get(0));
		} else {
			WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets2);
			GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer2);

			System.out.println(DXGISwapchain3.fromSwapchain(d3dSwapchain).GetCurrentBackBufferIndex() + " " + Pointer.nativeValue(dxColorBackbuffer2.getPointer()) + " " + d3dTargets2.get(0));
		}
	}

	@Override
	protected void unbindBackbuffer() {
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		if (time % 2 == 1) {
			WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets1);
		} else {
			WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets2);
		}
	}
}
