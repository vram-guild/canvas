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
