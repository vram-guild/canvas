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

package grondag.canvas.varia;

import com.mojang.blaze3d.platform.GlStateManager;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class GlStateSpy extends ReflectionToStringBuilder {
	public static final GlStateSpy INSTANCE = new GlStateSpy();

	private GlStateSpy() {
		super(new Object(), new MultilineRecursiveToStringStyle() {
			@Override
			public void appendDetail(StringBuffer buffer, String fieldName, Object value) {
				if(value == null || (!value.getClass().getName().contains("Buffer") && !(value instanceof Enum))) {
					super.appendDetail(buffer, fieldName, value);
				} else {
					super.appendSummary(buffer, fieldName, value);
				}
			}
		}, null, Object.class, true, true);
	}

	@Override
	public String toString() {
		getStringBuffer().setLength(0);
		appendFieldsIn(GlStateManager.class);
		getStyle().appendEnd(getStringBuffer(), getObject());
		return getStringBuffer().toString();
	}

	public static void print() {
		System.out.println(INSTANCE.toString());
	}
}