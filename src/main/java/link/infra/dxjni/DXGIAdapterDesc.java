package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

@Structure.FieldOrder({"Description", "VendorId", "DeviceId", "SubSysId", "Revision", "DedicatedVideoMemory", "DedicatedSystemMemory", "SharedSystemMemory", "AdapterLuid"})
public class DXGIAdapterDesc extends Structure {
	public char[] Description = new char[128];
	public WinDef.UINT VendorId;
	public WinDef.UINT DeviceId;
	public WinDef.UINT SubSysId;
	public WinDef.UINT Revision;
	public BaseTSD.SIZE_T DedicatedVideoMemory;
	public BaseTSD.SIZE_T DedicatedSystemMemory;
	public BaseTSD.SIZE_T SharedSystemMemory;
	public WinNT.LUID AdapterLuid;
}
