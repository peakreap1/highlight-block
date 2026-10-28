package community.wintercraft.highlightblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class HighlightManager {
    public static final int TINT_INDEX = 9999;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HighlightManager INSTANCE = new HighlightManager();

    public enum AnimationMode {
        NEON_PURPLE,
        RAINBOW,
        ELECTRIC_CYAN,
        AMBER_GOLD,
        STATIC
    }

    private boolean enabled = true;
    private boolean outlineEnabled = true;
    private boolean blockTintEnabled = false;
    private boolean fullbright = false;
    private boolean seeThrough = false; // No X-Ray: strictly occluded by walls
    private boolean haloEnabled = true;

    private AnimationMode animationMode = AnimationMode.NEON_PURPLE;
    private float animationSpeed = 1.0f;
    private float outlineWidth = 2.2f;
    private float haloWidth = 5.2f;
    private int outlineColor = 0xFFD000FF; // Vibrant electric purple
    private int highlightColor = 0xFFEE00;

    // Render & scan distance settings (in blocks)
    private int renderDistance = 64; // Default 64 blocks (previously was only 24)
    private int verticalDistance = 32; // Default 32 blocks up/down

    private final Set<String> highlightedBlockIds = new HashSet<>();
    private final HighlightTintSource tintSource = new HighlightTintSource();

    private HighlightManager() {
        // Default highlighted block: Andesite
        highlightedBlockIds.add(BuiltInRegistries.BLOCK.getKey(Blocks.ANDESITE).toString());
        load();
    }

    public static HighlightManager getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
        refreshWorld();
    }

    public boolean toggleEnabled() {
        setEnabled(!this.enabled);
        return this.enabled;
    }

    public boolean isOutlineEnabled() {
        return outlineEnabled;
    }

    public void setOutlineEnabled(boolean outlineEnabled) {
        this.outlineEnabled = outlineEnabled;
        save();
    }

    public boolean isBlockTintEnabled() {
        return blockTintEnabled;
    }

    public void setBlockTintEnabled(boolean blockTintEnabled) {
        this.blockTintEnabled = blockTintEnabled;
        save();
        refreshWorld();
    }

    public boolean isFullbright() {
        return fullbright;
    }

    public void setFullbright(boolean fullbright) {
        this.fullbright = fullbright;
        save();
        refreshWorld();
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        save();
    }

    public boolean isHaloEnabled() {
        return haloEnabled;
    }

    public void setHaloEnabled(boolean haloEnabled) {
        this.haloEnabled = haloEnabled;
        save();
    }

    public AnimationMode getAnimationMode() {
        return animationMode;
    }

    public void setAnimationMode(AnimationMode animationMode) {
        this.animationMode = animationMode;
        save();
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0.1f, Math.min(5.0f, animationSpeed));
        save();
    }

    public float getOutlineWidth() {
        return outlineWidth;
    }

    public void setOutlineWidth(float outlineWidth) {
        this.outlineWidth = Math.max(0.5f, Math.min(10.0f, outlineWidth));
        save();
    }

    public float getHaloWidth() {
        return haloWidth;
    }

    public void setHaloWidth(float haloWidth) {
        this.haloWidth = Math.max(1.0f, Math.min(15.0f, haloWidth));
        save();
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(int renderDistance) {
        this.renderDistance = Math.max(16, Math.min(96, renderDistance));
        save();
    }

    public int getVerticalDistance() {
        return verticalDistance;
    }

    public void setVerticalDistance(int verticalDistance) {
        this.verticalDistance = Math.max(8, Math.min(128, verticalDistance));
        save();
    }

    public int getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(int outlineColor) {
        this.outlineColor = outlineColor;
        save();
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(int color) {
        this.highlightColor = color;
        save();
        refreshWorld();
    }

    public HighlightTintSource getTintSource() {
        return tintSource;
    }

    public boolean isHighlighted(Block block) {
        if (!enabled) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && highlightedBlockIds.contains(id.toString());
    }

    public boolean isHighlightedForTint(Block block) {
        return blockTintEnabled && isHighlighted(block);
    }

    public boolean toggleBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return false;
        String idStr = id.toString();
        boolean activeNow;
        if (highlightedBlockIds.contains(idStr)) {
            highlightedBlockIds.remove(idStr);
            activeNow = false;
        } else {
            highlightedBlockIds.add(idStr);
            activeNow = true;
        }
        save();
        refreshWorld();
        return activeNow;
    }

    public void addBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            highlightedBlockIds.add(id.toString());
            save();
            refreshWorld();
        }
    }

    public void removeBlock(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id != null) {
            highlightedBlockIds.remove(id.toString());
            save();
            refreshWorld();
        }
    }

    public void clearBlocks() {
        highlightedBlockIds.clear();
        save();
        refreshWorld();
    }

    public Set<String> getHighlightedBlockIds() {
        return new HashSet<>(highlightedBlockIds);
    }

    public void refreshWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null && mc.level != null && mc.gameRenderer != null) {
            mc.levelRenderer.invalidateCompiledGeometry(
                    mc.level,
                    mc.options,
                    mc.gameRenderer.mainCamera(),
                    mc.getBlockColors()
            );
        }
    }

    private File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("highlightblock.json").toFile();
    }

    public void save() {
        try {
            File file = getConfigFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ConfigData data = new ConfigData();
            data.enabled = this.enabled;
            data.outlineEnabled = this.outlineEnabled;
            data.blockTintEnabled = this.blockTintEnabled;
            data.fullbright = this.fullbright;
            data.seeThrough = this.seeThrough;
            data.haloEnabled = this.haloEnabled;
            data.animationMode = this.animationMode != null ? this.animationMode.name() : "NEON_PURPLE";
            data.animationSpeed = this.animationSpeed;
            data.outlineColor = this.outlineColor;
            data.outlineWidth = this.outlineWidth;
            data.haloWidth = this.haloWidth;
            data.renderDistance = this.renderDistance;
            data.verticalDistance = this.verticalDistance;
            data.highlightColor = this.highlightColor;
            data.highlightedBlocks = this.highlightedBlockIds;
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            File file = getConfigFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        this.enabled = data.enabled;
                        this.outlineEnabled = data.outlineEnabled;
                        this.blockTintEnabled = data.blockTintEnabled;
                        this.fullbright = data.fullbright;
                        this.seeThrough = data.seeThrough;
                        this.haloEnabled = data.haloEnabled;
                        if (data.animationMode != null) {
                            try {
                                this.animationMode = AnimationMode.valueOf(data.animationMode.toUpperCase());
                            } catch (Exception ignored) {
                                this.animationMode = AnimationMode.NEON_PURPLE;
                            }
                        }
                        this.animationSpeed = data.animationSpeed > 0 ? data.animationSpeed : 1.0f;
                        this.outlineColor = data.outlineColor != 0 ? data.outlineColor : 0xFFD000FF;
                        this.outlineWidth = data.outlineWidth > 0 ? data.outlineWidth : 2.2f;
                        this.haloWidth = data.haloWidth > 0 ? data.haloWidth : 5.2f;
                        this.renderDistance = data.renderDistance >= 16 ? data.renderDistance : 64;
                        this.verticalDistance = data.verticalDistance >= 8 ? data.verticalDistance : 32;
                        this.highlightColor = data.highlightColor != 0 ? data.highlightColor : 0xFFEE00;
                        if (data.highlightedBlocks != null) {
                            this.highlightedBlockIds.clear();
                            this.highlightedBlockIds.addAll(data.highlightedBlocks);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        boolean enabled = true;
        boolean outlineEnabled = true;
        boolean blockTintEnabled = false;
        boolean fullbright = false;
        boolean seeThrough = false;
        boolean haloEnabled = true;
        String animationMode = "NEON_PURPLE";
        float animationSpeed = 1.0f;
        int outlineColor = 0xFFD000FF;
        float outlineWidth = 2.2f;
        float haloWidth = 5.2f;
        int renderDistance = 64;
        int verticalDistance = 32;
        int highlightColor = 0xFFEE00;
        Set<String> highlightedBlocks = new HashSet<>();
    }
}
