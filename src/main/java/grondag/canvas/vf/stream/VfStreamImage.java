/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.vf.stream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.vf.BufferWriter;

@Environment(EnvType.CLIENT)
public final class VfStreamImage {
	private final VfStreamSpec spec;

	private VfStreamHolder holder;
	boolean logging = false;

	public VfStreamImage(VfStreamSpec spec) {
		this.spec = spec;
	}

	public void close() {
		if (holder != null) {
			holder.close();
			holder = null;
		}
	}

	public void clear() {
		close();
	}

	public void prepare() {
		if (holder == null) {
			holder = new VfStreamHolder(spec);
		} else if (holder.capacity() <= 0) {
			assert holder.capacity() == 0;
			holder.detach();
			holder = new VfStreamHolder(spec);
		}

		holder.prepare();
	}

	public VfStreamReference allocate(int byteCount, BufferWriter writer) {
		if (byteCount > spec.imageCapacityBytes()) {
			assert false : "Cannot allocate more than stream image size";
			return null;
		}

		if (byteCount > holder.capacity()) {
			holder.flush();
			holder.detach();
			holder = new VfStreamHolder(spec);
			holder.prepare();
		}

		return holder.allocate(byteCount, writer);
	}

	public void flush() {
		holder.flush();

		if (holder.capacity() <= 0) {
			assert holder.capacity() == 0;
			holder.detach();
			holder = null;
		}
	}
}
