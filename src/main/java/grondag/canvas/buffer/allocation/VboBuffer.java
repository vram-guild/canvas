/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffer.allocation;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GLX;

public class VboBuffer extends UploadableBuffer implements AllocationProvider {
    ByteBuffer uploadBuffer;
    
    int byteOffset = 0;
    
    public VboBuffer(int bytes) {
        uploadBuffer = MemoryUtil.memAlloc(bytes);
    }
    
    @Override
    public void upload() {
        if(uploadBuffer != null) {
            bind();
            uploadBuffer.rewind();
            GLX.glBufferData(GLX.GL_ARRAY_BUFFER, uploadBuffer, GLX.GL_STATIC_DRAW);
            unbind();
            MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = null;
        }
    }
    
    @Override
    protected void dispose() {
        super.dispose();
        if(uploadBuffer != null) {
            MemoryUtil.memFree(uploadBuffer);
            uploadBuffer = null;
        }
    }
    
    @Override
    public ByteBuffer byteBuffer() {
        return uploadBuffer;
    }
    
    @Override
    public void claimAllocation(int byteCount, Consumer<BufferDelegate> consumer) {
        consumer.accept(BufferDelegate.claim(this, byteOffset, byteCount));
        byteOffset += byteCount;
    }

    @Override
    public boolean isVbo() {
        return true;
    }
}
