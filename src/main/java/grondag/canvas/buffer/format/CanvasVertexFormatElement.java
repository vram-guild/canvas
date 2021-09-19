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

package grondag.canvas.buffer.format;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CanvasVertexFormatElement {
	public final String attributeName;
	public final int elementCount;
	public final int glConstant;
	public final boolean isNormalized;
	public final boolean isInteger;
	public final int byteSize;

	public CanvasVertexFormatElement(VertexFormatElement.Type formatIn, int count, String attributeName, boolean isNormalized, boolean isInteger) {
		this.attributeName = attributeName;
		elementCount = count;
		glConstant = formatIn.getGlType();
		byteSize = formatIn.getSize() * count;
		this.isNormalized = isNormalized;
		this.isInteger = isInteger;
	}
}
