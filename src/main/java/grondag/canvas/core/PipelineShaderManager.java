package grondag.canvas.core;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class PipelineShaderManager
{
    public final static PipelineShaderManager INSTANCE = new PipelineShaderManager();
    private Object2ObjectOpenHashMap<String, PipelineVertexShader> vertexShaders = new Object2ObjectOpenHashMap<>();
    private Object2ObjectOpenHashMap<String, PipelineFragmentShader> fragmentShaders = new Object2ObjectOpenHashMap<>();

    String vertexLibrarySource;
    String fragmentLibrarySource;
    
    public final String DEFAULT_VERTEX_SOURCE = "/assets/canvas/shader/default.vert";
    public final String DEFAULT_FRAGMENT_SOURCE = "/assets/canvas/shader/default.frag";
    
    PipelineShaderManager()
    {
        this.loadLibrarySources();
    }
    
    private void loadLibrarySources()
    {
        String commonSource = AbstractPipelineShader.getShaderSource("/assets/canvas/shader/common_lib.glsl");
        this.vertexLibrarySource = commonSource + AbstractPipelineShader.getShaderSource("/assets/canvas/shader/vertex_lib.glsl");
        this.fragmentLibrarySource = commonSource + AbstractPipelineShader.getShaderSource("/assets/canvas/shader/fragment_lib.glsl");
    }
    
    private String shaderKey(String shaderFileName, int spriteDepth, boolean isSolidLayer)
    {
        return String.format("%s.%s.%s", shaderFileName, spriteDepth, isSolidLayer);
    }
    
    public PipelineVertexShader getOrCreateVertexShader(String shaderFileName, int spriteDepth, boolean isSolidLayer)
    {
        final String shaderKey = shaderKey(shaderFileName, spriteDepth, isSolidLayer);
        
        synchronized(vertexShaders)
        {
            PipelineVertexShader result = vertexShaders.get(shaderKey);
            if(result == null)
            {
                result = new PipelineVertexShader(shaderFileName, spriteDepth, isSolidLayer);
                vertexShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public PipelineFragmentShader getOrCreateFragmentShader(String shaderFileName, int spriteDepth, boolean isSolidLayer)
    {
        final String shaderKey = shaderKey(shaderFileName, spriteDepth, isSolidLayer);
        
        synchronized(fragmentShaders)
        {
            PipelineFragmentShader result = fragmentShaders.get(shaderKey);
            if(result == null)
            {
                result = new PipelineFragmentShader(shaderFileName, spriteDepth, isSolidLayer);
                fragmentShaders.put(shaderKey, result);
            }
            return result;
        }
    }

    public void forceReload()
    {
        this.loadLibrarySources();
        this.fragmentShaders.values().forEach(s -> s.forceReload());
        this.vertexShaders.values().forEach(s -> s.forceReload());
    }
}
