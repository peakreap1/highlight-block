package community.wintercraft.highlightblock.gui;

import community.wintercraft.highlightblock.HighlightManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class HighlightConfigScreen extends Screen {
    private final Screen parent;

    public HighlightConfigScreen(Screen parent) {
        super(Component.literal("Highlight Block - Einstellungen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        HighlightManager manager = HighlightManager.getInstance();

        int buttonWidth = 155;
        int buttonHeight = 20;
        int leftX = this.width / 2 - buttonWidth - 5;
        int rightX = this.width / 2 + 5;

        int startY = 48;
        int rowSpacing = 26;

        // Row 0: On/Off & Mode
        addRenderableWidget(
                CycleButton.onOffBuilder(manager.isEnabled())
                        .create(
                                leftX, startY, buttonWidth, buttonHeight,
                                Component.literal("Leucht-Umrandungen"),
                                (button, value) -> manager.setEnabled(value)
                        )
        );

        addRenderableWidget(
                CycleButton.builder(
                                (HighlightManager.AnimationMode mode) -> switch (mode) {
                                    case NEON_PURPLE -> Component.literal("Neon Lila (Puls)");
                                    case RAINBOW -> Component.literal("Rainbow Wave");
                                    case ELECTRIC_CYAN -> Component.literal("Cyberpunk Cyan");
                                    case AMBER_GOLD -> Component.literal("Golden Ore");
                                    case STATIC -> Component.literal("Statisch");
                                },
                                manager.getAnimationMode()
                        )
                        .withValues(HighlightManager.AnimationMode.values())
                        .create(
                                rightX, startY, buttonWidth, buttonHeight,
                                Component.literal("Modus"),
                                (button, value) -> manager.setAnimationMode(value)
                        )
        );

        // Row 1: Render Distance & Animation Speed
        List<Integer> distances = List.of(32, 48, 64, 96, 128, 160, 200);
        addRenderableWidget(
                CycleButton.builder(
                                (Integer dist) -> Component.literal(dist + " Blöcke"),
                                closestDistance(manager.getRenderDistance(), distances)
                        )
                        .withValues(distances)
                        .create(
                                leftX, startY + rowSpacing, buttonWidth, buttonHeight,
                                Component.literal("Reichweite"),
                                (button, value) -> manager.setRenderDistance(value)
                        )
        );

        List<Float> speeds = List.of(0.5f, 1.0f, 2.0f);
        addRenderableWidget(
                CycleButton.builder(
                                (Float spd) -> spd == 0.5f ? Component.literal("Langsam (0.5x)") : (spd == 2.0f ? Component.literal("Schnell (2.0x)") : Component.literal("Normal (1.0x)")),
                                manager.getAnimationSpeed()
                        )
                        .withValues(speeds)
                        .create(
                                rightX, startY + rowSpacing, buttonWidth, buttonHeight,
                                Component.literal("Animations-Tempo"),
                                (button, value) -> manager.setAnimationSpeed(value)
                        )
        );

        // Row 2: Halo Bloom & Outline Width
        addRenderableWidget(
                CycleButton.onOffBuilder(manager.isHaloEnabled())
                        .create(
                                leftX, startY + rowSpacing * 2, buttonWidth, buttonHeight,
                                Component.literal("Leucht-Aura (Halo)"),
                                (button, value) -> manager.setHaloEnabled(value)
                        )
        );

        List<Float> widths = List.of(1.5f, 2.2f, 3.2f, 4.5f);
        addRenderableWidget(
                CycleButton.builder(
                                (Float w) -> Component.literal(w + " px"),
                                closestWidth(manager.getOutlineWidth(), widths)
                        )
                        .withValues(widths)
                        .create(
                                rightX, startY + rowSpacing * 2, buttonWidth, buttonHeight,
                                Component.literal("Linienstärke"),
                                (button, value) -> manager.setOutlineWidth(value)
                        )
        );

        // Row 3: Andesite Quick-Add & Clear Blocks
        Button[] clearHolder = new Button[1];
        Button addAndesiteBtn = Button.builder(
                        Component.literal("+ Andesit hinzufügen"),
                        btn -> {
                            manager.addBlock(Blocks.ANDESITE);
                            if (clearHolder[0] != null) {
                                clearHolder[0].setMessage(Component.literal("Auswahl leeren (" + manager.getHighlightedBlockIds().size() + ")"));
                            }
                        }
                )
                .bounds(leftX, startY + rowSpacing * 3, buttonWidth, buttonHeight)
                .build();
        addRenderableWidget(addAndesiteBtn);

        clearHolder[0] = Button.builder(
                        Component.literal("Auswahl leeren (" + manager.getHighlightedBlockIds().size() + ")"),
                        btn -> {
                            manager.clearBlocks();
                            btn.setMessage(Component.literal("Auswahl geleert (0)"));
                        }
                )
                .bounds(rightX, startY + rowSpacing * 3, buttonWidth, buttonHeight)
                .build();
        addRenderableWidget(clearHolder[0]);

        // Bottom: Done Button
        addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                        .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                        .build()
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(extractor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        extractor.centeredText(this.font, this.title, this.width / 2, 20, 0xFFE080FF);
        extractor.centeredText(
                this.font,
                Component.literal("Tipp: Drücke 'H' im Spiel, um den anvisierten Block auszuwählen / zu entfernen"),
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

    private static int closestDistance(int current, List<Integer> list) {
        int best = list.get(0);
        int diff = Math.abs(current - best);
        for (int v : list) {
            int d = Math.abs(current - v);
            if (d < diff) {
                diff = d;
                best = v;
            }
        }
        return best;
    }

    private static float closestWidth(float current, List<Float> list) {
        float best = list.get(0);
        float diff = Math.abs(current - best);
        for (float v : list) {
            float d = Math.abs(current - v);
            if (d < diff) {
                diff = d;
                best = v;
            }
        }
        return best;
    }
}
