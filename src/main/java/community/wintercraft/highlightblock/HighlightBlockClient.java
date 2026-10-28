package community.wintercraft.highlightblock;

import com.mojang.blaze3d.platform.InputConstants;
import community.wintercraft.highlightblock.gui.HighlightConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class HighlightBlockClient implements ClientModInitializer {
    public static final String MOD_ID = "highlightblock";

    private static KeyMapping keyToggleTarget;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        keyToggleTarget = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.highlightblock.toggle_target",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));

        // Auto-initialize pack on client startup (ensures fresh Modrinth downloads work out-of-the-box)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            HighlightManager.getInstance().ensurePackInitialized(client);
        });

        // Key handler with single-click consumption (prevents double triggers)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            if (keyToggleTarget.consumeClick()) {
                handleToggleTarget(client);
            }
        });

        registerCommands();
    }

    private void handleToggleTarget(Minecraft client) {
        // Prevent triggering while a reload is already taking place
        if (HighlightManager.isReloading()) {
            client.player.sendSystemMessage(Component.literal("[Highlight Block] Bitte kurz warten, Texturen werden noch geladen...")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            Block block = state.getBlock();
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);

            if (id != null) {
                boolean active = HighlightManager.getInstance().toggleBlock(block, client);
                Component msg = Component.literal("[Highlight Block] ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(block.getName().getString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(active ? " [Gelbes '!' Textur AKTIV]" : " [Normal zurückgesetzt]")
                                .withStyle(active ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
                client.player.sendSystemMessage(msg);
            }
        } else {
            client.player.sendSystemMessage(Component.literal("[Highlight Block] Schaue einen Block an, um ihn hervorzuheben!")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("highlightblock")
                    .then(ClientCommands.literal("config")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                client.execute(() -> client.gui.setScreen(new HighlightConfigScreen(client.gui.screen())));
                                return 1;
                            }))
                    .then(ClientCommands.literal("gui")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                client.execute(() -> client.gui.setScreen(new HighlightConfigScreen(client.gui.screen())));
                                return 1;
                            }))
                    .then(ClientCommands.literal("clear")
                            .executes(context -> {
                                Minecraft client = Minecraft.getInstance();
                                HighlightManager.getInstance().clearBlocks(client);
                                context.getSource().sendFeedback(Component.literal("[Highlight Block] Alle Block-Auswahlen gelöscht.").withStyle(ChatFormatting.GOLD));
                                return 1;
                            }))
                    .then(ClientCommands.literal("list")
                            .executes(context -> {
                                Set<String> list = HighlightManager.getInstance().getHighlightedBlockIds();
                                if (list.isEmpty()) {
                                    context.getSource().sendFeedback(Component.literal("[Highlight Block] Keine Blöcke ausgewählt.").withStyle(ChatFormatting.GRAY));
                                } else {
                                    context.getSource().sendFeedback(Component.literal("[Highlight Block] Aktive Blöcke:").withStyle(ChatFormatting.GOLD));
                                    for (String b : list) {
                                        context.getSource().sendFeedback(Component.literal(" - " + b).withStyle(ChatFormatting.WHITE));
                                    }
                                }
                                return 1;
                            }))
            );

            dispatcher.register(ClientCommands.literal("hb")
                    .redirect(dispatcher.getRoot().getChild("highlightblock"))
            );
        });
    }
}
