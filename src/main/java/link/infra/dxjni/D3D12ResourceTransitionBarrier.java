package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"pResource", "Subresource", "StateBefore", "StateAfter"})
public class D3D12ResourceTransitionBarrier extends Structure {
	public D3D12Resource pResource;
	public WinDef.UINT Subresource;
	public WinDef.UINT StateBefore;
	public WinDef.UINT StateAfter;
}
