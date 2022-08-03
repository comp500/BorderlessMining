package link.infra.dxjni;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class COMHelper {
	public static long getVtblOffset(long ptr, long offset) {
		// https://devblogs.microsoft.com/oldnewthing/20040205-00/?p=40733
		long lpVtbl = MemoryUtil.memGetAddress(ptr);
		lpVtbl += offset * Pointer.POINTER_SIZE;
		return lpVtbl;
	}

}
