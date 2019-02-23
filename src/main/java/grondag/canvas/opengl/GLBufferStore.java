package grondag.acuity.opengl;

import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL15;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;

/**
 * Buffer gen is incredibly slow on some Windows/NVidia systems and default MC behavior
 */
public class GLBufferStore
{
    private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    private static final IntBuffer buff = GLAllocation.createDirectIntBuffer(128);
    
    public static int claimBuffer()
    {
        if(queue.isEmpty())
        {
            if(OpenGlHelper.arbVbo)
                ARBVertexBufferObject.glGenBuffersARB(buff);
            else
                GL15.glGenBuffers(buff);
            
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
