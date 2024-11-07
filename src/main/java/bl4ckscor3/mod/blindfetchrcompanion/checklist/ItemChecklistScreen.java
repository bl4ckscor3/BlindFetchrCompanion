package bl4ckscor3.mod.blindfetchrcompanion.checklist;

import com.mojang.blaze3d.platform.InputConstants;

import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanion;
import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanionClient;
import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanionConfig.CheckedItemDisplayType;
import bl4ckscor3.mod.blindfetchrcompanion.mixin.KeyMappingAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ItemChecklistScreen extends AbstractContainerScreen<ItemChecklistMenu> {
	public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BlindFetchrCompanion.MODID, "gui/container/item_checklist.png");
	public static final ResourceLocation CONFIRM_SPRITE = ResourceLocation.withDefaultNamespace("container/beacon/confirm");

	public ItemChecklistScreen(ItemChecklistMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 248;
		imageHeight = 257;
		inventoryLabelY = -100;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		guiGraphics.blit(RenderType::guiTextured, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 257, 257);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);

		for (Slot slot : menu.slots) {
			if (slot.index < menu.itemStates.size() && menu.itemStates.get(slot.index).isChecked()) {
				int x = leftPos + slot.x;
				int y = topPos + slot.y;
				CheckedItemDisplayType displayType = BlindFetchrCompanion.getConfig().checkedItemDisplayType;

				if (displayType.darkens())
					guiGraphics.fill(x, y, x + 16, y + 16, 0x80000000);

				if (displayType.showsCheckmark())
					guiGraphics.blitSprite(RenderType::guiTexturedOverlay, CONFIRM_SPRITE, x, y - 2, 18, 18);
			}
		}

		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		InputConstants.Key pressedKey = InputConstants.getKey(keyCode, scanCode);

		if (pressedKey != InputConstants.UNKNOWN && pressedKey.equals(((KeyMappingAccessor) BlindFetchrCompanionClient.openItemChecklistKey).getKey())) {
			onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		super.onClose();
		BlindFetchrCompanionClient.cooldown = 0;
	}
}
