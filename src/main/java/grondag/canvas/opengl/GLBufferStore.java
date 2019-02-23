package grondag.canvas.opengl;

import java.nio.IntBuffer;

import com.mojang.blaze3d.platform.GLX;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.minecraft.client.util.GlAllocationUtils;

/**
 * Buffer gen is incredibly slow on some Windows/NVidia systems and default MC behavior
 */
public class GLBufferStore
{
    private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    private static final IntBuffer buff = GlAllocationUtils.allocateByteBuffer(128 * 4).asIntBuffer();
    
    public static int claimBuffer()
    {
        if(queue.isEmpty())
        {
            GLX.glGenBuffers(buff);
            
            for(int i = 0; i < 128; i++)
                queue.enqueue(buff.get(i));
            
            buff.clear();
        }
        
        return queue.dequeueInt();
    }
    
    public static void releaseBuffer(int buffer)
    {
        queue.enqueue(buffer);
    }
    
    // should not be needed - Gl resources are destroyed when the context is destroyed
//    public static void deleteAll()
//    {
//        while(!queue.isEmpty())
//        {
//            while(!queue.isEmpty() && buff.position() < 128)
//                buff.put(queue.dequeueInt());
//            
//            if(OpenGlHelper.arbVbo)
//                ARBVertexBufferObject.glDeleteBuffersARB(buff);
//            else
//                GL15.glDeleteBuffers(buff);
//            
//            buff.clear();
//        }
//    }
}
