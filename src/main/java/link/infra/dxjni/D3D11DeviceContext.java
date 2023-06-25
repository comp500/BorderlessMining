package link.infra.dxjni;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.WinDef;

public class D3D11DeviceContext extends Unknown {
	public D3D11DeviceContext(Pointer ptr) {
		super(ptr);
	}

	// TODO: JNA used; could replace with dyncall (1.18-) / libffi (1.19+) for potentially better performance?
	public void OMSetRenderTargets(WinDef.UINT NumViews, Pointer[] ppRenderTargetViews, Pointer pDepthStencilView) {
		_invokeNativeVoid(33, new Object[]{this.getPointer(), NumViews, ppRenderTargetViews, pDepthStencilView});
	}

	public void ClearRenderTargetView(Pointer pRenderTargetView, float[] ColorRGBA) {
		_invokeNativeVoid(50, new Object[]{this.getPointer(), pRenderTargetView, ColorRGBA});
	}

	public void ClearState() {
		_invokeNativeVoid(110, new Object[]{this.getPointer()});
	}

	public void Flush() {
		_invokeNativeVoid(111, new Object[]{this.getPointer()});
	}

}
