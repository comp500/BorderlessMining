package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;

public class DXGIAdapter extends Unknown {
	public static final Guid.IID IID_IDXGIAdapter = new Guid.IID("{2411e7e1-12ac-4ccf-bd14-9798e8534dc0}");

	public DXGIAdapter() {
		super();
	}

	public DXGIAdapter(Pointer ptr) {
		super(ptr);
	}

	public WinNT.HRESULT GetDesc(DXGIAdapterDesc pDesc) {
		return (WinNT.HRESULT) _invokeNativeObject(8, new Object[]{this.getPointer(), pDesc}, WinNT.HRESULT.class);
	}
}
