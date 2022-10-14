package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Numerator", "Denominator"})
public class DXGIRational extends Structure {
	public WinDef.UINT Numerator, Denominator;
}
