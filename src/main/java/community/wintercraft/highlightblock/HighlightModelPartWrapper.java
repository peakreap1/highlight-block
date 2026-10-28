package community.wintercraft.highlightblock;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class HighlightModelPartWrapper implements BlockStateModelPart {
    private final BlockStateModelPart original;

    public HighlightModelPartWrapper(BlockStateModelPart original) {
        this.original = original;
    }

    @Override
    public List<BakedQuad> getQuads(Direction direction) {
        List<BakedQuad> origQuads = original.getQuads(direction);
        if (origQuads == null || origQuads.isEmpty()) {
            return origQuads;
        }

        boolean fullbright = HighlightManager.getInstance().isFullbright();
        List<BakedQuad> highlightedQuads = new ArrayList<>(origQuads.size());

        for (BakedQuad q : origQuads) {
            BakedQuad.MaterialInfo origMat = q.materialInfo();
            BakedQuad.MaterialInfo newMat = new BakedQuad.MaterialInfo(
                    origMat.sprite(),
                    origMat.layer(),
                    origMat.itemRenderType(),
                    HighlightManager.TINT_INDEX,
                    !fullbright, // shade (false if fullbright)
                    fullbright ? 15 : origMat.lightEmission() // glowing fullbright
            );

            BakedQuad newQuad = new BakedQuad(
                    q.position0(), q.position1(), q.position2(), q.position3(),
                    q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
                    q.direction(),
                    newMat
            );
            highlightedQuads.add(newQuad);
        }

        return highlightedQuads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return !HighlightManager.getInstance().isFullbright() && original.useAmbientOcclusion();
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
