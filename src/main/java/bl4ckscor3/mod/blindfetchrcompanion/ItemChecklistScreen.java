package bl4ckscor3.mod.blindfetchrcompanion;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import bl4ckscor3.mod.blindfetchrcompanion.BlindFetchrCompanionConfig.CheckedItemDisplayType;
import bl4ckscor3.mod.blindfetchrcompanion.mixin.KeyMappingAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ItemChecklistScreen extends AbstractContainerScreen<ItemChecklistMenu> {
	public static final ResourceLocation TEXTURE = new ResourceLocation(BlindFetchrCompanion.MODID, "gui/container/item_checklist.png");
	public static final ResourceLocation BEACON_GUI = new ResourceLocation("textures/gui/container/beacon.png");

	public ItemChecklistScreen(ItemChecklistMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = 248;
		imageHeight = 239;
		inventoryLabelY = -100;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		renderTransparentBackground(guiGraphics);
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
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

				if (displayType.showsCheckmark()) {
					RenderSystem.disableDepthTest();
					guiGraphics.blit(BEACON_GUI, x - 2, y - 3, 88, 219, 21, 22, 256, 256);
					RenderSystem.enableDepthTest();
				}
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
