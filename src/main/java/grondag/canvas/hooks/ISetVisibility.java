package grondag.canvas.hooks;

public interface ISetVisibility {
    public Object getVisibilityData();

    public void setVisibilityData(Object data);

    public void releaseVisibilityData();
}
