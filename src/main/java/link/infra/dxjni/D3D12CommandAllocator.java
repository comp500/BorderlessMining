package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;

public class D3D12CommandAllocator extends D3D12DeviceChild {
	public static final Guid.IID IID_ID3D12CommandAllocator = new Guid.IID("{6102dee4-af59-4b09-b999-b44d73f09b24}");

	public D3D12CommandAllocator(Pointer ptr) {
		super(ptr);
	}

	public D3D12CommandAllocator() {
		super();
	}
}
