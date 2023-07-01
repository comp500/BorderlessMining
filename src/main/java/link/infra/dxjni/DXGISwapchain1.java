package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class DXGISwapchain1 extends DXGISwapchain {
	public DXGISwapchain1(Pointer ptr) {
		super(ptr);
	}

	public WinNT.HRESULT GetDesc1(PointerByReference desc) {
		return (WinNT.HRESULT) _invokeNativeObject(18, new Object[]{this.getPointer(), desc}, WinNT.HRESULT.class);
	}
}
