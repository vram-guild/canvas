package grondag.canvas.mixinext;

public interface ChunkVisibility {
    public Object getVisibilityData();

    public void setVisibilityData(Object data);

    public void releaseVisibilityData();
}
