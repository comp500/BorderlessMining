package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

/**
 * WinNT.LUID is pass-by-reference only, so we use a custom interface
 */
@Structure.FieldOrder({"LowPart", "HighPart"})
public class DXGILUID extends Structure {
	public WinDef.DWORD LowPart;
	public WinDef.LONG HighPart;

	public DXGILUID() {}
	public DXGILUID(Pointer ptr) {
		super(ptr);
	}

	public static class ByValue extends DXGILUID implements Structure.ByValue {
		public ByValue(Pointer ptr) {
			super(ptr);
		}
	}

	public ByValue byValue() {
		ByValue bv = new ByValue(getPointer());
		bv.read();
		return bv;
	}
}
