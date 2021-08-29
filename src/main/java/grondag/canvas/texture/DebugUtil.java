package grondag.canvas.texture;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.texture.TextureStitcher;

public interface DebugUtil {
	default String output(List<TextureStitcher.Holder> list) {
		final String result = list.stream().map(h -> h.sprite.getId().toString()).collect(Collectors.joining(", "));
		System.out.println(result);
		return result;
	}
}
