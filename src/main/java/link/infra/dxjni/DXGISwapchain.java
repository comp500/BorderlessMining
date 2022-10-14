package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class DXGISwapchain extends Unknown {
	public DXGISwapchain(Pointer ptr) {
		super(ptr);
	}

	// TODO: JNA used; could replace with dyncall (1.18-) / libffi (1.19+) for potentially better performance?
	public WinNT.HRESULT Present(WinDef.UINT SyncInterval, WinDef.UINT Flags) {
		return (WinNT.HRESULT) _invokeNativeObject(8, new Object[]{this.getPointer(), SyncInterval, Flags}, WinNT.HRESULT.class);
	}

	public WinNT.HRESULT GetBuffer(WinDef.UINT Buffer, Guid.REFIID riid, PointerByReference ppSurface) {
		return (WinNT.HRESULT) _invokeNativeObject(9, new Object[]{this.getPointer(), Buffer, riid, ppSurface}, WinNT.HRESULT.class);
	}

	public WinNT.HRESULT ResizeBuffers(WinDef.UINT BufferCount, WinDef.UINT Width, WinDef.UINT Height, WinDef.UINT NewFormat, WinDef.UINT SwapChainFlags) {
		return (WinNT.HRESULT) _invokeNativeObject(13, new Object[]{this.getPointer(), BufferCount, Width, Height, NewFormat, SwapChainFlags}, WinNT.HRESULT.class);
	}

}
