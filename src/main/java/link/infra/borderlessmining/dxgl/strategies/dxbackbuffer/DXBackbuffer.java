package link.infra.borderlessmining.dxgl.strategies.dxbackbuffer;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import link.infra.borderlessmining.dxgl.DXGLWindow;
import link.infra.borderlessmining.dxgl.DXGLWindowSettings;
import link.infra.dxjni.D3D12ResourceAllocationInfo;
import link.infra.dxjni.D3D12ResourceDesc;
import net.minecraft.client.util.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.GL32C;

import java.util.ArrayList;
import java.util.List;

public abstract class DXBackbuffer extends DXGLWindow {
	public int colorTexture;
	public int targetFramebuffer;
	public int dxBackbufferMemoryObject;
	public final PointerBuffer d3dTargets = PointerBuffer.allocateDirect(1);

	public DXBackbuffer(Window parent, DXGLWindowSettings settings) {
		super(parent, settings);
	}

	@Override
	protected void setupGlBuffers() {
		targetFramebuffer = GL32C.glGenFramebuffers();
	}

	@Override
	protected void freeGlBuffers() {
		GL32C.glDeleteFramebuffers(targetFramebuffer);
	}

	@Override
	protected void registerBackbuffer(int width, int height) {
		D3D12ResourceDesc desc = dxColorBackbuffer.GetDesc(new D3D12ResourceDesc());
		D3D12ResourceAllocationInfo allocInfo = d3dDevice.GetResourceAllocationInfo(new D3D12ResourceAllocationInfo(), new WinDef.UINT(0), new WinDef.UINT(1), desc.getPointer());
		long size = allocInfo.SizeInBytes.longValue();
		long sizeOrig = (long) width * height * 4; // RGBA;
		System.out.println("Got size " + size + " (orig " + sizeOrig + ")");

		int num = GL32C.glGetInteger(GL32C.GL_NUM_EXTENSIONS);
		List<String> exts = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			exts.add(GL32C.glGetStringi(GL32C.GL_EXTENSIONS, i));
		}
		if (!exts.contains("GL_EXT_memory_object")) {
			System.out.println("GL_EXT_memory_object not supported!");
		}

		colorTexture = GL32C.glGenTextures();

		// Register d3d backbuffer as an OpenGL memory object
		dxBackbufferMemoryObject = EXTMemoryObject.glCreateMemoryObjectsEXT();
		WinNT.HANDLEByReference shareHandle = new WinNT.HANDLEByReference();
		COMUtils.checkRC(d3dDevice.CreateSharedHandle(dxColorBackbuffer, Pointer.NULL, new WinDef.DWORD(WinNT.GENERIC_ALL), null, shareHandle));
		System.out.println("Created shared handle");
		EXTMemoryObject.glMemoryObjectParameteriEXT(dxBackbufferMemoryObject, EXTMemoryObject.GL_DEDICATED_MEMORY_OBJECT_EXT, GL32C.GL_TRUE);
		EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(dxBackbufferMemoryObject, size,
			EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, Pointer.nativeValue(shareHandle.getValue().getPointer()));
		int err = GL32C.glGetError();
		if (err != GL32C.GL_NO_ERROR) {
			throw new IllegalStateException("Failed to import DXGI backbuffer as memory object: " + err);
		}
		System.out.println("Imported shared handle!");

		// Attach OpenGL texture to memory object
		// Note: Renderbuffers don't seem to work with EXT_external_objects_win32
		GL32C.glBindTexture(GL32C.GL_TEXTURE_2D, colorTexture);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, EXTMemoryObject.GL_TEXTURE_TILING_EXT, EXTMemoryObject.GL_OPTIMAL_TILING_EXT);
		EXTMemoryObject.glTexStorageMem2DEXT(GL32C.GL_TEXTURE_2D, 1, GL32C.GL_RGBA8, width, height, dxBackbufferMemoryObject, 0);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_MAG_FILTER, GL32C.GL_LINEAR);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_MIN_FILTER, GL32C.GL_LINEAR);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_WRAP_S, GL32C.GL_REPEAT);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_WRAP_T, GL32C.GL_REPEAT);
		GL32C.glBindTexture(GL32C.GL_TEXTURE_2D, 0);

		System.out.println("Attached texture to mem obj");

		// Attach d3d backbuffer to OpenGL framebuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer);
		GL32C.glFramebufferTexture(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, colorTexture, 0);
		int status = GL32C.glCheckFramebufferStatus(GL32C.GL_FRAMEBUFFER);
		if (status != GL32C.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Unexpected status when creating framebuffer: " + status);
		}
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);

		System.out.println("Attached backbuffer to opengl framebuffer");
	}

	@Override
	protected void unregisterBackbuffer() {
		//WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets.get());
		//d3dTargets.flip();
		// TODO: unbind framebuffers?
		GL32C.glDeleteTextures(colorTexture);
		EXTMemoryObject.glDeleteMemoryObjectsEXT(dxBackbufferMemoryObject);
		// TODO: clean up shared handle?
	}

	@Override
	protected void bindBackbuffer() {
		//WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, targetFramebuffer);
	}

	@Override
	protected void unbindBackbuffer() {
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		//WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets);
	}
}
