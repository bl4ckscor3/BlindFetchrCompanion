package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import bl4ckscor3.mod.blindfetchrcompanion.checklist.ChecklistsSavedData;
import bl4ckscor3.mod.blindfetchrcompanion.checklist.ItemChecklistMenu;
import bl4ckscor3.mod.blindfetchrcompanion.checklist.ItemState;
import bl4ckscor3.mod.blindfetchrcompanion.network.ClientboundUpdateItemStatePacket;
import bl4ckscor3.mod.blindfetchrcompanion.network.ServerboundRequestToOpenMenuPacket;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;

/* @formatter:off
 * TODO:
 * - Remove menu
 * - Change screen to handle everything, but make server open the screen for the player
 * 	- Keep track of players who have a screen open to minimize update packets
 * 	- Send item states to client when opening the screen
 * 	- Send single changes to the server which then sends it off to other players
 * - Server-side config option for how items are displayed (categories, no categories, highlight categories when hovering)
 * @formatter:on
 */
public class BlindFetchrCompanion implements ModInitializer {
	public static final String MODID = "blindfetchrcompanion";
	public static final ExtendedScreenHandlerType<ItemChecklistMenu, List<ItemState>> CHECKLIST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(MODID, "checklist"), new ExtendedScreenHandlerType<>((id, inv, data) -> new ItemChecklistMenu(id, data), ItemState.LIST_STREAM_CODEC));
	public static final Type<ServerboundRequestToOpenMenuPacket> REQUEST_TO_OPEN_MENU_MESSAGE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "request_to_open_menu"));
	public static final Type<ClientboundUpdateItemStatePacket> UPDATE_ITEM_STATE_MESSAGE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "update_item_state"));
	private static final List<ItemStack> FETCHR_ITEMS = new ArrayList<>();
	private static ChecklistsSavedData itemChecklists;
	private static BlindFetchrCompanionConfig config;

	@Override
	public void onInitialize() {
		AutoConfig.register(BlindFetchrCompanionConfig.class, JanksonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(BlindFetchrCompanionConfig.class).getConfig();
		PayloadTypeRegistry.playS2C().register(UPDATE_ITEM_STATE_MESSAGE, ClientboundUpdateItemStatePacket.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(REQUEST_TO_OPEN_MENU_MESSAGE, ServerboundRequestToOpenMenuPacket.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(BlindFetchrCompanion.REQUEST_TO_OPEN_MENU_MESSAGE, (packet, ctx) -> {
			Player player = ctx.player();

			if (!player.hasContainerOpen()) {
				player.openMenu(new ExtendedScreenHandlerFactory<>() {
					@Override
					public List<ItemState> getScreenOpeningData(ServerPlayer player) {
						return getItemStates(player);
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new ItemChecklistMenu(id, getItemStates(player));
					}

					@Override
					public Component getDisplayName() {
						return Component.translatable(String.format("%s.item_checklist", MODID));
					}
				});
			}
		});
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> resetAllChecklists(server));
		ServerLifecycleEvents.SERVER_STARTED.register(BlindFetchrCompanion::loadChecklists);
		ServerLifecycleEvents.SERVER_STOPPING.register(BlindFetchrCompanion::saveChecklists);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			//@formatter:off
			dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal(String.format("%s_reset", MODID))
					.requires(sourceStack -> sourceStack.hasPermission(3))
					.executes(ctx -> {
						resetAllChecklists(ctx.getSource().getServer());
						ctx.getSource().sendSuccess(() -> Component.translatable(String.format("%s.command.success", MODID)), true);
						return 1;
					})
					.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("team", TeamArgument.team())
							.executes(ctx -> {
								//@formatter:on
								PlayerTeam team = TeamArgument.getTeam(ctx, "team");

								resetTeamChecklist(team);
								ctx.getSource().sendSuccess(() -> Component.translatable(String.format("%s.command.team.success", MODID), team.getName()), true);
								return 1;
							})));
		});
	}

	public static void loadChecklists(MinecraftServer server) {
		itemChecklists = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ChecklistsSavedData::new, (tag, lookupProvider) -> ChecklistsSavedData.load(server, tag, lookupProvider), DataFixTypes.LEVEL), MODID);

		if (itemChecklists.isFirstLoad())
			resetAllChecklists(server);
	}

	public static void saveChecklists(MinecraftServer server) {
		server.overworld().getDataStorage().set(MODID, itemChecklists);
		setItemChecklistsDirty();
	}

	public static void setItemChecklistsDirty() {
		itemChecklists.setDirty();
	}

	public static List<ItemState> getItemStates(Player player) {
		return itemChecklists.getOrDefault(player.getScoreboard().getPlayersTeam(player.getName().getString()));
	}

	private static void resetAllChecklists(MinecraftServer server) {
		RegistryAccess registryAccess = server.registryAccess();

		if (FETCHR_ITEMS.isEmpty())
			populateFetchrItems(registryAccess);

		itemChecklists.clear();
		server.getScoreboard().getPlayerTeams().forEach(BlindFetchrCompanion::resetTeamChecklist);
	}

	private static void resetTeamChecklist(PlayerTeam team) {
		List<ItemState> itemStates = new ArrayList<>();

		FETCHR_ITEMS.forEach(stack -> itemStates.add(new ItemState(stack, false)));
		itemChecklists.put(team, itemStates);
	}

	private static void populateFetchrItems(RegistryAccess registryAccess) {
		try {
			ItemStack leatherBoots = new ItemStack(Items.LEATHER_BOOTS);
			ItemStack arrowOfSlowness = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.SLOWNESS);
			ItemStack arrowOfPoison = PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON);

			leatherBoots.set(DataComponents.TRIM, new ArmorTrim(registryAccess.lookup(Registries.TRIM_MATERIAL).get().get(TrimMaterials.LAPIS).get(), registryAccess.lookup(Registries.TRIM_PATTERN).get().get(TrimPatterns.SHAPER).get()));
			FETCHR_ITEMS.addAll(Arrays.asList( //@formatter:off
				new ItemStack(Items.ACACIA_HANGING_SIGN),
				new ItemStack(Items.ACACIA_SAPLING),
				new ItemStack(Items.ACTIVATOR_RAIL),
				new ItemStack(Items.AMETHYST_BLOCK),
				new ItemStack(Items.AMETHYST_SHARD),
				new ItemStack(Items.APPLE),
				new ItemStack(Items.ARMADILLO_SCUTE),
				new ItemStack(Items.ARROW),
				arrowOfSlowness,
				arrowOfPoison,
				new ItemStack(Items.AXOLOTL_BUCKET),
				new ItemStack(Items.BAKED_POTATO),
				new ItemStack(Items.BAMBOO),
				new ItemStack(Items.BIG_DRIPLEAF),
				new ItemStack(Items.BLAST_FURNACE),
				new ItemStack(Items.BONE),
				new ItemStack(Items.BONE_BLOCK),
				new ItemStack(Items.BOOK),
				new ItemStack(Items.BOOKSHELF),
				new ItemStack(Items.BRICK),
				new ItemStack(Items.BROWN_BANNER),
				new ItemStack(Items.BRUSH),
				new ItemStack(Items.CAKE),
				new ItemStack(Items.CALCITE),
				new ItemStack(Items.CARROT),
				new ItemStack(Items.CARROT_ON_A_STICK),
				new ItemStack(Items.CAULDRON),
				new ItemStack(Items.CHERRY_CHEST_BOAT),
				new ItemStack(Items.CHERRY_SAPLING),
				new ItemStack(Items.CHEST_MINECART),
				new ItemStack(Items.CLOCK),
				new ItemStack(Items.COCOA_BEANS),
				new ItemStack(Items.COD),
				new ItemStack(Items.COD_BUCKET),
				new ItemStack(Items.COMPASS),
				new ItemStack(Items.COOKED_RABBIT),
				new ItemStack(Items.COOKIE),
				new ItemStack(Items.COPPER_BLOCK),
				new ItemStack(Items.CRACKED_DEEPSLATE_BRICKS),
				new ItemStack(Items.CROSSBOW),
				new ItemStack(Items.CYAN_DYE),
				new ItemStack(Items.DARK_OAK_SAPLING),
				new ItemStack(Items.DEAD_BUSH),
				new ItemStack(Items.DEEPSLATE),
				new ItemStack(Items.DEEPSLATE_TILE_WALL),
				new ItemStack(Items.DETECTOR_RAIL),
				new ItemStack(Items.DIAMOND_HOE),
				new ItemStack(Items.DIAMOND_PICKAXE),
				new ItemStack(Items.DISPENSER),
				new ItemStack(Items.DRIED_KELP),
				new ItemStack(Items.DRIED_KELP_BLOCK),
				new ItemStack(Items.DRIPSTONE_BLOCK),
				new ItemStack(Items.EGG),
				new ItemStack(Items.EMERALD),
				new ItemStack(Items.ENCHANTED_BOOK),
				new ItemStack(Items.ENDER_PEARL),
				new ItemStack(Items.EXPOSED_CUT_COPPER),
				new ItemStack(Items.FERMENTED_SPIDER_EYE),
				new ItemStack(Items.FERN),
				new ItemStack(Items.FIREWORK_ROCKET),
				new ItemStack(Items.FLETCHING_TABLE),
				new ItemStack(Items.FLINT),
				new ItemStack(Items.FLINT_AND_STEEL),
				new ItemStack(Items.FLOWERING_AZALEA),
				new ItemStack(Items.FLOWER_POT),
				new ItemStack(Items.FURNACE_MINECART),
				new ItemStack(Items.GLASS_BOTTLE),
				new ItemStack(Items.GLISTERING_MELON_SLICE),
				new ItemStack(Items.GLOW_BERRIES),
				new ItemStack(Items.GLOW_INK_SAC),
				new ItemStack(Items.GLOW_ITEM_FRAME),
				new ItemStack(Items.GLOW_LICHEN),
				new ItemStack(Items.GOLDEN_APPLE),
				new ItemStack(Items.GOLDEN_AXE),
				new ItemStack(Items.GOLDEN_CARROT),
				new ItemStack(Items.GOLDEN_SHOVEL),
				new ItemStack(Items.GOLDEN_SWORD),
				new ItemStack(Items.GOLD_BLOCK),
				new ItemStack(Items.GRAY_DYE),
				new ItemStack(Items.GREEN_DYE),
				new ItemStack(Items.GUNPOWDER),
				new ItemStack(Items.HANGING_ROOTS),
				new ItemStack(Items.HAY_BLOCK),
				new ItemStack(Items.HEART_OF_THE_SEA),
				new ItemStack(Items.HOPPER),
				new ItemStack(Items.HOPPER_MINECART),
				new ItemStack(Items.INK_SAC),
				new ItemStack(Items.IRON_BLOCK),
				new ItemStack(Items.ITEM_FRAME),
				new ItemStack(Items.JACK_O_LANTERN),
				new ItemStack(Items.JUKEBOX),
				new ItemStack(Items.JUNGLE_SAPLING),
				new ItemStack(Items.LAPIS_BLOCK),
				new ItemStack(Items.LAPIS_LAZULI),
				new ItemStack(Items.LEAD),
				leatherBoots,
				new ItemStack(Items.LECTERN),
				new ItemStack(Items.LIGHT_BLUE_BUNDLE),
				new ItemStack(Items.LIME_DYE),
				new ItemStack(Items.MAGMA_BLOCK),
				new ItemStack(Items.MANGROVE_PROPAGULE),
				new ItemStack(Items.MAP),
				new ItemStack(Items.MELON),
				new ItemStack(Items.MELON_SLICE),
				new ItemStack(Items.MILK_BUCKET),
				new ItemStack(Items.MOSSY_STONE_BRICKS),
				new ItemStack(Items.MOSS_CARPET),
				new ItemStack(Items.MUDDY_MANGROVE_ROOTS),
				new ItemStack(Items.MUD_BRICKS),
				new ItemStack(Items.MUSHROOM_STEW),
				new ItemStack(Items.OBSIDIAN),
				new ItemStack(Items.ORANGE_CONCRETE),
				new ItemStack(Items.PACKED_MUD),
				new ItemStack(Items.PAINTING),
				new ItemStack(Items.PINK_GLAZED_TERRACOTTA),
				new ItemStack(Items.PINK_PETALS),
				new ItemStack(Items.PISTON),
				new ItemStack(Items.POINTED_DRIPSTONE),
				new ItemStack(Items.POWERED_RAIL),
				new ItemStack(Items.PUMPKIN_PIE),
				new ItemStack(Items.PUMPKIN_SEEDS),
				new ItemStack(Items.PURPLE_DYE),
				new ItemStack(Items.RABBIT_HIDE),
				new ItemStack(Items.RAIL),
				new ItemStack(Items.RAW_COPPER_BLOCK),
				new ItemStack(Items.RAW_GOLD_BLOCK),
				new ItemStack(Items.RAW_IRON_BLOCK),
				new ItemStack(Items.REDSTONE_BLOCK),
				new ItemStack(Items.RED_BED),
				new ItemStack(Items.REPEATER),
				new ItemStack(Items.ROOTED_DIRT),
				new ItemStack(Items.SALMON),
				new ItemStack(Items.SALMON_BUCKET),
				new ItemStack(Items.SCAFFOLDING),
				new ItemStack(Items.SEAGRASS),
				new ItemStack(Items.SEA_PICKLE),
				new ItemStack(Items.SLIME_BALL),
				new ItemStack(Items.SMOKER),
				new ItemStack(Items.SNOW),
				new ItemStack(Items.SPIDER_EYE),
				new ItemStack(Items.SPORE_BLOSSOM),
				new ItemStack(Items.SPRUCE_SAPLING),
				new ItemStack(Items.SPYGLASS),
				new ItemStack(Items.STICKY_PISTON),
				new ItemStack(Items.STONECUTTER),
				new ItemStack(Items.SUSPICIOUS_STEW),
				new ItemStack(Items.SWEET_BERRIES),
				new ItemStack(Items.TARGET),
				new ItemStack(Items.TNT),
				new ItemStack(Items.TNT_MINECART),
				new ItemStack(Items.TUFF),
				new ItemStack(Items.TROPICAL_FISH),
				new ItemStack(Items.TROPICAL_FISH_BUCKET),
				new ItemStack(Items.VINE),
				new ItemStack(Items.WHITE_STAINED_GLASS),
				new ItemStack(Items.WOLF_ARMOR),
				new ItemStack(Items.WRITABLE_BOOK)));
			//@formatter:on
			Collections.sort(FETCHR_ITEMS, (stack1, stack2) -> stack1.getDisplayName().getString().compareTo(stack2.getDisplayName().getString()));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static BlindFetchrCompanionConfig getConfig() {
		return config;
	}
}
