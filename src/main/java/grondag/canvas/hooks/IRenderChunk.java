package grondag.canvas.hooks;


import grondag.canvas.buffering.DrawableChunk;

public interface IRenderChunk
{
    void setSolidDrawable(DrawableChunk.Solid drawable);
    void setTranslucentDrawable(DrawableChunk.Translucent drawable);
    DrawableChunk.Solid getSolidDrawable();
    DrawableChunk.Translucent getTranslucentDrawable();
    void releaseDrawables();
}
