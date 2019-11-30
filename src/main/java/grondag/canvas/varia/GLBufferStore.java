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

package grondag.canvas.varia;

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.GLX;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.minecraft.client.util.GlAllocationUtils;

/**
 * Buffer gen is incredibly slow on some Windows/NVidia systems and default MC
 * behavior
 */
public class GLBufferStore {
	private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
	private static final IntBuffer buff = GlAllocationUtils.allocateByteBuffer(128 * 4).asIntBuffer();

	public static int claimBuffer() {
		if (queue.isEmpty()) {
			GLX.glGenBuffers(buff);

			for (int i = 0; i < 128; i++) {
				queue.enqueue(buff.get(i));
			}

			buff.clear();
		}

		return queue.dequeueInt();
	}

	public static void releaseBuffer(int buffer) {
		queue.enqueue(buffer);
	}

	// should not be needed - Gl resources are destroyed when the context is
	// destroyed
	//    public static void deleteAll()
	//    {
	//        while(!queue.isEmpty())
	//        {
	//            while(!queue.isEmpty() && buff.position() < 128)
	//                buff.put(queue.dequeueInt());
	//
	//            if(OpenGlHelper.arbVbo)
	//                ARBVertexBufferObject.glDeleteBuffersARB(buff);
	//            else
	//                GL15.glDeleteBuffers(buff);
	//
	//            buff.clear();
	//        }
	//    }
}
