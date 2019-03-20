# Canvas
Canvas is a shader-based Renderer for the [Fabric](https://fabricmc.net) modding toolchain.  It supports all features of the proposed [Fabric Rendering API](https://github.com/FabricMC/fabric/pull/65) plus extensions defined in [FREX](https://github.com/grondag/frex).

## Limitations
Canvas is in EARLY ALPHA.  Expect it to break.  Currently terrain rendering works with shaders but item rendering does not. (It will.). Rendering for blocks in movement (falling blocks, blocks being moved by pistons, etc.) is WIP and *will* crash.  

The FREX extensions, shader library, vertex formats, attribute bindings, and lighting options are subject to change - causing your code to break.  Sorry.  When there is a stable release (sometime after 1.14 is released) I will avoid breaking changes in shipping versions.  Until then, experimentation is the norm.

## Why
People new to Canvas usually ask if it is a performance mod or a replacement for Optifine / shader packs.  The answer is "no, but..."

Optifine and shader packs primarily target vanilla Minecraft.  They work with modded, often well, but they aren't designed as tools for *mod authors*.

Canvas' entire purpose is to give mod authors more control and options for rendering modded blocks.  It *could* be used for building a shader pack by replacing vanilla models with material-aware models that have custom shaders.  But that isn't its main reason for being.

Performance-wise, Canvas does try to be reasonably fast and has/will have optimizations - but the intent of these changes is to make better rendering practical, not to be a general-purpose performance mod. It isn't meant to run on low-end hardware and  may or may not make your game run faster overall.

Currently, there are two main optimizations.  1) Canvas buffers and renders SOLID and CUTOUT layers in a single pass with cutout and mip mapping variation handled in the fragment shader.  2) Canvas buffers and renders multiple chunks using the same matrix transform to reduce the number of GL state changes.

Additional optimizations will wait until 1.14 is released and stable.

## Using Canvas
Before using Canvas, you should first understand RenderMaterials, Meshes, RenderContexts and other features defined by the Fabric Rendering API.  For that information, consult the [rendering article on the Fabric Wiki](https://fabricmc.net/wiki/rendering).

### Overlay Sprites
Canvas supports a max sprite depth of three - meaning you can add one or two overlap sprites to each quad.  For example, to add one overlay texture, choose a material with a sprite depth of two:

```java
RendderMaterial mat = RendererAccess.INSTANCE.getRenderer().finder().spriteDepth(2)
//select other material properties...
.find();
```

Specify UV coordinates, blend mode and colors for your overlay sprites like so:
```java
  .blendMode(1, TRANSLUCENT)
  .disableColorIndex(1, true)
  .spriteBake(1, sprite, MutableQuadView.BAKE_LOCK_UV | MutableQuadView.BAKE_NORMALIZED)
  .spriteColor(1, -1, -1, -1, -1)
```

Note that is doesn't make sense to use `BlockRenderLayer.SOLID` as the blend mode for overlay textures - it would cover up the texture beneath it.  Use `TRANSLUCENT` or one of the `CUTOUT` modes instead.


### Custom Shaders
