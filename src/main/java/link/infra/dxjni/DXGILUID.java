package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

/**
 * WinNT.LUID is pass-by-reference, so we use a custom interface
 */
@Structure.FieldOrder({"LowPart", "HighPart"})
public class DXGILUID extends Structure implements Structure.ByValue {
	public WinDef.DWORD LowPart;
	public WinDef.LONG HighPart;
}
