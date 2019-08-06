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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import grondag.canvas.Configurator;
import net.minecraft.util.math.MathHelper;

/**
 * Tracks all allocations, ensures deallocation on render reload.
 * Implements configuration of allocation method.
 */
public class BufferAllocator {
    private static final IntFunction<ByteBuffer> SUPPLIER = Configurator.safeNativeMemoryAllocation ? BufferUtils::createByteBuffer : MemoryUtil::memAlloc;
    private static final Consumer<ByteBuffer> CONSUMER = Configurator.safeNativeMemoryAllocation ? b -> {} : MemoryUtil::memFree;
    private static final Set<ByteBuffer> OPEN = Collections.newSetFromMap(new IdentityHashMap<ByteBuffer, Boolean>());
    
    public static synchronized  ByteBuffer claim(int bytes) {
        if(bytes < 4096) {
            bytes = 4096;
        }
        bytes = MathHelper.smallestEncompassingPowerOfTwo(bytes);
        final ByteBuffer result =SUPPLIER.apply(bytes);
        OPEN.add(result);
        return result;
    }

    public static synchronized void release(ByteBuffer uploadBuffer) {
        if(OPEN.remove(uploadBuffer)) {
            CONSUMER.accept(uploadBuffer);
        }
    }

    public static synchronized void forceReload() {
        OPEN.forEach(CONSUMER);
        OPEN.clear();
    }

}
