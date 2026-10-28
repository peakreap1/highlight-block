package community.wintercraft.highlightblock.mixin;

import community.wintercraft.highlightblock.HighlightManager;
import community.wintercraft.highlightblock.HighlightModelWrapper;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateModelSet.class)
public class BlockStateModelSetMixin {

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void onGetModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        if (HighlightManager.getInstance().isHighlightedForModel(state.getBlock())) {
            BlockStateModel orig = cir.getReturnValue();
            if (orig != null) {
                cir.setReturnValue(HighlightModelWrapper.wrap(orig));
            }
        }
    }
}
