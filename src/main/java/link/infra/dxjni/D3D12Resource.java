package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;

public class D3D12Resource extends D3D12DeviceChild {
	public static final Guid.IID IID_ID3D12Resource = new Guid.IID("{696442be-a72e-4059-bc79-5b5c98040fad}");

	public D3D12Resource(Pointer ptr) {
		super(ptr);
	}

	public D3D12Resource() {
		super();
	}

	public D3D12ResourceDesc GetDesc(D3D12ResourceDesc retVal) {
		return (D3D12ResourceDesc) _invokeNativeObject(10, new Object[]{this.getPointer(), retVal}, D3D12ResourceDesc.class);
	}
}
