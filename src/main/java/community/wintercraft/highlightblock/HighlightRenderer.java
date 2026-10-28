package community.wintercraft.highlightblock;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighlightRenderer {
    private static final int SCAN_RADIUS_XZ = 24;
    private static final int SCAN_RADIUS_Y = 16;
    private static List<BlockPos> cachedBlocks = Collections.emptyList();
    private static int tickCounter = 0;

    public static void init() {
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (!HighlightManager.getInstance().isEnabled()) return;
            if (!HighlightManager.getInstance().isOutlineEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            Camera camera = mc.gameRenderer.mainCamera();
            Vec3 camPos = camera.position();
            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();

            int outlineColor = HighlightManager.getInstance().getOutlineColor();
            float lineWidth = HighlightManager.getInstance().getOutlineWidth();
            boolean seeThrough = HighlightManager.getInstance().isSeeThrough();

            List<BlockPos> blocksToRender = cachedBlocks;
            for (BlockPos pos : blocksToRender) {
                BlockState state = mc.level.getBlockState(pos);
                VoxelShape shape = state.getShape(mc.level, pos);
                if (shape.isEmpty()) continue;

                double rx = pos.getX() - camPos.x();
                double ry = pos.getY() - camPos.y();
                double rz = pos.getZ() - camPos.z();

                poseStack.pushPose();
                poseStack.translate(rx, ry, rz);

                collector.submitShapeOutline(
                        poseStack,
                        shape,
                        RenderTypes.lines(),
                        outlineColor,
                        lineWidth,
                        seeThrough
                );

                poseStack.popPose();
            }
        });
    }

    public static void clientTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        if (!HighlightManager.getInstance().isEnabled()) {
            cachedBlocks = Collections.emptyList();
            return;
        }

        tickCounter++;
        // Scan every 3 ticks (~6 times per second) for super responsive yet lightweight updates
        if (tickCounter % 3 != 0) return;

        BlockPos playerPos = mc.player.blockPosition();
        Level level = mc.level;
        HighlightManager manager = HighlightManager.getInstance();

        List<BlockPos> visibleList = new ArrayList<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        int px = playerPos.getX();
        int py = playerPos.getY();
        int pz = playerPos.getZ();

        for (int y = -SCAN_RADIUS_Y; y <= SCAN_RADIUS_Y; y++) {
            for (int x = -SCAN_RADIUS_XZ; x <= SCAN_RADIUS_XZ; x++) {
                for (int z = -SCAN_RADIUS_XZ; z <= SCAN_RADIUS_XZ; z++) {
                    mpos.set(px + x, py + y, pz + z);
                    BlockState state = level.getBlockState(mpos);

                    if (manager.isHighlighted(state.getBlock())) {
                        // Check if block has at least one exposed/visible face (fair play: not hidden in solid rock)
                        if (isExposed(level, mpos, neighborPos)) {
                            visibleList.add(mpos.immutable());
                        }
                    }
                }
            }
        }

        cachedBlocks = visibleList;
    }

    private static boolean isExposed(Level level, BlockPos pos, BlockPos.MutableBlockPos npos) {
        for (Direction dir : Direction.values()) {
            npos.setWithOffset(pos, dir);
            BlockState neighbor = level.getBlockState(npos);
            // If neighbor does not occlude the face, this face is exposed to air/light/water
            if (!neighbor.isSolidRender()) {
                return true;
            }
        }
        return false;
    }
}
