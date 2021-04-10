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

package grondag.canvas.buffer.encoding;

public interface VertexAppender {
	void append(int val);

	default void append(int v0, int v1) {
		append(v0);
		append(v1);
	}

	default void append(int v0, int v1, int v2) {
		append(v0);
		append(v1);
		append(v2);
	}

	default void append(int v0, int v1, int v2, int v3) {
		append(v0);
		append(v1);
		append(v2);
		append(v3);
	}

	default void append(int v0, int v1, int v2, int v3, int v4) {
		append(v0);
		append(v1);
		append(v2);
		append(v3);
		append(v4);
	}

	default void append(float val) {
		append(Float.floatToRawIntBits(val));
	}

	default void append(float v0, float v1) {
		append(Float.floatToRawIntBits(v0));
		append(Float.floatToRawIntBits(v1));
	}

	default void append(float v0, float v1, float v2) {
		append(Float.floatToRawIntBits(v0));
		append(Float.floatToRawIntBits(v1));
		append(Float.floatToRawIntBits(v2));
	}

	default void append(float v0, float v1, float v2, float v3) {
		append(Float.floatToRawIntBits(v0));
		append(Float.floatToRawIntBits(v1));
		append(Float.floatToRawIntBits(v2));
		append(Float.floatToRawIntBits(v3));
	}

	default void append(float v0, float v1, float v2, float v3, float v4) {
		append(Float.floatToRawIntBits(v0));
		append(Float.floatToRawIntBits(v1));
		append(Float.floatToRawIntBits(v2));
		append(Float.floatToRawIntBits(v3));
		append(Float.floatToRawIntBits(v4));
	}
}
