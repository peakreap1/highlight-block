package community.wintercraft.highlightblock.mixin;

import community.wintercraft.highlightblock.HighlightManager;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {

    @Inject(method = "getTintSource", at = @At("HEAD"), cancellable = true)
    private void onGetTintSource(BlockState state, int tintIndex, CallbackInfoReturnable<BlockTintSource> cir) {
        if (tintIndex == HighlightManager.TINT_INDEX) {
            cir.setReturnValue(HighlightManager.getInstance().getTintSource());
        }
    }
}
