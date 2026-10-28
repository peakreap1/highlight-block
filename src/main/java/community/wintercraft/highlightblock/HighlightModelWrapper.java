package community.wintercraft.highlightblock;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public class HighlightModelWrapper implements BlockStateModel {
    private final BlockStateModel original;

    public HighlightModelWrapper(BlockStateModel original) {
        this.original = original;
    }

    public static BlockStateModel wrap(BlockStateModel original) {
        if (original instanceof HighlightModelWrapper) {
            return original;
        }
        return new HighlightModelWrapper(original);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> list) {
        List<BlockStateModelPart> temp = new ArrayList<>();
        original.collectParts(random, temp);
        for (BlockStateModelPart part : temp) {
            list.add(new HighlightModelPartWrapper(part));
        }
    }

    @Override
    public Material.Baked particleMaterial() {
        return original.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return original.materialFlags();
    }

    @Override
    public boolean hasMaterialFlag(int flag) {
        return original.hasMaterialFlag(flag);
    }
}
