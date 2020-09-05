/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.apiimpl.material;

class MeshMaterialKey {
	protected long bits0;
	protected long bits1;

	MeshMaterialKey(long bits0, long bits1) {
		this.bits0 = bits0;
		this.bits1 = bits1;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof MeshMaterialKey)) {
			return false;
		}

		final MeshMaterialKey o = (MeshMaterialKey) other;

		return bits0 == o.bits0 && bits1 == o.bits1;
	}

	@Override
	public int hashCode() {
		return it.unimi.dsi.fastutil.HashCommon.long2int(bits0) * 31 + it.unimi.dsi.fastutil.HashCommon.long2int(bits1);
	}
}
