package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

public class D3D12GraphicsCommandList extends D3D12DeviceChild {
	public static final Guid.IID IID_ID3D12GraphicsCommandList = new Guid.IID("{5b160d0f-ac1b-4185-8ba8-b3ae42a5a455}");

	public static final int D3D12_COMMAND_LIST_TYPE_DIRECT = 0;

	public D3D12GraphicsCommandList(Pointer ptr) {
		super(ptr);
	}

	public D3D12GraphicsCommandList() {
		super();
	}

	public WinNT.HRESULT Close() {
		return (WinNT.HRESULT) _invokeNativeObject(9, new Object[]{this.getPointer()}, WinNT.HRESULT.class);
	}

	public void ResourceBarrier(D3D12ResourceBarrier[] pBarriers) {
		_invokeNativeObject(26, new Object[]{this.getPointer(), new WinDef.UINT(pBarriers.length), pBarriers}, void.class);
	}
}
