package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"Width", "Height", "Format", "Stereo", "SampleDesc", "BufferUsage", "BufferCount", "Scaling", "SwapEffect", "AlphaMode", "Flags"})
public class DXGISwapChainDesc1 extends Structure {
	// Swapchain flags
	public static final int DXGI_SWAP_CHAIN_FLAG_FRAME_LATENCY_WAITABLE_OBJECT = 64; // TODO
	public static final int DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING = 2048;

	public static final int DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x00000020;
	public static final int DXGI_SWAP_EFFECT_DISCARD = 0;
	public static final int DXGI_SWAP_EFFECT_SEQUENTIAL = 1;
	public static final int DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL = 3;
	public static final int DXGI_SWAP_EFFECT_FLIP_DISCARD = 4;

	public WinDef.UINT Width;
	public WinDef.UINT Height;
	public WinDef.UINT Format;
	public WinDef.BOOL Stereo;
	public DXGISampleDesc SampleDesc;
	public WinDef.UINT BufferUsage;
	public WinDef.UINT BufferCount;
	public WinDef.UINT Scaling;
	public WinDef.UINT SwapEffect;
	public WinDef.UINT AlphaMode;
	public WinDef.UINT Flags;

	public DXGISwapChainDesc1(Pointer p) {
		super(p);
	}

	public DXGISwapChainDesc1() {}
}
