package grondag.canvas.wip.state;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;

public class RenderContextState {
	/**
	 * Set via world rendered when incoming vertices are for an entity.
	 * Meant to enable entity material maps
	 */
	private @Nullable Entity currentEntity;

	public @Nullable Entity getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(@Nullable Entity currentEntity) {
		this.currentEntity = currentEntity;
	}
}
