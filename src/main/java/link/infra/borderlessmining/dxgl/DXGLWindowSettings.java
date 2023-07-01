package link.infra.borderlessmining.dxgl;

import link.infra.dxjni.DXGISwapChainDesc;
import link.infra.dxjni.DXJNIShim;

public record DXGLWindowSettings(
	int bufferCount,
	// TODO: backcompat? FLIP_DISCARD is only w10+
	int swapEffect,
	// TODO: feature test allow tearing
	int swapchainFlags,
	int presentFlags,
	// TODO: feature test allow tearing
	int presentFlagsVsync,
	int vsyncSyncInterval,
	boolean debug
) {
	DXGLWindowSettings() {
		this(
			2,
			DXGISwapChainDesc.DXGI_SWAP_EFFECT_FLIP_DISCARD,
			DXGISwapChainDesc.DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING,
			DXJNIShim.DXGI_PRESENT_ALLOW_TEARING,
			0,
			1,
			false
		);
	}
}
