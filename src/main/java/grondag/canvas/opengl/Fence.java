package grondag.acuity.opengl;

public abstract class Fence
{
    public abstract boolean isReached();

    public abstract void set();

    public void deleteGlResources()
    {
        
    }
}