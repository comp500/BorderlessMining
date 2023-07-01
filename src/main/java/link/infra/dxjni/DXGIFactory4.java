package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class DXGIFactory4 extends Unknown {
	public static final Guid.IID IID_IDXGIFactory4 = new Guid.IID("{1bc6ea02-ef36-464f-bf0c-21ca39e5168a}");

	public DXGIFactory4(Pointer ptr) {
		super(ptr);
	}

	public WinNT.HRESULT CreateSwapChainForHwnd(D3D12CommandQueue commandQueue, WinDef.HWND hWnd, DXGISwapChainDesc1 desc, Pointer fullscreenDesc, Pointer restrictToOutput, PointerByReference swapchain) {
		return (WinNT.HRESULT) _invokeNativeObject(15, new Object[]{this.getPointer(), commandQueue, hWnd, desc, fullscreenDesc, restrictToOutput, swapchain}, WinNT.HRESULT.class);
	}
}
