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
