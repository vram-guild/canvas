package grondag.canvas.terrain;

import java.util.List;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.entity.BlockEntity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.buffer.encoding.VertexCollectorImpl;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.material.MaterialState;
import grondag.canvas.shader.ShaderPass;

@Environment(EnvType.CLIENT)
public class RegionData {
	public static final RegionData EMPTY = new RegionData();

	final ObjectArrayList<BlockEntity> blockEntities = new ObjectArrayList<>();
	int[] occlusionData = null;

	@Nullable int[] translucentState;

	public List<BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	public void endBuffering(float x, float y, float z, VertexCollectorList buffers) {
		final VertexCollectorImpl buffer = buffers.getIfExists(MaterialState.getDefault(ShaderPass.TRANSLUCENT));

		if (buffer != null) {
			buffer.sortQuads(x, y, z);
			translucentState = buffer.saveState(translucentState);
		}
	}

	public int[] getOcclusionData() {
		return occlusionData;
	}

	public void complete(int[] occlusionData) {
		this.occlusionData = occlusionData;
	}
}