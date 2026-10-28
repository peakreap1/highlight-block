package community.wintercraft.highlightblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HighlightModelPartWrapper implements BlockStateModelPart {
    private final BlockStateModelPart original;
    private static TextureAtlas cachedAtlas = null;
    private static TextureAtlasSprite cachedSprite = null;

    public HighlightModelPartWrapper(BlockStateModelPart original) {
        this.original = original;
    }

    public static TextureAtlasSprite getExclamationSprite() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getAtlasManager() == null) return null;
            TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
            if (atlas == null) return null;
            if (cachedAtlas == atlas && cachedSprite != null) {
                return cachedSprite;
            }
            TextureAtlasSprite sprite = atlas.getSprite(Identifier.fromNamespaceAndPath("highlightblock", "block/exclamation_mark"));
            if (sprite != null && sprite != atlas.missingSprite()) {
                cachedAtlas = atlas;
                cachedSprite = sprite;
                return sprite;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static void resetSpriteCache() {
        cachedAtlas = null;
        cachedSprite = null;
    }

    @Override
    public List<BakedQuad> getQuads(Direction direction) {
        List<BakedQuad> origQuads = original.getQuads(direction);
        if (origQuads == null || origQuads.isEmpty()) {
            return origQuads;
        }

        HighlightManager manager = HighlightManager.getInstance();
        boolean textureSwap = manager.isTextureSwapEnabled();
        boolean fullbright = manager.isFullbright();
        boolean tint = manager.isBlockTintEnabled();

        if (!textureSwap && !fullbright && !tint) {
            return origQuads;
        }

        TextureAtlasSprite exclamationSprite = textureSwap ? getExclamationSprite() : null;
        List<BakedQuad> result = new ArrayList<>(origQuads.size());

        for (BakedQuad q : origQuads) {
            BakedQuad.MaterialInfo origMat = q.materialInfo();
            if (exclamationSprite != null) {
                TextureAtlasSprite origSprite = origMat.sprite();
                long uv0 = remapUV(q.packedUV0(), origSprite, exclamationSprite);
                long uv1 = remapUV(q.packedUV1(), origSprite, exclamationSprite);
                long uv2 = remapUV(q.packedUV2(), origSprite, exclamationSprite);
                long uv3 = remapUV(q.packedUV3(), origSprite, exclamationSprite);

                BakedQuad.MaterialInfo newMat = new BakedQuad.MaterialInfo(
                        exclamationSprite,
                        origMat.layer(),
                        origMat.itemRenderType(),
                        -1, // No tint index, preserve pure yellow
                        false, // No shading
                        15 // Fullbright glow
                );

                result.add(new BakedQuad(
                        q.position0(), q.position1(), q.position2(), q.position3(),
                        uv0, uv1, uv2, uv3,
                        q.direction(),
                        newMat
                ));
            } else {
                BakedQuad.MaterialInfo newMat = new BakedQuad.MaterialInfo(
                        origMat.sprite(),
                        origMat.layer(),
                        origMat.itemRenderType(),
                        tint ? HighlightManager.TINT_INDEX : origMat.tintIndex(),
                        !fullbright,
                        fullbright ? 15 : origMat.lightEmission()
                );

                result.add(new BakedQuad(
                        q.position0(), q.position1(), q.position2(), q.position3(),
                        q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                        q.direction(),
                        newMat
                ));
            }
        }

        return result;
    }

    private static long remapUV(long packedUV, TextureAtlasSprite origSprite, TextureAtlasSprite targetSprite) {
        float origU = UVPair.unpackU(packedUV);
        float origV = UVPair.unpackV(packedUV);

        float spanU = origSprite.getU1() - origSprite.getU0();
        float spanV = origSprite.getV1() - origSprite.getV0();

        float relU = (spanU > 0.00001f || spanU < -0.00001f) ? (origU - origSprite.getU0()) / spanU : 0.0f;
        float relV = (spanV > 0.00001f || spanV < -0.00001f) ? (origV - origSprite.getV0()) / spanV : 0.0f;

        relU = Math.max(0.0f, Math.min(1.0f, relU));
        relV = Math.max(0.0f, Math.min(1.0f, relV));

        float newU = targetSprite.getU(relU);
        float newV = targetSprite.getV(relV);

        return UVPair.pack(newU, newV);
    }

    @Override
    public boolean useAmbientOcclusion() {
        HighlightManager manager = HighlightManager.getInstance();
        if (manager.isTextureSwapEnabled() || manager.isFullbright()) {
            return false;
        }
        return original.useAmbientOcclusion();
    }

    @Override
    public Material.Baked particleMaterial() {
        return original.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return original.materialFlags();
    }
}
