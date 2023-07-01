package link.infra.dxjni;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public interface DXGILibrary extends Library {
	DXGILibrary INSTANCE = Native.load("dxgi", DXGILibrary.class);

	WinDef.UINT D3D_FEATURE_LEVEL_11_0 = new WinDef.UINT(0xb000);

	WinNT.HRESULT CreateDXGIFactory1(Guid.REFIID riid, PointerByReference factory);
}
