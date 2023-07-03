package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class DXGIDevice extends Unknown {
	public static final Guid.IID IID_IDXGIDevice = new Guid.IID("{54ec77fa-1377-44e6-8c32-88fd5f44c84c}");

	public DXGIDevice(Pointer ptr) {
		super(ptr);
	}

	// Only works with D3D11, not D3D12!
//	public static DXGIDevice fromD3D(Unknown device) {
//		PointerByReference dxgiDevice = new PointerByReference();
//		if (COMUtils.SUCCEEDED(device.QueryInterface(new Guid.REFIID(IID_IDXGIDevice), dxgiDevice))) {
//			return new DXGIDevice(dxgiDevice.getValue());
//		}
//		throw new IllegalStateException("Failed to upcast to DXGIDevice");
//	}

	public WinNT.HRESULT GetAdapter(PointerByReference pAdapter) {
		return (WinNT.HRESULT) _invokeNativeObject(7, new Object[]{this.getPointer(), pAdapter}, WinNT.HRESULT.class);
	}
}
