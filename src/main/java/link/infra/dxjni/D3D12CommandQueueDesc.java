package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Type", "Priority", "Flags", "NodeMask"})
public class D3D12CommandQueueDesc extends Structure {
	public WinDef.UINT Type;
	public int Priority;
	public WinDef.UINT Flags;
	public WinDef.UINT NodeMask;
}
