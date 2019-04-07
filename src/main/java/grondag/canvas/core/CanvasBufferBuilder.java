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

package grondag.canvas.core;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import grondag.canvas.buffering.AbstractBufferDelegate;
import grondag.canvas.buffering.AllocableBuffer;
import grondag.canvas.buffering.AllocationProvider;
import grondag.canvas.buffering.DrawableDelegate;
import grondag.canvas.buffering.VertexCollectorList;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;

public class CanvasBufferBuilder extends BufferBuilder implements AllocationProvider {
    private final CanvasBuffer canvasBuffer = new CanvasBuffer();
    
    public CanvasBufferBuilder(int size) {
        super(size);
    }

    public final VertexCollectorList vcList = new VertexCollectorList();
    
    int byteOffset = 0;

    @Override
    public void setOffset(double x, double y, double z) {
        vcList.setAbsoluteRenderOrigin(-x, -y, -z);
        super.setOffset(x, y, z);
    }
    
    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void begin(int primitive, VertexFormat format) {
        super.begin(primitive, format);
    }

    @Override
    public void end() {
        super.end();
    }
    
    public void clearAllocations() {
        canvasBuffer.clear();
        byteOffset = 0;
    }

    public void ensureCapacity(int totalBytes) {
        canvasBuffer.ensureByteCapacity(totalBytes);
    }
    
    @Override
    public void claimAllocation(ConditionalPipeline pipeline, int byteCount, Consumer<AbstractBufferDelegate<?>> consumer) {
        final int newOffset = byteOffset + byteCount;
        
        consumer.accept(new CanvasBufferDelegate(canvasBuffer, byteOffset, byteCount));
        byteOffset = newOffset;        
    }
    
    private class CanvasBufferDelegate extends AbstractBufferDelegate<CanvasBuffer> {
        protected CanvasBufferDelegate(CanvasBuffer buffer, int byteOffset, int byteCount) {
            super(buffer, byteOffset, byteCount);
        }

        @Override
        public boolean isVbo() {
            return false;
        }

        @Override
        protected void lockForUpload() {
            // NOOP
        }

        @Override
        protected void unlockForUpload() {
            // NOOP
        }

        @Override
        protected void retain(DrawableDelegate result) {
            // NOOP
        }

        @Override
        protected int glBufferId() {
            return -1;
        }

        @Override
        protected void bind() {
            // NOOP
        }

        @Override
        protected boolean isDisposed() {
            return false;
        }

        @Override
        protected void release(DrawableDelegate drawableDelegate) {
            // NOOP
        }

        @Override
        protected void flush() {
            // NOOP
        }
    }
    
    private class CanvasBuffer extends AllocableBuffer {
        private static final int BUFFER_SIZE_INCREMENT = 2097152;
        private ByteBuffer byteBuffer = GlAllocationUtils.allocateByteBuffer(BUFFER_SIZE_INCREMENT);
        
        @Override
        protected ByteBuffer byteBuffer() {
            return byteBuffer;
        }
        
        private void clear() {
            byteBuffer.clear();
        }
        
        private void ensureByteCapacity(int byteCount) {
            final ByteBuffer byteBuffer = this.byteBuffer;
            if (byteCount > byteBuffer.capacity()) {
                int newSize = byteBuffer.capacity() + BUFFER_SIZE_INCREMENT;
                while(newSize < byteCount) {
                    newSize += BUFFER_SIZE_INCREMENT;
                }
                ByteBuffer newBuffer = GlAllocationUtils.allocateByteBuffer(newSize);
                byteBuffer.position(0);
                newBuffer.put(byteBuffer);
                newBuffer.rewind();
                this.byteBuffer = newBuffer;
             }
        }
    }
}
