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

package grondag.canvas.pipeline.config;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class JanksonHelper {
	static @Nullable String asString(JsonElement json) {
		if (json instanceof JsonPrimitive) {
			final JsonPrimitive p = (JsonPrimitive) json;

			if (p.getValue() instanceof String) {
				return (String) p.getValue();
			}
		}

		return null;
	}
}
