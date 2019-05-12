package grondag.canvas.light;

import grondag.fermion.varia.BitPacker64;
import grondag.fermion.varia.BitPacker64.IntElement;

@SuppressWarnings("rawtypes")
public final class LightKey {
    private static final BitPacker64<Void> PACKER = new BitPacker64<Void>(null, null);
    
    private static final IntElement CENTER = PACKER.createIntElement(-1, 60);
    
    private static final IntElement TOP = PACKER.createIntElement(-1, 60);
    private static final IntElement LEFT = PACKER.createIntElement(-1, 60);
    private static final IntElement RIGHT = PACKER.createIntElement(-1, 60);
    private static final IntElement BOTTOM = PACKER.createIntElement(-1, 60);
    
    private static final IntElement TOP_LEFT = PACKER.createIntElement(-1, 60);
    private static final IntElement TOP_RIGHT = PACKER.createIntElement(-1, 60);
    private static final IntElement BOTTOM_LEFT = PACKER.createIntElement(-1, 60);
    private static final IntElement BOTTOM_RIGHT = PACKER.createIntElement(-1, 60);
    
    static long toKey(
            int top,
            int left,
            int right,
            int bottom,
            int topLeft,
            int topRight,
            int bottomLeft,
            int bottomRight,
            int center)
    {
        long result = CENTER.setValue(clamp(center), 0);
        
        result = TOP.setValue(clamp(top), result);
        result = LEFT.setValue(clamp(left), result);
        result = RIGHT.setValue(clamp(right), result);
        result = BOTTOM.setValue(clamp(bottom), result);
        
        result = TOP_LEFT.setValue(clamp(topLeft), result);
        result = TOP_RIGHT.setValue(clamp(topRight), result);
        result = BOTTOM_LEFT.setValue(clamp(bottomLeft), result);
        result = BOTTOM_RIGHT.setValue(clamp(bottomRight), result);
        
        return result;
    }
    
    private static int clamp(int val) {
        if(val < 0 || val == 0xFF) {
            return -1;
        } else if(val > 240) {
            return 60;
        }
        return val >> 2; // 0-60
    }
    
    private static int unclamp(int val) {
        return val == -1 ? -1 : val << 2;
    }
    
    public static int center(long key) {
        return unclamp(CENTER.getValue(key));
    }
    
    public static int top(long key) {
        return unclamp(TOP.getValue(key));
    }
    
    public static int left(long key) {
        return unclamp(LEFT.getValue(key));
    }
    
    public static int right(long key) {
        return unclamp(RIGHT.getValue(key));
    }
    
    public static int bottom(long key) {
        return unclamp(BOTTOM.getValue(key));
    }
    
    public static int topLeft(long key) {
        return unclamp(TOP_LEFT.getValue(key));
    }
    
    public static int topRight(long key) {
        return unclamp(TOP_RIGHT.getValue(key));
    }
    
    public static int bottomLeft(long key) {
        return unclamp(BOTTOM_LEFT.getValue(key));
    }
    
    public static int bottomRight(long key) {
        return unclamp(BOTTOM_RIGHT.getValue(key));
    }
}
