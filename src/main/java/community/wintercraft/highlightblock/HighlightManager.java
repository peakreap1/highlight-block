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

    private boolean enabled = true;
    private boolean fullbright = true;
    private int highlightColor = 0xFFEE00; // Brilliant Signal Yellow
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

    public boolean isFullbright() {
        return fullbright;
    }

    public void setFullbright(boolean fullbright) {
        this.fullbright = fullbright;
        save();
        refreshWorld();
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
            data.fullbright = this.fullbright;
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
                        this.fullbright = data.fullbright;
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
        boolean fullbright = true;
        int highlightColor = 0xFFEE00;
        Set<String> highlightedBlocks = new HashSet<>();
    }
}
