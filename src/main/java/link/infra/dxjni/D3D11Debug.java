package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class D3D11Debug extends Unknown {
	public static final Guid.IID IID_IDXGIDebug = new Guid.IID("{79cf2233-7536-4948-9d36-1e4692dc5760}");

	public D3D11Debug(Pointer ptr) {
		super(ptr);
	}

	public static D3D11Debug fromDevice(D3D11Device device) {
		PointerByReference chain3 = new PointerByReference();
		if (COMUtils.SUCCEEDED(device.QueryInterface(new Guid.REFIID(IID_IDXGIDebug), chain3))) {
			return new D3D11Debug(chain3.getValue());
		}
		return null;
	}

	public WinNT.HRESULT ReportLiveDeviceObjects(WinDef.UINT flags) {
		return (WinNT.HRESULT) _invokeNativeObject(10, new Object[]{this.getPointer(), flags}, WinNT.HRESULT.class);
	}
}
