package link.infra.dxjni;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DXJNIShim {

	// TODO: make this shared?
	private static final Logger LOGGER = LogManager.getLogger("BorderlessMining 2?");

	// TODO: EnumAdapters to determine an adapter compatible with the OGL one?
	// TODO: EnumAdapterByGpuPreference is better :)
	// TODO: EXT_external_objects_win32/GL_EXT_memory_object_win32 can be used to get DEVICE_LUID_EXT
	// TODO: check support of WGL_NV_DX_interop2 GL extension
	// TODO: idle state w/Present1? could be complicated...

	// TODO: setfullscreenstate? could be useful if people want exclusive fullscreen as an option
	// TODO: makewindowassociation, to ensure DXGI doesn't interfere with GLFW

	public static final int DXGI_PRESENT_RESTART = 0x00000004;
	public static final int DXGI_PRESENT_ALLOW_TEARING = 0x00000200;

}
