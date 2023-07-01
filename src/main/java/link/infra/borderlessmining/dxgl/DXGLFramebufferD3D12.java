package link.infra.borderlessmining.dxgl;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import link.infra.dxjni.*;
import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.GL32C;

public class DXGLFramebufferD3D12 {
	private D3D12Resource dxResource;
	private final int glTexture;
	private final int glFramebuffer;
	private final int glMemoryObject;

	public DXGLFramebufferD3D12(int width, int height, int bufferIdx, DXGISwapchain3 d3dSwapchain, D3D12Device d3dDevice) {
		PointerByReference colorBufferBuf = new PointerByReference();
		// Get swapchain backbuffer as an ID3D12Resource
		COMUtils.checkRC(d3dSwapchain.GetBuffer(
			new WinDef.UINT(bufferIdx),
			new Guid.REFIID(D3D12Resource.IID_ID3D12Resource),
			colorBufferBuf
		));
		dxResource = new D3D12Resource(colorBufferBuf.getValue());

		D3D12ResourceDesc desc = dxResource.GetDesc(new D3D12ResourceDesc());
		D3D12ResourceAllocationInfo allocInfo = d3dDevice.GetResourceAllocationInfo(new D3D12ResourceAllocationInfo(), new WinDef.UINT(0), new WinDef.UINT(1), desc.getPointer());
		long size = allocInfo.SizeInBytes.longValue();
		long sizeOrig = (long) width * height * 4; // RGBA;
		System.out.println("Got size " + size + " (orig " + sizeOrig + ")");

		glTexture = GL32C.glGenTextures();
		glFramebuffer = GL32C.glGenFramebuffers();

		// Register d3d backbuffer as an OpenGL memory object
		glMemoryObject = EXTMemoryObject.glCreateMemoryObjectsEXT();
		WinNT.HANDLEByReference shareHandle = new WinNT.HANDLEByReference();
		COMUtils.checkRC(d3dDevice.CreateSharedHandle(dxResource, Pointer.NULL, new WinDef.DWORD(WinNT.GENERIC_ALL), null, shareHandle));
		System.out.println("Created shared handle");
		EXTMemoryObject.glMemoryObjectParameteriEXT(glMemoryObject, EXTMemoryObject.GL_DEDICATED_MEMORY_OBJECT_EXT, GL32C.GL_TRUE);
		EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(glMemoryObject, size,
			EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, Pointer.nativeValue(shareHandle.getValue().getPointer()));
		int err = GL32C.glGetError();
		if (err != GL32C.GL_NO_ERROR) {
			throw new IllegalStateException("Failed to import DXGI backbuffer as memory object: " + err);
		}
		System.out.println("Imported shared handle!");

		// Attach OpenGL texture to memory object
		// Note: Renderbuffers don't seem to work with EXT_external_objects_win32
		GL32C.glBindTexture(GL32C.GL_TEXTURE_2D, glTexture);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, EXTMemoryObject.GL_TEXTURE_TILING_EXT, EXTMemoryObject.GL_OPTIMAL_TILING_EXT);
		EXTMemoryObject.glTexStorageMem2DEXT(GL32C.GL_TEXTURE_2D, 1, GL32C.GL_RGBA8, width, height, glMemoryObject, 0);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_MAG_FILTER, GL32C.GL_LINEAR);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_MIN_FILTER, GL32C.GL_LINEAR);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_WRAP_S, GL32C.GL_REPEAT);
		GL32C.glTexParameteri(GL32C.GL_TEXTURE_2D, GL32C.GL_TEXTURE_WRAP_T, GL32C.GL_REPEAT);
		GL32C.glBindTexture(GL32C.GL_TEXTURE_2D, 0);

		System.out.println("Attached texture to mem obj");

		// Attach d3d backbuffer to OpenGL framebuffer
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, glFramebuffer);
		GL32C.glFramebufferTexture(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, glTexture, 0);
		int status = GL32C.glCheckFramebufferStatus(GL32C.GL_FRAMEBUFFER);
		if (status != GL32C.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("Unexpected status when creating framebuffer: " + status);
		}
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);

		System.out.println("Attached backbuffer to opengl framebuffer");
	}

	public void free() {
		GL32C.glDeleteFramebuffers(glFramebuffer);
		GL32C.glDeleteTextures(glTexture);
		EXTMemoryObject.glDeleteMemoryObjectsEXT(glMemoryObject);
		// TODO: clean up shared handle?
		dxResource.Release();
		//WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets.get());
		//d3dTargets.flip();
		dxResource = null;
	}

	public void bind() {
		//WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, glFramebuffer);
	}

	public void unbind() {
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		//WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets);
	}
}
