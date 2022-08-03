package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;

public class D3D11Texture2D extends Unknown {
	public static final Guid.IID IID_ID3D11Texture2D = new Guid.IID("{6f15aaf2-d208-4e89-9ab4-489535d34f9c}");

	public D3D11Texture2D(Pointer pvInstance) {
		super(pvInstance);
	}


}
