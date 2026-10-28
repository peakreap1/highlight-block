package community.wintercraft.highlightblock;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collections;
import java.util.Set;

public class HighlightTintSource implements BlockTintSource {

    @Override
    public int color(BlockState state) {
        return HighlightManager.getInstance().getHighlightColor();
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return HighlightManager.getInstance().getHighlightColor();
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return HighlightManager.getInstance().getHighlightColor();
    }

    @Override
    public Set<Property<?>> relevantProperties() {
        return Collections.emptySet();
    }
}
