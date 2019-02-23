package grondag.acuity.core;

import grondag.acuity.api.RenderPipeline;

public class DummyPackingList extends VertexPackingList
{
    public static final DummyPackingList INSTANCE = new DummyPackingList();
    
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public int quadCount()
    {
        return 0;
    }

    @Override
    public int totalBytes()
    {
        return 0;
    }

    @Override
    public void addPacking(RenderPipeline pipeline, int vertexCount)
    {
        throw new UnsupportedOperationException();
    }
}
