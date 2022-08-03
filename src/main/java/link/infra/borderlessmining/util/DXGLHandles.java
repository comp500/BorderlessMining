package link.infra.borderlessmining.util;

import org.lwjgl.PointerBuffer;

public class DXGLHandles {

	public static long dxColorBuffer = 0;
	public static int colorRenderbuffer = 0;

	// TODO: move to a field on Window?
	public static long offscreenContext = 0;

	public static int targetFramebuffer = 0;
	public static long d3dDevice = 0;
	public static long d3dDeviceGl = 0;
	public static long d3dSwapchain = 0;
	public static long d3dContext = 0;
	public static PointerBuffer d3dTargets = PointerBuffer.allocateDirect(1);


	// TODO: refactor mixins for best practices (less code in mixins!)

}
