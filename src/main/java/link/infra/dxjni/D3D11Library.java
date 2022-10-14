package link.infra.dxjni;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public interface D3D11Library extends Library {
	D3D11Library INSTANCE = Native.load("d3d11", D3D11Library.class);

	WinDef.UINT D3D11_SDK_VERSION = new WinDef.UINT(7);

	int D3D_DRIVER_TYPE_HARDWARE = 1;
	int D3D11_CREATE_DEVICE_DEBUG = 2;

	WinNT.HRESULT D3D11CreateDeviceAndSwapChain(Pointer adapter, WinDef.UINT driverType, WinDef.HMODULE software, WinDef.UINT flags,
												Pointer featureLevels, WinDef.UINT numFeatureLevels, WinDef.UINT sdkVersion, DXGISwapChainDesc swapChainDesc,
												PointerByReference swapChain, PointerByReference device, WinDef.UINTByReference featureLevel, PointerByReference context);
}
