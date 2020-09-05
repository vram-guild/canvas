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

package grondag.canvas.buffer.encoding;

import net.minecraft.client.render.VertexConsumer;

public interface VertexCollector extends VertexConsumer {
	void addi(int i);

	void addf(float f);

	void addf(float u, float v);

	void addf(float x, float y, float z);

	void addf(float... f);

	void add(int[] appendData, int length);
}
