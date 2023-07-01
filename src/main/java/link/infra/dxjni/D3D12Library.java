package link.infra.dxjni;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public interface D3D12Library extends Library {
	D3D12Library INSTANCE = Native.load("d3d12", D3D12Library.class);

	WinDef.UINT D3D_FEATURE_LEVEL_11_0 = new WinDef.UINT(0xb000);

	WinNT.HRESULT D3D12CreateDevice(Pointer adapter, WinDef.UINT minimumFeatureLevel, Guid.REFIID riid, PointerByReference device);
}
