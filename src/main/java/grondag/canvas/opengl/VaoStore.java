package grondag.acuity.opengl;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.minecraft.client.renderer.GLAllocation;

public class VaoStore
{
    private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    private static final IntBuffer buff = GLAllocation.createDirectIntBuffer(128);
    
    public static int[] claimVertexArrays(int howMany)
    {
        int[] result = new int[howMany];
        for(int i = 0; i < howMany; i++)
        {
            result[i] = claimVertexArray();
        }
        return result;
    }
    
    public static int claimVertexArray()
    {
        if(queue.isEmpty())
        {
            OpenGlHelperExt.glGenVertexArrays(buff);
            
            for(int i = 0; i < 128; i++)
                queue.enqueue(buff.get(i));
            
            buff.clear();
        }
        
        return queue.dequeueInt();
    }
    
    public static void releaseVertexArray(int vaoBufferId)
    {
        queue.enqueue(vaoBufferId);
    }
    
    public static void releaseVertexArrays(int[] vaoBufferId)
    {
        for(int b : vaoBufferId)
            releaseVertexArray(b);
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
