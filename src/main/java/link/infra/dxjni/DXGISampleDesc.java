package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Count", "Quality"})
public class DXGISampleDesc extends Structure {
	public WinDef.UINT Count;
	public WinDef.UINT Quality;
}
