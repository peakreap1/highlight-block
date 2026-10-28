package peakreap1.highlightblock.gui;

import peakreap1.highlightblock.HighlightManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Set;

public class HighlightConfigScreen extends Screen {
    private final Screen parent;

    public HighlightConfigScreen(Screen parent) {
        super(Component.literal("Highlight Block - Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        HighlightManager manager = HighlightManager.getInstance();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;

        // Button 1: Clear Selection
        int selectedCount = manager.getHighlightedBlockIds().size();
        addRenderableWidget(
                Button.builder(
                                Component.literal("Clear Selection (" + selectedCount + " active)"),
                                btn -> {
                                    manager.clearBlocks(this.minecraft);
                                    btn.setMessage(Component.literal("Selection Cleared (0 active)"));
                                }
                        )
                        .bounds(centerX, 45, buttonWidth, buttonHeight)
                        .build()
        );

        // Button 2: Done Button
        addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                        .bounds(centerX, this.height - 30, buttonWidth, buttonHeight)
                        .build()
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFD700);

        HighlightManager manager = HighlightManager.getInstance();
        Set<String> blocks = manager.getHighlightedBlockIds();

        int y = 78;
        extractor.centeredText(this.font, Component.literal("Active Blocks with Spotlight Texture (!):"), this.width / 2, y, 0xFFFFFFFF);
        y += 16;

        if (blocks.isEmpty()) {
            extractor.centeredText(this.font, Component.literal("(No blocks selected)"), this.width / 2, y, 0xFF888888);
        } else {
            for (String id : blocks) {
                if (y > this.height - 65) {
                    extractor.centeredText(this.font, Component.literal("... and more"), this.width / 2, y, 0xFFAAAAAA);
                    break;
                }
                extractor.centeredText(this.font, Component.literal("• " + id), this.width / 2, y, 0xFFFFEE55);
                y += 13;
            }
        }

        extractor.centeredText(
                this.font,
                Component.literal("Tip: Aim at any block and press 'H' to spotlight or reset it!"),
                this.width / 2,
                this.height - 48,
                0xFFAAAAAA
        );
    }

    @Override
    public void onClose() {
        HighlightManager.getInstance().save();
        if (this.minecraft != null && this.minecraft.gui != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }
}
