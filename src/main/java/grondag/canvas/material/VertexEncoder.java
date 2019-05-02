package grondag.canvas.material;

public class VertexEncoder {
    private static final MaterialVertexFormat[] FORMATS = MaterialVertexFormat.values();
    
    public static MaterialVertexFormat format(int shaderProps) {
        return FORMATS[ShaderProps.spriteDepth(shaderProps) - 1];
    }
}
