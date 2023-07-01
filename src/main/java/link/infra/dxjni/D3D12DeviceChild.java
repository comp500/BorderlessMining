package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;

public class D3D12DeviceChild extends Unknown {
	public D3D12DeviceChild(Pointer ptr) {
		super(ptr);
	}

	public D3D12DeviceChild() {
	}
}
