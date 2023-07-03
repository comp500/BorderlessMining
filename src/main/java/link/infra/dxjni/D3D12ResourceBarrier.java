package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Type", "Flags", "Transition"})
public class D3D12ResourceBarrier extends Structure {
	public WinDef.UINT Type;
	public WinDef.UINT Flags;
	// Actually a union, but we only use Transition
	public D3D12ResourceTransitionBarrier Transition;

	public static int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = 0xffffffff;
	public static int D3D12_RESOURCE_BARRIER_TYPE_TRANSITION = 0;
	public static int D3D12_RESOURCE_BARRIER_FLAG_NONE = 0;

	public static int D3D12_RESOURCE_STATE_PRESENT = 0;
	public static int D3D12_RESOURCE_STATE_RENDER_TARGET = 0x4;

	public static D3D12ResourceBarrier Transition(D3D12Resource dxResource, int stateBefore, int stateAfter) {
		D3D12ResourceTransitionBarrier transitionBarrier = new D3D12ResourceTransitionBarrier();
		transitionBarrier.pResource = dxResource;
		transitionBarrier.Subresource = new WinDef.UINT(D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
		transitionBarrier.StateBefore = new WinDef.UINT(stateBefore);
		transitionBarrier.StateAfter = new WinDef.UINT(stateAfter);

		D3D12ResourceBarrier barrier = new D3D12ResourceBarrier();
		barrier.Type = new WinDef.UINT(D3D12_RESOURCE_BARRIER_TYPE_TRANSITION);
		barrier.Flags = new WinDef.UINT(D3D12_RESOURCE_BARRIER_FLAG_NONE);
		barrier.Transition = transitionBarrier;

		return barrier;
	}
}
