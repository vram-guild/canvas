package grondag.canvas.opengl;

import org.lwjgl.opengl.GL20;

public class CanvasGlHelper {
    static int attributeEnabledCount = 0;
    
    /**
     * Disables all generic vertex attributes and resets tracking state.
     * Use after calling {@link #enableAttributesVao(int)}
     */
    public static void resetAttributes()
    {
        for(int i = 0; i < 6; i++)
        {
            GL20.glDisableVertexAttribArray(i);
        }
        attributeEnabledCount = 0;
    }
    
    /**
     * Like {@link CanvasGlHelper#enableAttributes(int)} but enables all attributes 
     * regardless of prior state. Tracking state for {@link CanvasGlHelper#enableAttributes(int)} remains unchanged.
     * Used to initialize VAO state
     */
    public static void enableAttributesVao(int enabledCount)
    {
        for(int i = 1; i <= enabledCount; i++)
        {
            GL20.glEnableVertexAttribArray(i);
        }
    }
    /**
     * Enables the given number of generic vertex attributes if not already enabled.
     * Using 1-based numbering for attribute slots because GL (on my machine at least) not liking slot 0.
     */
    public static void enableAttributes(int enabledCount)
    {
        if(enabledCount > attributeEnabledCount)
        {
            while(enabledCount > attributeEnabledCount)
                GL20.glEnableVertexAttribArray(1 + attributeEnabledCount++);
        }
        else if(enabledCount < attributeEnabledCount)
        {
            while(enabledCount < attributeEnabledCount)
                GL20.glDisableVertexAttribArray(--attributeEnabledCount + 1);
        }
    }
}
