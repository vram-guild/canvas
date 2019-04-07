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
import java.nio.IntBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.math.MathHelper;

/**
 * Holds a thread-safe cache of buffer builders to be used for VBO uploads
 */
public class BufferStore {
    private static final ArrayBlockingQueue<ExpandableByteBuffer> store = new ArrayBlockingQueue<ExpandableByteBuffer>(
            4096);
    private static final int BUFFER_SIZE_INCREMENT = 0x200000;

    public static class ExpandableByteBuffer {
        private ByteBuffer byteBuffer;
        private IntBuffer intBuffer;

        private ExpandableByteBuffer() {
            byteBuffer = GlAllocationUtils.allocateByteBuffer(BUFFER_SIZE_INCREMENT);
            intBuffer = byteBuffer.asIntBuffer();
        }

        public ByteBuffer byteBuffer() {
            return byteBuffer;
        }

        public IntBuffer intBuffer() {
            return intBuffer;
        }

        public void expand(int minByteSize) {
            if (minByteSize > this.byteBuffer.capacity()) {
                ByteBuffer newBuffer = GlAllocationUtils
                        .allocateByteBuffer(MathHelper.roundUp(minByteSize, BUFFER_SIZE_INCREMENT));
                int oldIntPos = this.intBuffer.position();
                int oldBytePos = this.byteBuffer.position();
                this.byteBuffer.position(0);
                newBuffer.put(this.byteBuffer);
                newBuffer.rewind();
                this.byteBuffer = newBuffer;
                this.intBuffer = newBuffer.asIntBuffer();
                newBuffer.position(oldBytePos);
                this.intBuffer.position(oldIntPos);
            }
        }

    }

    public static ExpandableByteBuffer claim() {
        ExpandableByteBuffer result = store.poll();
        return result == null ? new ExpandableByteBuffer() : result;
    }

    public static void release(ExpandableByteBuffer buffer) {
        buffer.byteBuffer.clear();
        store.offer(buffer);
    }
}
