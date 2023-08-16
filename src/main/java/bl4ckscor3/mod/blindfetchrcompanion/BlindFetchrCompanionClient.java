package bl4ckscor3.mod.blindfetchrcompanion;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;

public class BlindFetchrCompanionClient implements ClientModInitializer {
	private static final int COOLDOWN_LENGTH = 60;
	public static int cooldown = COOLDOWN_LENGTH;
	public static KeyMapping openItemChecklistKey;

	@Override
	public void onInitializeClient() {
		openItemChecklistKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(String.format("key.%s.open_item_checklist", BlindFetchrCompanion.MODID), GLFW.GLFW_KEY_C, KeyMapping.CATEGORY_MISC));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (cooldown > 0)
				cooldown--;
			else if (client.screen == null && openItemChecklistKey.consumeClick()) {
				ClientPlayNetworking.send(BlindFetchrCompanion.OPEN_MENU_MESSAGE, PacketByteBufs.create());
				cooldown = COOLDOWN_LENGTH;
			}
		});
		MenuScreens.register(BlindFetchrCompanion.CHECKLIST_MENU_TYPE, ItemChecklistScreen::new);
	}
}
