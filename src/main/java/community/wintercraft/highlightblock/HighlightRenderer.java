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
            HighlightManager manager = HighlightManager.getInstance();
            if (!manager.isEnabled()) return;
            if (!manager.isOutlineEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;

            Camera camera = mc.gameRenderer.mainCamera();
            Vec3 camPos = camera.position();
            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();

            boolean seeThrough = manager.isSeeThrough();
            boolean haloEnabled = manager.isHaloEnabled();
            float coreWidth = manager.getOutlineWidth();
            float haloWidth = manager.getHaloWidth();
            float speed = manager.getAnimationSpeed();
            HighlightManager.AnimationMode mode = manager.getAnimationMode();

            double time = (System.currentTimeMillis() % 100_000_000L) / 1000.0 * speed;

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

                int haloColor;
                int coreColor;

                switch (mode) {
                    case RAINBOW -> {
                        // Smooth flowing chroma RGB wave across blocks
                        float hue = (float) (((time * 0.22) + (pos.getX() * 0.04 + pos.getY() * 0.06 + pos.getZ() * 0.04)) % 1.0);
                        if (hue < 0) hue += 1.0f;
                        int rgb = hsbToRgb(hue, 0.9f, 1.0f);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        float pulse = (float) ((Math.sin(time * 3.5 + (pos.getX() + pos.getZ()) * 0.3) + 1.0) * 0.5);
                        int haloAlpha = (int) (70 + 70 * pulse);
                        haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;

                        int coreAlpha = (int) (210 + 45 * pulse);
                        int cr = Math.min(255, (int) (r * 0.7f + 75));
                        int cg = Math.min(255, (int) (g * 0.7f + 75));
                        int cb = Math.min(255, (int) (b * 0.7f + 75));
                        coreColor = (coreAlpha << 24) | (cr << 16) | (cg << 8) | cb;
                    }
                    case ELECTRIC_CYAN -> {
                        // Cyberpunk electric cyan/azure breathing pulse
                        double wave = Math.sin(time * 3.2 + (pos.getX() * 0.35 + pos.getY() * 0.5 + pos.getZ() * 0.35));
                        float t = (float) ((wave + 1.0) * 0.5);

                        int r = (int) (0 + 100 * t);
                        int g = (int) (180 + 75 * t);
                        int b = 255;

                        int haloAlpha = (int) (80 + 70 * t);
                        haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;

                        int coreAlpha = (int) (215 + 40 * t);
                        int cr = Math.min(255, r + 60);
                        int cg = 255;
                        int cb = 255;
                        coreColor = (coreAlpha << 24) | (cr << 16) | (cg << 8) | cb;
                    }
                    case AMBER_GOLD -> {
                        // Warm glowing gold/amber ore pulse
                        double wave = Math.sin(time * 3.2 + (pos.getX() * 0.35 + pos.getY() * 0.5 + pos.getZ() * 0.35));
                        float t = (float) ((wave + 1.0) * 0.5);

                        int r = 255;
                        int g = (int) (140 + 95 * t);
                        int b = (int) (10 + 40 * t);

                        int haloAlpha = (int) (80 + 70 * t);
                        haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;

                        int coreAlpha = (int) (215 + 40 * t);
                        int cr = 255;
                        int cg = Math.min(255, g + 40);
                        int cb = Math.min(255, b + 60);
                        coreColor = (coreAlpha << 24) | (cr << 16) | (cg << 8) | cb;
                    }
                    case STATIC -> {
                        int base = manager.getOutlineColor();
                        haloColor = (0x60 << 24) | (base & 0x00FFFFFF);
                        coreColor = (0xFA << 24) | (base & 0x00FFFFFF);
                    }
                    case NEON_PURPLE -> {
                        // Animated glowing neon purple texturepack style:
                        // Spatial wave along vein + breathing intensity pulse
                        double wave = Math.sin(time * 3.2 + (pos.getX() * 0.35 + pos.getY() * 0.5 + pos.getZ() * 0.35));
                        float t = (float) ((wave + 1.0) * 0.5); // 0.0 .. 1.0

                        // Shifts between electric violet (150, 15, 255) and glowing neon magenta/orchid (255, 60, 255)
                        int r = (int) (150 + 105 * t);
                        int g = (int) (15 + 65 * t);
                        int b = 255;

                        // Outer soft glowing aura / bloom halo
                        int haloAlpha = (int) (85 + 75 * t);
                        haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;

                        // Inner intense luminous filament
                        int coreAlpha = (int) (220 + 35 * t);
                        int cr = Math.min(255, r + 45);
                        int cg = Math.min(255, g + 85);
                        int cb = 255;
                        coreColor = (coreAlpha << 24) | (cr << 16) | (cg << 8) | cb;
                    }
                    default -> {
                        double wave = Math.sin(time * 3.2 + (pos.getX() * 0.35 + pos.getY() * 0.5 + pos.getZ() * 0.35));
                        float t = (float) ((wave + 1.0) * 0.5);
                        int r = (int) (150 + 105 * t);
                        int g = (int) (15 + 65 * t);
                        int b = 255;
                        int haloAlpha = (int) (85 + 75 * t);
                        haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;
                        int coreAlpha = (int) (220 + 35 * t);
                        int cr = Math.min(255, r + 45);
                        int cg = Math.min(255, g + 85);
                        int cb = 255;
                        coreColor = (coreAlpha << 24) | (cr << 16) | (cg << 8) | cb;
                    }
                }

                // Pass 1: Outer soft glowing halo (bloom aura)
                if (haloEnabled) {
                    collector.submitShapeOutline(
                            poseStack,
                            shape,
                            RenderTypes.linesTranslucent(),
                            haloColor,
                            haloWidth,
                            seeThrough
                    );
                }

                // Pass 2: Inner bright neon core
                collector.submitShapeOutline(
                        poseStack,
                        shape,
                        RenderTypes.linesTranslucent(),
                        coreColor,
                        coreWidth,
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

    public static int hsbToRgb(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (t * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 1 -> { r = (int) (q * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (p * 255.0f + 0.5f); }
                case 2 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (brightness * 255.0f + 0.5f); b = (int) (t * 255.0f + 0.5f); }
                case 3 -> { r = (int) (p * 255.0f + 0.5f); g = (int) (q * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 4 -> { r = (int) (t * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (brightness * 255.0f + 0.5f); }
                case 5 -> { r = (int) (brightness * 255.0f + 0.5f); g = (int) (p * 255.0f + 0.5f); b = (int) (q * 255.0f + 0.5f); }
            }
        }
        return (r << 16) | (g << 8) | b;
    }
}
