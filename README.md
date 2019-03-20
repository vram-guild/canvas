# Canvas
Canvas is a shader-based Renderer for the [Fabric](https://fabricmc.net) modding toolchain.  It supports all features of the proposed [Fabric Rendering API](https://github.com/FabricMC/fabric/pull/65) plus extensions defined in [FREX](https://github.com/grondag/frex).

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
