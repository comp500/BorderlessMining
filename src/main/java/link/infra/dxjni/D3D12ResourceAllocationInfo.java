package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"SizeInBytes", "Alignment"})
public class D3D12ResourceAllocationInfo extends Structure {
	public WinDef.ULONGLONG SizeInBytes;
	public WinDef.ULONGLONG Alignment;
}
