package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class D3D12Device extends Unknown {
	public static final Guid.IID IID_ID3D12Device = new Guid.IID("{189819f1-1db6-4b57-be54-1821339b85f7}");

	public D3D12Device(Pointer ptr) {
		super(ptr);
	}

	public WinNT.HRESULT CreateCommandQueue(D3D12CommandQueueDesc desc, Guid.REFIID riid, PointerByReference commandQueue) {
		return (WinNT.HRESULT) _invokeNativeObject(8, new Object[]{this.getPointer(), desc, riid, commandQueue}, WinNT.HRESULT.class);
	}

	public D3D12ResourceAllocationInfo GetResourceAllocationInfo(D3D12ResourceAllocationInfo retVal, WinDef.UINT visibleMask, WinDef.UINT numResourceDescs, Pointer pResourceDescs) {
		return (D3D12ResourceAllocationInfo) _invokeNativeObject(25, new Object[]{this.getPointer(), retVal, visibleMask, numResourceDescs, pResourceDescs}, D3D12ResourceAllocationInfo.class);
	}

	public WinNT.HRESULT CreateSharedHandle(D3D12DeviceChild object, Pointer attributes, WinDef.DWORD access, WTypes.LPWSTR name, WinNT.HANDLEByReference handle) {
		return (WinNT.HRESULT) _invokeNativeObject(31, new Object[]{this.getPointer(), object, attributes, access, name, handle}, WinNT.HRESULT.class);
	}

	public WinNT.HRESULT CreateFence(long InitialValue, WinDef.UINT Flags, Guid.REFIID riid, PointerByReference ppFence) {
		return (WinNT.HRESULT) _invokeNativeObject(36, new Object[]{this.getPointer(), InitialValue, Flags, riid, ppFence}, WinNT.HRESULT.class);
	}
}
