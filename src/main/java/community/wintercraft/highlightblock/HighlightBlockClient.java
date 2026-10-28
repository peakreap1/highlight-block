package community.wintercraft.highlightblock;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
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
    private static KeyMapping keyToggleAll;

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

        keyToggleAll = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.highlightblock.toggle_all",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        // Initialize Level Outline Renderer
        HighlightRenderer.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            while (keyToggleTarget.consumeClick()) {
                handleToggleTarget(client);
            }

            while (keyToggleAll.consumeClick()) {
                handleToggleAll(client);
            }

            HighlightRenderer.clientTick(client);
        });

        registerCommands();
    }

    private void handleToggleTarget(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            Block block = state.getBlock();
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);

            if (id != null) {
                boolean active = HighlightManager.getInstance().toggleBlock(block);
                Component msg = Component.literal("[Highlight Block] ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal(block.getName().getString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(active ? " [Lila Umrandung Aktiv]" : " [Umrandung Entfernt]")
                                .withStyle(active ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
                client.player.sendSystemMessage(msg);
            }
        } else {
            client.player.sendSystemMessage(Component.literal("[Highlight Block] Schaue einen Block an, um ihn mit Umrandung hervorzuheben!")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private void handleToggleAll(Minecraft client) {
        boolean enabled = HighlightManager.getInstance().toggleEnabled();
        Component msg = Component.literal("[Highlight Block] Umrandungen sind jetzt ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(enabled ? "AKTIVIERT" : "DEAKTIVIERT")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        client.player.sendSystemMessage(msg);
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("highlightblock")
                    .then(ClientCommands.literal("toggle")
                            .executes(context -> {
                                boolean en = HighlightManager.getInstance().toggleEnabled();
                                context.getSource().sendFeedback(Component.literal("[Highlight Block] Aktiv: " + en).withStyle(ChatFormatting.LIGHT_PURPLE));
                                return 1;
                            }))
                    .then(ClientCommands.literal("clear")
                            .executes(context -> {
                                HighlightManager.getInstance().clearBlocks();
                                context.getSource().sendFeedback(Component.literal("[Highlight Block] Alle Block-Umrandungen entfernt.").withStyle(ChatFormatting.LIGHT_PURPLE));
                                return 1;
                            }))
                    .then(ClientCommands.literal("list")
                            .executes(context -> {
                                Set<String> list = HighlightManager.getInstance().getHighlightedBlockIds();
                                if (list.isEmpty()) {
                                    context.getSource().sendFeedback(Component.literal("[Highlight Block] Keine Blöcke ausgewählt.").withStyle(ChatFormatting.GRAY));
                                } else {
                                    context.getSource().sendFeedback(Component.literal("[Highlight Block] Aktive Blöcke:").withStyle(ChatFormatting.LIGHT_PURPLE));
                                    for (String b : list) {
                                        context.getSource().sendFeedback(Component.literal(" - " + b).withStyle(ChatFormatting.WHITE));
                                    }
                                }
                                return 1;
                            }))
            );

            // Shortcut /hb
            dispatcher.register(ClientCommands.literal("hb")
                    .redirect(dispatcher.getRoot().getChild("highlightblock"))
            );
        });
    }
}
