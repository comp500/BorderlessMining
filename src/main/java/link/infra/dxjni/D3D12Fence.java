package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

public class D3D12Fence extends D3D12DeviceChild {
	public static final Guid.IID IID_ID3D12Fence = new Guid.IID("{0a753dcf-c4d8-4b91-adf6-be5a60d95a76}");

	public D3D12Fence(Pointer ptr) {
		super(ptr);
	}

	public D3D12Fence() {
		super();
	}

	public WinDef.ULONGLONG GetCompletedValue() {
		return (WinDef.ULONGLONG) _invokeNativeObject(8, new Object[]{this.getPointer()}, WinDef.ULONGLONG.class);
	}

	public WinNT.HRESULT SetEventOnCompletion(WinDef.ULONGLONG value, WinNT.HANDLE fenceEvent) {
		return (WinNT.HRESULT) _invokeNativeObject(9, new Object[]{this.getPointer(), value, fenceEvent}, WinNT.HRESULT.class);
	}
}
