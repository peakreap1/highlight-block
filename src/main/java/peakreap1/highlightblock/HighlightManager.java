package peakreap1.highlightblock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HighlightManager {
    public static final String PACK_FOLDER_NAME = "Highlight Block";
    public static final String PACK_ID = "file/" + PACK_FOLDER_NAME;
    private static final Logger LOGGER = LoggerFactory.getLogger("HighlightBlock");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HighlightManager INSTANCE = new HighlightManager();

    private final Set<String> highlightedBlockIds = new LinkedHashSet<>();
    private static byte[] exclamationBytesCache = null;

    private static boolean reloading = false;
    private static long lastReloadTime = 0;

    private HighlightManager() {
        // Default highlighted block: Andesite
        highlightedBlockIds.add(BuiltInRegistries.BLOCK.getKey(Blocks.ANDESITE).toString());
        load();
    }

    public static HighlightManager getInstance() {
        return INSTANCE;
    }

    public static boolean isReloading() {
        return reloading || (System.currentTimeMillis() - lastReloadTime < 2500);
    }

    public static boolean isReloading(Minecraft mc) {
        if (reloading || (System.currentTimeMillis() - lastReloadTime < 2500)) {
            return true;
        }
        if (mc != null && mc.gui != null && mc.gui.overlay() != null) {
            return true;
        }
        return false;
    }

    public boolean isHighlighted(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null && highlightedBlockIds.contains(id.toString());
    }

    public boolean toggleBlock(Block block, Minecraft mc) {
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
        if (mc != null) {
            applyTexturePack(mc);
        }
        return activeNow;
    }

    public void clearBlocks(Minecraft mc) {
        highlightedBlockIds.clear();
        save();
        if (mc != null) {
            applyTexturePack(mc);
        }
    }

    public Set<String> getHighlightedBlockIds() {
        return Collections.unmodifiableSet(highlightedBlockIds);
    }

    public static byte[] getExclamationMarkBytes() {
        if (exclamationBytesCache != null) {
            return exclamationBytesCache;
        }
        try (InputStream in = HighlightManager.class.getResourceAsStream("/assets/highlightblock/textures/block/exclamation_mark.png")) {
            if (in != null) {
                exclamationBytesCache = in.readAllBytes();
                LOGGER.info("[HighlightBlock] Loaded exclamation mark texture bytes: {} bytes", exclamationBytesCache.length);
                return exclamationBytesCache;
            }
        } catch (Exception e) {
            LOGGER.error("[HighlightBlock] Failed to load exclamation mark texture", e);
        }
        return null;
    }

    private static final Set<String> EXCLUDED_GENERIC_TEXTURES = Set.of(
        "block/stone",
        "block/deepslate",
        "block/dirt",
        "block/cobblestone",
        "block/cobbled_deepslate",
        "block/smooth_stone",
        "block/bedrock",
        "block/netherrack",
        "block/end_stone",
        "block/sand",
        "block/gravel"
    );

    private static boolean shouldIncludeTexture(String texPath, String blockPath) {
        if (texPath == null || blockPath == null) return false;

        // If it's a generic base material, only include it if the block itself is that material
        for (String generic : EXCLUDED_GENERIC_TEXTURES) {
            if (texPath.equals(generic)) {
                String genericName = generic.substring(generic.lastIndexOf('/') + 1);
                return blockPath.equals(genericName);
            }
        }

        // 1. Direct or substring match (e.g. "block/redstone_ore", "block/deepslate_redstone_ore")
        if (texPath.contains(blockPath)) {
            return true;
        }

        // 2. Deepslate variants (e.g. block "deepslate_redstone_ore", tex "block/redstone_ore/deepslate_off")
        if (blockPath.startsWith("deepslate_")) {
            String base = blockPath.substring("deepslate_".length()); // e.g. "redstone_ore"
            String baseNoOre = base.replace("_ore", ""); // e.g. "redstone"
            if (texPath.contains("deepslate") && (texPath.contains(base) || texPath.contains(baseNoOre))) {
                return true;
            }
        }

        // 3. Ore variants (e.g. block "redstone_ore", tex "block/redstone_ore/off" or "block/redstone_ore/on")
        if (blockPath.endsWith("_ore")) {
            String baseOre = blockPath.replace("_ore", ""); // e.g. "redstone"
            if (texPath.contains(baseOre)) {
                return true;
            }
        }

        // 4. Token matching: all non-empty tokens of blockPath are contained in texPath
        if (blockPath.contains("_")) {
            String[] parts = blockPath.split("_");
            boolean allParts = true;
            for (String p : parts) {
                if (!p.isEmpty() && !texPath.contains(p)) {
                    allParts = false;
                    break;
                }
            }
            if (allParts) return true;
        }

        // 5. If texPath is placed in a folder dedicated to this block
        if (texPath.contains("/" + blockPath + "/")) {
            return true;
        }

        return false;
    }

    public static Set<String> getTexturePathsForBlock(Minecraft mc, Block block) {
        Set<String> list = new LinkedHashSet<>();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) return list;

        String ns = blockId.getNamespace();
        String path = blockId.getPath();

        // 1. Extract textures from all possible block model states and quads
        try {
            if (mc != null && mc.getModelManager() != null) {
                BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
                if (modelSet != null) {
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        BlockStateModel model = modelSet.get(state);
                        if (model == null) continue;

                        List<BlockStateModelPart> parts = new ArrayList<>();
                        model.collectParts(RandomSource.create(42L), parts);
                        for (BlockStateModelPart part : parts) {
                            for (Direction dir : Direction.values()) {
                                List<BakedQuad> quads = part.getQuads(dir);
                                if (quads != null) {
                                    for (BakedQuad q : quads) {
                                        if (q.materialInfo() != null && q.materialInfo().sprite() != null && q.materialInfo().sprite().contents() != null) {
                                            Identifier tex = q.materialInfo().sprite().contents().name();
                                            if (tex != null && shouldIncludeTexture(tex.getPath(), path)) {
                                                list.add("assets/" + tex.getNamespace() + "/textures/" + tex.getPath() + ".png");
                                            }
                                        }
                                    }
                                }
                            }
                            List<BakedQuad> unculled = part.getQuads(null);
                            if (unculled != null) {
                                for (BakedQuad q : unculled) {
                                    if (q.materialInfo() != null && q.materialInfo().sprite() != null && q.materialInfo().sprite().contents() != null) {
                                        Identifier tex = q.materialInfo().sprite().contents().name();
                                        if (tex != null && shouldIncludeTexture(tex.getPath(), path)) {
                                            list.add("assets/" + tex.getNamespace() + "/textures/" + tex.getPath() + ".png");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[HighlightBlock] Error inspecting model for block {}", blockId, t);
        }

        // 2. Guaranteed standard texture paths
        list.add("assets/" + ns + "/textures/block/" + path + ".png");
        list.add("assets/" + ns + "/textures/block/" + path + "_top.png");
        list.add("assets/" + ns + "/textures/block/" + path + "_side.png");
        list.add("assets/" + ns + "/textures/block/" + path + "_bottom.png");

        // 3. Explicit fallbacks for redstone ore variants across popular resource packs (e.g. Redstone Tweaks)
        if (path.equals("redstone_ore")) {
            list.add("assets/minecraft/textures/block/lit_redstone_ore.png");
            list.add("assets/minecraft/textures/block/redstone_ore/off.png");
            list.add("assets/minecraft/textures/block/redstone_ore/on.png");
            list.add("assets/minecraft/textures/block/redstone_ore/redstone_ore.png");
            list.add("assets/minecraft/textures/block/redstone_ore/lit_redstone_ore.png");
        } else if (path.equals("deepslate_redstone_ore")) {
            list.add("assets/minecraft/textures/block/lit_deepslate_redstone_ore.png");
            list.add("assets/minecraft/textures/block/deepslate_redstone_ore_top.png");
            list.add("assets/minecraft/textures/block/deepslate_redstone_ore_side.png");
            list.add("assets/minecraft/textures/block/deepslate_redstone_ore_bottom.png");
            list.add("assets/minecraft/textures/block/redstone_ore/deepslate_off.png");
            list.add("assets/minecraft/textures/block/redstone_ore/deepslate_on.png");
            list.add("assets/minecraft/textures/block/deepslate/redstone_ore_off.png");
            list.add("assets/minecraft/textures/block/deepslate/redstone_ore_on.png");
        }

        return list;
    }

    /**
     * Ensures the resource pack folder, pack.mcmeta, and pack.png are created on disk.
     * Guaranteed to work for fresh installs downloaded directly from Modrinth.
     */
    public void ensurePackInitialized(Minecraft mc) {
        try {
            Path resourcePackDir = mc.getResourcePackDirectory();
            if (resourcePackDir == null) return;

            Path packDir = resourcePackDir.resolve(PACK_FOLDER_NAME);
            Files.createDirectories(packDir);

            Path mcmeta = packDir.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                String metaContent = "{\n  \"pack\": {\n    \"pack_format\": 88,\n    \"min_format\": 84,\n    \"max_format\": 100,\n    \"description\": \"§e§lHighlight Block §7[26.2]\\n§6Dynamic Block Spotlight Texture Pack (!)\"\n  }\n}";
                Files.writeString(mcmeta, metaContent, StandardCharsets.UTF_8);
            }

            Path packPng = packDir.resolve("pack.png");
            byte[] exclamationBytes = getExclamationMarkBytes();
            if (!Files.exists(packPng) && exclamationBytes != null) {
                Files.write(packPng, exclamationBytes);
            }

            // Write initial textures if not yet present
            Path assetsDir = packDir.resolve("assets");
            if (!Files.exists(assetsDir) && !highlightedBlockIds.isEmpty() && exclamationBytes != null) {
                for (String idStr : highlightedBlockIds) {
                    Identifier id = Identifier.tryParse(idStr);
                    if (id != null) {
                        Block block = BuiltInRegistries.BLOCK.getValue(id);
                        if (block != null && block != Blocks.AIR) {
                            Set<String> relPaths = getTexturePathsForBlock(mc, block);
                            for (String relPath : relPaths) {
                                Path targetFile = packDir.resolve(relPath);
                                Files.createDirectories(targetFile.getParent());
                                Files.write(targetFile, exclamationBytes);
                            }
                        }
                    }
                }
            }

            // Ensure the pack is registered and selected with highest priority
            PackRepository repo = mc.getResourcePackRepository();
            repo.reload();
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
            if (!selected.contains(PACK_ID)) {
                selected.add(PACK_ID);
                repo.setSelected(selected);
                mc.options.updateResourcePacks(repo);
                mc.options.save();
            } else if (!selected.get(selected.size() - 1).equals(PACK_ID)) {
                selected.remove(PACK_ID);
                selected.add(PACK_ID);
                repo.setSelected(selected);
                mc.options.updateResourcePacks(repo);
                mc.options.save();
            }
        } catch (Exception e) {
            LOGGER.error("[HighlightBlock] Failed to initialize default pack", e);
        }
    }

    public void applyTexturePack(Minecraft mc) {
        mc.execute(() -> {
            try {
                Path resourcePackDir = mc.getResourcePackDirectory();
                if (resourcePackDir == null) return;

                Path packDir = resourcePackDir.resolve(PACK_FOLDER_NAME);
                Files.createDirectories(packDir);

                // Write pack.mcmeta
                Path mcmeta = packDir.resolve("pack.mcmeta");
                String metaContent = "{\n  \"pack\": {\n    \"pack_format\": 88,\n    \"min_format\": 84,\n    \"max_format\": 100,\n    \"description\": \"§e§lHighlight Block §7[26.2]\\n§6Dynamic Block Spotlight Texture Pack (!)\"\n  }\n}";
                Files.writeString(mcmeta, metaContent, StandardCharsets.UTF_8);

                // Write pack.png
                byte[] exclamationBytes = getExclamationMarkBytes();
                Path packPng = packDir.resolve("pack.png");
                if (!Files.exists(packPng) && exclamationBytes != null) {
                    Files.write(packPng, exclamationBytes);
                }

                // Clean existing textures in packDir/assets
                Path assetsDir = packDir.resolve("assets");
                if (Files.exists(assetsDir)) {
                    deleteDirectoryRecursively(assetsDir);
                }

                // If we have selected blocks, generate the exclamation mark textures
                boolean shouldBeActive = !highlightedBlockIds.isEmpty();
                if (shouldBeActive && exclamationBytes != null) {
                    for (String idStr : highlightedBlockIds) {
                        Identifier id = Identifier.tryParse(idStr);
                        if (id != null) {
                            Block block = BuiltInRegistries.BLOCK.getValue(id);
                            if (block != null && block != Blocks.AIR) {
                                Set<String> relPaths = getTexturePathsForBlock(mc, block);
                                for (String relPath : relPaths) {
                                    Path targetFile = packDir.resolve(relPath);
                                    Files.createDirectories(targetFile.getParent());
                                    Files.write(targetFile, exclamationBytes);
                                    LOGGER.info("[HighlightBlock] Generated texture: {}", relPath);
                                }
                            }
                        }
                    }
                }

                // Synchronize pack selection in Minecraft
                // The pack ALWAYS stays in repo.getSelectedIds()!
                // This guarantees updateResourcePacks() does NOT trigger a second reload.
                PackRepository repo = mc.getResourcePackRepository();
                repo.reload();

                reloading = true;
                lastReloadTime = System.currentTimeMillis();

                List<String> currentSelected = new ArrayList<>(repo.getSelectedIds());
                boolean needUpdate = false;
                if (!currentSelected.contains(PACK_ID)) {
                    currentSelected.add(PACK_ID);
                    needUpdate = true;
                } else if (!currentSelected.get(currentSelected.size() - 1).equals(PACK_ID)) {
                    currentSelected.remove(PACK_ID);
                    currentSelected.add(PACK_ID);
                    needUpdate = true;
                }

                if (needUpdate) {
                    repo.setSelected(currentSelected);
                    // updateResourcePacks() calls mc.reloadResourcePacks() internally on change!
                    mc.options.updateResourcePacks(repo);
                    mc.options.save();
                    LOGGER.info("[HighlightBlock] Pack selection updated to top priority via updateResourcePacks.");
                    reloading = false;
                } else {
                    // Normal case: pack is already active and at top priority. Trigger exactly ONE reload!
                    CompletableFuture<Void> future = mc.reloadResourcePacks();
                    if (future != null) {
                        future.whenComplete((res, err) -> {
                            reloading = false;
                            LOGGER.info("[HighlightBlock] Single reload finished successfully.");
                        });
                    } else {
                        reloading = false;
                    }
                }
            } catch (Exception e) {
                reloading = false;
                LOGGER.error("[HighlightBlock] Failed to apply texture pack", e);
            }
        });
    }

    private static void deleteDirectoryRecursively(Path path) {
        try {
            if (!Files.exists(path)) return;
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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
            data.highlightedBlocks = new HashSet<>(this.highlightedBlockIds);
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
                    if (data != null && data.highlightedBlocks != null) {
                        this.highlightedBlockIds.clear();
                        this.highlightedBlockIds.addAll(data.highlightedBlocks);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        Set<String> highlightedBlocks = new HashSet<>();
    }
}
