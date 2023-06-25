package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.PointerByReference;

public class DXGISwapchain3 extends Unknown {
	public static final Guid.IID IID_IDXGISwapChain3 = new Guid.IID("{94d99bdb-f1f8-4ab0-b236-7da0170edab1}");

	public DXGISwapchain3(Pointer ptr) {
		super(ptr);
	}

	public static DXGISwapchain3 fromSwapchain(DXGISwapchain swapchain) {
		PointerByReference chain3 = new PointerByReference();
		if (COMUtils.SUCCEEDED(swapchain.QueryInterface(new Guid.REFIID(IID_IDXGISwapChain3), chain3))) {
			return new DXGISwapchain3(chain3.getValue());
		}
		return null;
	}

	public WinDef.UINT GetCurrentBackBufferIndex() {
		return (WinDef.UINT) _invokeNativeObject(36, new Object[]{this.getPointer()}, WinDef.UINT.class);
	}
}
