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

package grondag.canvas.buffer.packing;

import java.nio.IntBuffer;

import grondag.canvas.buffer.allocation.AllocationProvider;
import grondag.canvas.draw.DelegateLists;
import grondag.canvas.draw.DrawableDelegate;
import grondag.canvas.material.MaterialState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BufferPacker {
    private static final ThreadLocal<BufferPacker> THREADLOCAL = ThreadLocal.withInitial(BufferPacker::new);
    
    ObjectArrayList<DrawableDelegate> delegates;
    VertexCollectorList collectorList;
    AllocationProvider allocator;
    
    private BufferPacker() {
        //private
    }
    
    /** Does not retain packing list reference */
    public static ObjectArrayList<DrawableDelegate> pack(BufferPackingList packingList, VertexCollectorList collectorList, AllocationProvider allocator) {
        final BufferPacker packer = THREADLOCAL.get();
        final ObjectArrayList<DrawableDelegate> result = DelegateLists.getReadyDelegateList();
        packer.delegates = result;
        packer.collectorList = collectorList;
        packer.allocator = allocator;
        packingList.forEach(packer);
        packer.delegates = null;
        packer.collectorList = null;
        packer.allocator = null;
        return result;
    }

    public void accept(MaterialState materialState, int vertexStart, int vertexCount) {
        final int stride = materialState.shader.piplineVertexFormat().vertexStrideBytes;
        allocator.claimAllocation(materialState, vertexCount * stride, ref -> {
            final int byteOffset = ref.byteOffset();
            final int byteCount = ref.byteCount();
            final int intLength = byteCount / 4;

            ref.buffer().lockForWrite();
            final IntBuffer intBuffer = ref.intBuffer();
            intBuffer.position(byteOffset / 4);
            intBuffer.put(collectorList.get(materialState).rawData(), vertexStart * stride / 4, intLength);
            ref.buffer().unlockForWrite();

            delegates.add(DrawableDelegate.claim(ref, materialState, byteCount / stride));
        });
    }
}
