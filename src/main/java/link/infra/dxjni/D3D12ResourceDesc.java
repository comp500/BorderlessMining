package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Dimension", "Alignment", "Width", "Height", "DepthOrArraySize", "MipLevels", "Format", "SampleDesc", "Layout", "Flags"})
public class D3D12ResourceDesc extends Structure {
	public WinDef.UINT Dimension;
	public WinDef.ULONGLONG Alignment;
	public WinDef.ULONGLONG Width;
	public WinDef.UINT Height;
	public WinDef.USHORT DepthOrArraySize;
	public WinDef.USHORT MipLevels;
	public WinDef.UINT Format;
	public DXGISampleDesc SampleDesc;
	public WinDef.UINT Layout;
	public WinDef.UINT Flags;
}
