package link.infra.dxjni;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

@Structure.FieldOrder({"BufferDesc", "SampleDesc", "BufferUsage", "BufferCount", "OutputWindow", "Windowed", "SwapEffect", "Flags"})
public class DXGISwapChainDesc extends Structure {
	// Swapchain flags
	public static final int DXGI_SWAP_CHAIN_FLAG_FRAME_LATENCY_WAITABLE_OBJECT = 64; // TODO
	public static final int DXGI_SWAP_CHAIN_FLAG_ALLOW_TEARING = 2048;

	public static final int DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x00000020;
	public static final int DXGI_SWAP_EFFECT_SEQUENTIAL = 1;
	public static final int DXGI_SWAP_EFFECT_FLIP_DISCARD = 4;

	public DXGIModeDesc BufferDesc;
	public DXGISampleDesc SampleDesc;
	public WinDef.UINT BufferUsage;
	public WinDef.UINT BufferCount;
	public WinDef.HWND OutputWindow;
	public WinDef.BOOL Windowed;
	public WinDef.UINT SwapEffect;
	public WinDef.UINT Flags;
}
