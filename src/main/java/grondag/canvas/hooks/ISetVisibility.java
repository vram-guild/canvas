package grondag.acuity.hooks;

import javax.annotation.Nullable;

public interface ISetVisibility
{
    public @Nullable Object getVisibilityData();
    
    public void setVisibilityData( Object data);

    public void releaseVisibilityData();
}
