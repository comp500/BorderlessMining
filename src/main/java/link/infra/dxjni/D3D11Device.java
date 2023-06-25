package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class D3D11Device extends Unknown {
	public D3D11Device(Pointer ptr) {
		super(ptr);
	}

	// TODO: JNA used; could replace with dyncall (1.18-) / libffi (1.19+) for potentially better performance?
	public WinNT.HRESULT CreateRenderTargetView(D3D11Texture2D pResource, Pointer pDesc, PointerByReference ppRTView) {
		return (WinNT.HRESULT) _invokeNativeObject(9, new Object[]{this.getPointer(), pResource.getPointer(), pDesc, ppRTView}, WinNT.HRESULT.class);
	}

}
