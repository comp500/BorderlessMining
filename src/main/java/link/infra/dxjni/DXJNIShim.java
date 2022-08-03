package link.infra.dxjni;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DXJNIShim {

	// TODO: make this shared?
	private static final Logger LOGGER = LogManager.getLogger("BorderlessMining 2?");

//	static {
//		Path dllCopyPath = Paths.get(".borderlessmining", "DXJNIShim.dll");
//
//		try {
//			Files.createDirectories(dllCopyPath.getParent());
//			InputStream stream = GLFWReplace.class.getClassLoader().getResourceAsStream("borderlessmining/DXJNIShim.dll");
//			if (stream == null) {
//				LOGGER.error("Failed to read DXJNIShim dll");
//			} else {
//				Files.copy(stream, dllCopyPath, StandardCopyOption.REPLACE_EXISTING);
//				System.load(dllCopyPath.toAbsolutePath().toString());
//			}
//		} catch (IOException e) {
//			LOGGER.error("Failed to copy DXJNIShim dll", e);
//		}
//	}

	// Swapchain flags
	public static final int DXGI_SWAP_CHAIN_FLAG_FRAME_LATENCY_WAITABLE_OBJECT = 64; // TODO
	public static final int DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING = 2048; // TODO

	// TODO: EnumAdapters to determine an adapter compatible with the OGL one?
	// TODO: EnumAdapterByGpuPreference is better :)
	// TODO: EXT_external_objects_win32/GL_EXT_memory_object_win32 can be used to get DEVICE_LUID_EXT
	// TODO: check support of WGL_NV_DX_interop2 GL extension
	// TODO: idle state w/Present1? could be complicated...

	// TODO: setfullscreenstate? could be useful if people want exclusive fullscreen as an option
	// TODO: makewindowassociation, to ensure DXGI doesn't interfere with GLFW

	// TODO: get rid of DXJNIShim (was used for shim DLL, now using JNA)

	//public static native long DXGISwapChainPresent(long swapChain, int syncInterval, int flags);
	public static long DXGISwapChainPresent(long swapChain, int syncInterval, int flags) {
		return new DXGISwapchain(new Pointer(swapChain)).Present(new WinDef.UINT(syncInterval), new WinDef.UINT(flags)).longValue();
	}

	//public static native long DXGISwapChainGetBufferTexture2D(long swapChain, int idx, long texture);
	public static long DXGISwapChainGetBufferTexture2D(long swapChain, int idx, long texture) {
		return new DXGISwapchain(new Pointer(swapChain)).GetBuffer(new WinDef.UINT(idx),
			new Guid.REFIID(D3D11Texture2D.IID_ID3D11Texture2D), new Pointer(texture)).longValue();
	}

	//public static native long DXGISwapChainResizeBuffers(long swapChain, int bufferCount, int width, int height, int format, int flags);
	public static long DXGISwapChainResizeBuffers(long swapChain, int bufferCount, int width, int height, int format, int flags) {
		return new DXGISwapchain(new Pointer(swapChain)).ResizeBuffers(new WinDef.UINT(bufferCount), new WinDef.UINT(width), new WinDef.UINT(height),
			new WinDef.UINT(DXGI_FORMAT_R8G8B8A8_UNORM), new WinDef.UINT(flags)).longValue();
	}

	public static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;
	public static final int DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x00000020;
	public static final int DXGI_SWAP_EFFECT_FLIP_DISCARD = 4;

//  public static native long D3D11CreateDeviceAndSwapChain(long adapter, int driverType, long software, int flags,
//															long featureLevels, int numFeatureLevels, long swapChainDesc,
//															long swapChain, long device, long featureLevel, long context);

	public interface D3D11Library extends Library {
		D3D11Library INSTANCE = Native.load("d3d11", D3D11Library.class);

		// TODO: use proper JNA types?

		long D3D11CreateDeviceAndSwapChain(long adapter, int driverType, long software, int flags,
										   long featureLevels, int numFeatureLevels, int sdkVersion, long swapChainDesc,
										   long swapChain, long device, long featureLevel, long context);
	}

	public static final int D3D11_SDK_VERSION = 7;

	public static long D3D11CreateDeviceAndSwapChain(long adapter, int driverType, long software, int flags,
															long featureLevels, int numFeatureLevels, long swapChainDesc,
															long swapChain, long device, long featureLevel, long context) {
		return D3D11Library.INSTANCE.D3D11CreateDeviceAndSwapChain(adapter, driverType, software, flags, featureLevels,
			numFeatureLevels, D3D11_SDK_VERSION, swapChainDesc, swapChain, device, featureLevel, context);
	}

	//public static native long __ID3D11Texture2DRelease(long texture);
	public static long ID3D11Texture2DRelease(long texture) {
		return new Unknown(new Pointer(texture)).Release();
	}

	public static final int D3D_DRIVER_TYPE_HARDWARE = 1;
	public static final int D3D11_CREATE_DEVICE_DEBUG = 2;
}
