/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.mixin.perf;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import grondag.acuity.mixin.extension.MutableBoundingBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;

@Mixin(BoundingBox.class)
public abstract class MixinBoundingBox implements MutableBoundingBox
{
    @Shadow @Final @Mutable private double minX;
    @Shadow @Final @Mutable private double minY;
    @Shadow @Final @Mutable private double minZ;
    @Shadow @Final @Mutable private double maxX;
    @Shadow @Final @Mutable private double maxY;
    @Shadow @Final @Mutable private double maxZ;
    
    @Override
    public MutableBoundingBox set(BoundingBox box)
    {
        this.minX = box.minX;
        this.minY = box.minY;
        this.minZ = box.minZ;
        this.maxX = box.maxX;
        this.maxY = box.maxY;
        this.maxZ = box.maxZ;
        return this;
    }
    
    @Override
    public MutableBoundingBox growMutable(double x, double y, double z)
    {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    @Override
    public MutableBoundingBox growMutable(double value)
    {
        return this.growMutable(value, value, value);
    }

    @Override
    public MutableBoundingBox offsetMutable(BlockPos pos)
    {
        this.minX += (double)pos.getX();
        this.minY += (double)pos.getY();
        this.minZ += (double)pos.getZ();
        this.maxX += (double)pos.getX();
        this.maxY += (double)pos.getY();
        this.maxZ += (double)pos.getZ();
        return this;
    }
    
    @Override
    public MutableBoundingBox expandMutable(double x, double y, double z)
    {
        if (x < 0.0D)
        {
            this.minX += x;
        }
        else if (x > 0.0D)
        {
            this.maxX += x;
        }

        if (y < 0.0D)
        {
            this.minY += y;
        }
        else if (y > 0.0D)
        {
            this.maxY += y;
        }

        if (z < 0.0D)
        {
            this.minZ += z;
        }
        else if (z > 0.0D)
        {
            this.maxZ += z;
        }
        
        return this;
    }
    
    @Override
    public BoundingBox cast()
    {
        return (BoundingBox)(Object)this;
    }

    @Override
    public BoundingBox toImmutable()
    {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
