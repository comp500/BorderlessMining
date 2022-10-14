package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Width", "Height", "RefreshRate", "Format", "ScanlineOrdering", "Scaling"})
public class DXGIModeDesc extends Structure {
	public static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;

	public WinDef.UINT Width;
	public WinDef.UINT Height;
	public DXGIRational RefreshRate;
	public WinDef.UINT Format;
	public WinDef.UINT ScanlineOrdering;
	public WinDef.UINT Scaling;
}
