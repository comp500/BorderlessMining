package link.infra.borderlessmining.dxgl;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import link.infra.dxjni.*;
import org.lwjgl.opengl.*;

public class DXGLFramebufferD3D12 {
	private final D3D12GraphicsCommandList commandListTransitionToRenderTarget;
	private final D3D12GraphicsCommandList commandListTransitionToPresent;
	private final D3D12GraphicsCommandList[] commandListTransitionToRenderTargetArr;
	private final D3D12GraphicsCommandList[] commandListTransitionToPresentArr;
	private final D3D12CommandAllocator allocator;

	private final D3D12Resource dxResource;
	private final D3D12Fence dxFence;
	private final D3D12CommandQueue dxCommandQueue;
	private long fenceValue = 0;
	private final int glTexture;
	private final int glFramebuffer;
	private final int glMemoryObject;
	private final int glSemaphore;

	public DXGLFramebufferD3D12(int width, int height, int bufferIdx,
								DXGISwapchain3 dxSwapchain, D3D12Device dxDevice,
								D3D12CommandQueue dxCommandQueue) {
		PointerByReference colorBufferBuf = new PointerByReference();
		// Get swapchain backbuffer as an ID3D12Resource
		COMUtils.checkRC(dxSwapchain.GetBuffer(
			new WinDef.UINT(bufferIdx),
			new Guid.REFIID(D3D12Resource.IID_ID3D12Resource),
			colorBufferBuf
		));
		dxResource = new D3D12Resource(colorBufferBuf.getValue());

		D3D12ResourceDesc desc = dxResource.GetDesc(new D3D12ResourceDesc());
		D3D12ResourceAllocationInfo allocInfo = dxDevice.GetResourceAllocationInfo(new D3D12ResourceAllocationInfo(), new WinDef.UINT(0), new WinDef.UINT(1), desc.getPointer());
		long size = allocInfo.SizeInBytes.longValue();
		long sizeOrig = (long) width * height * 4; // RGBA;
		System.out.println("Got size " + size + " (orig " + sizeOrig + ")");

		glTexture = GL32C.glGenTextures();
		glFramebuffer = GL32C.glGenFramebuffers();

		// Register d3d backbuffer as an OpenGL memory object
		glMemoryObject = EXTMemoryObject.glCreateMemoryObjectsEXT();
		WinNT.HANDLEByReference shareHandleBuffer = new WinNT.HANDLEByReference();
		COMUtils.checkRC(dxDevice.CreateSharedHandle(dxResource, Pointer.NULL, new WinDef.DWORD(WinNT.GENERIC_ALL), null, shareHandleBuffer));
		System.out.println("Created shared handle");
		EXTMemoryObject.glMemoryObjectParameteriEXT(glMemoryObject, EXTMemoryObject.GL_DEDICATED_MEMORY_OBJECT_EXT, GL32C.GL_TRUE);
		EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(glMemoryObject, size,
			EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, Pointer.nativeValue(shareHandleBuffer.getValue().getPointer()));
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

		// Create fence for synchronisation
		PointerByReference fenceBuf = new PointerByReference();
		COMUtils.checkRC(dxDevice.CreateFence(fenceValue, new WinDef.UINT(D3D12Fence.D3D12_FENCE_FLAG_SHARED), new Guid.REFIID(D3D12Fence.IID_ID3D12Fence), fenceBuf));
		dxFence = new D3D12Fence(fenceBuf.getValue());
		// Import fence as a GL semaphore
		glSemaphore = EXTSemaphore.glGenSemaphoresEXT();
		WinNT.HANDLEByReference shareHandleSemaphore = new WinNT.HANDLEByReference();
		COMUtils.checkRC(dxDevice.CreateSharedHandle(dxFence, Pointer.NULL, new WinDef.DWORD(WinNT.GENERIC_ALL), null, shareHandleSemaphore));
		EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT(glSemaphore, EXTSemaphoreWin32.GL_HANDLE_TYPE_D3D12_FENCE_EXT, Pointer.nativeValue(shareHandleSemaphore.getValue().getPointer()));

		// Create transition barriers for claiming the backbuffer
		D3D12ResourceBarrier barrierTransitionToPresent = D3D12ResourceBarrier.Transition(dxResource, D3D12ResourceBarrier.D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12ResourceBarrier.D3D12_RESOURCE_STATE_PRESENT);
		D3D12ResourceBarrier barrierTransitionToRenderTarget = D3D12ResourceBarrier.Transition(dxResource, D3D12ResourceBarrier.D3D12_RESOURCE_STATE_PRESENT, D3D12ResourceBarrier.D3D12_RESOURCE_STATE_RENDER_TARGET);
		// Create command allocator
		PointerByReference allocBuf = new PointerByReference();
		COMUtils.checkRC(dxDevice.CreateCommandAllocator(new WinDef.UINT(D3D12GraphicsCommandList.D3D12_COMMAND_LIST_TYPE_DIRECT),
			new Guid.REFIID(D3D12CommandAllocator.IID_ID3D12CommandAllocator), allocBuf));
		allocator = new D3D12CommandAllocator(allocBuf.getValue());
		// Create command lists for claiming the backbuffer
		{
			PointerByReference listBuf = new PointerByReference();
			COMUtils.checkRC(dxDevice.CreateCommandList(new WinDef.UINT(0),
				new WinDef.UINT(D3D12GraphicsCommandList.D3D12_COMMAND_LIST_TYPE_DIRECT), allocator, Pointer.NULL,
				new Guid.REFIID(D3D12GraphicsCommandList.IID_ID3D12GraphicsCommandList), listBuf));
			commandListTransitionToPresent = new D3D12GraphicsCommandList(listBuf.getValue());
			commandListTransitionToPresent.ResourceBarrier(new D3D12ResourceBarrier[]{barrierTransitionToPresent});
			COMUtils.checkRC(commandListTransitionToPresent.Close());
			commandListTransitionToPresentArr = new D3D12GraphicsCommandList[] {commandListTransitionToPresent};
		}
		{
			PointerByReference listBuf = new PointerByReference();
			COMUtils.checkRC(dxDevice.CreateCommandList(new WinDef.UINT(0),
				new WinDef.UINT(D3D12GraphicsCommandList.D3D12_COMMAND_LIST_TYPE_DIRECT), allocator, Pointer.NULL,
				new Guid.REFIID(D3D12GraphicsCommandList.IID_ID3D12GraphicsCommandList), listBuf));
			commandListTransitionToRenderTarget = new D3D12GraphicsCommandList(listBuf.getValue());
			commandListTransitionToRenderTarget.ResourceBarrier(new D3D12ResourceBarrier[]{barrierTransitionToRenderTarget});
			COMUtils.checkRC(commandListTransitionToRenderTarget.Close());
			commandListTransitionToRenderTargetArr = new D3D12GraphicsCommandList[] {commandListTransitionToRenderTarget};
		}

		this.dxCommandQueue = dxCommandQueue;
	}

	public void free() {
		GL32C.glDeleteFramebuffers(glFramebuffer);
		GL32C.glDeleteTextures(glTexture);
		EXTMemoryObject.glDeleteMemoryObjectsEXT(glMemoryObject);
		// TODO: clean up shared handle?
		dxResource.Release();
		commandListTransitionToPresent.Release();
		commandListTransitionToRenderTarget.Release();
		allocator.Release();
		//WGLNVDXInterop.wglDXUnregisterObjectNV(d3dDeviceGl, d3dTargets.get());
		//d3dTargets.flip();
	}

	private static final int[] emptyIntArr = new int[0];

	public void bind() {
		// D3D: lock backbuffer then signal fence (in queue)
		dxCommandQueue.ExecuteCommandLists(commandListTransitionToRenderTargetArr);
		COMUtils.checkRC(dxCommandQueue.Signal(dxFence, ++fenceValue));

		// OpenGL: wait for semaphore (server-side), bind framebuffer
		EXTSemaphore.glSemaphoreParameterui64EXT(glSemaphore, EXTSemaphoreWin32.GL_D3D12_FENCE_VALUE_EXT, fenceValue);
		// (texture layouts don't seem to be necessary, at least for D3D)
		EXTSemaphore.glWaitSemaphoreEXT(glSemaphore, emptyIntArr, emptyIntArr, emptyIntArr);
		//WGLNVDXInterop.wglDXLockObjectsNV(d3dDeviceGl, d3dTargets);
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, glFramebuffer);
	}

	public void unbind() {
		// OpenGL: unbind framebuffer, signal semaphore (server-side)
		GL32C.glBindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
		//WGLNVDXInterop.wglDXUnlockObjectsNV(d3dDeviceGl, d3dTargets);
		EXTSemaphore.glSemaphoreParameterui64EXT(glSemaphore, EXTSemaphoreWin32.GL_D3D12_FENCE_VALUE_EXT, ++fenceValue);
		EXTSemaphore.glSignalSemaphoreEXT(glSemaphore, emptyIntArr, emptyIntArr, emptyIntArr);

		// D3D: wait for fence then unlock backbuffer
		COMUtils.checkRC(dxCommandQueue.Wait(dxFence, fenceValue));
		dxCommandQueue.ExecuteCommandLists(commandListTransitionToPresentArr);

		// Give GL a hint that it should do some work (as we're not using swapbuffers)
		GL32C.glFlush();
	}
}
