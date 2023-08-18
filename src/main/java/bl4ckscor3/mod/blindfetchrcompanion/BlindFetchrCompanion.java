package bl4ckscor3.mod.blindfetchrcompanion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.scores.PlayerTeam;

public class BlindFetchrCompanion implements ModInitializer {
	public static final String MODID = "blindfetchrcompanion";
	public static final ExtendedScreenHandlerType<ItemChecklistMenu> CHECKLIST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MODID, "checklist"), new ExtendedScreenHandlerType<>((id, inv, buf) -> new ItemChecklistMenu(id, readItemStates(buf))));
	public static final ResourceLocation OPEN_MENU_MESSAGE = new ResourceLocation(BlindFetchrCompanion.MODID, "open_menu");
	private static final List<ItemStack> FETCHR_ITEMS = new ArrayList<>();
	private static ChecklistsSavedData itemChecklists;

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(OPEN_MENU_MESSAGE, (server, player, handler, buf, responseSender) -> {
			if (!player.hasContainerOpen()) {
				player.openMenu(new ExtendedScreenHandlerFactory() {
					@Override
					public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
						writeItemStates(player.getScoreboard().getPlayersTeam(player.getName().getString()), buf);
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
						return new ItemChecklistMenu(id, itemChecklists.getOrDefault(player.getScoreboard().getPlayersTeam(player.getName().getString())));
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
		itemChecklists = server.overworld().getDataStorage().computeIfAbsent(tag -> ChecklistsSavedData.load(server, tag), ChecklistsSavedData::new, MODID);

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

	public static void writeItemStates(PlayerTeam team, FriendlyByteBuf buf) {
		List<ItemState> itemStates = itemChecklists.getOrDefault(team);

		buf.writeVarInt(itemStates.size());
		itemStates.forEach(state -> state.write(buf));
	}

	public static List<ItemState> readItemStates(FriendlyByteBuf buf) {
		List<ItemState> itemStates = new ArrayList<>();
		int size = buf.readVarInt();

		for (int i = 0; i < size; i++) {
			itemStates.add(ItemState.read(buf));
		}

		return itemStates;
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
		ItemStack leatherBoots = new ItemStack(Items.LEATHER_BOOTS);
		ItemStack tippedArrow = new ItemStack(Items.TIPPED_ARROW);

		ArmorTrim.setTrim(registryAccess, leatherBoots, new ArmorTrim(registryAccess.lookup(Registries.TRIM_MATERIAL).get().get(TrimMaterials.LAPIS).get(), registryAccess.lookup(Registries.TRIM_PATTERN).get().get(TrimPatterns.COAST).get()));
		PotionUtils.setPotion(tippedArrow, Potions.SLOWNESS);
		FETCHR_ITEMS.addAll(Arrays.asList(
		//@formatter:off
				new ItemStack(Items.ACACIA_HANGING_SIGN),
				new ItemStack(Items.ACACIA_SAPLING),
				new ItemStack(Items.ACTIVATOR_RAIL),
				new ItemStack(Items.AMETHYST_BLOCK),
				new ItemStack(Items.AMETHYST_SHARD),
				new ItemStack(Items.APPLE),
				new ItemStack(Items.ARROW),
				new ItemStack(Items.AXOLOTL_BUCKET),
				new ItemStack(Items.BAMBOO),
				new ItemStack(Items.BIRCH_SAPLING),
				new ItemStack(Items.BLAST_FURNACE),
				new ItemStack(Items.BONE),
				new ItemStack(Items.BONE_BLOCK),
				new ItemStack(Items.BOOK),
				new ItemStack(Items.BOOKSHELF),
				new ItemStack(Items.BRICK),
				new ItemStack(Items.BRUSH),
				new ItemStack(Items.CAKE),
				new ItemStack(Items.CALCITE),
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
				new ItemStack(Items.DIAMOND_AXE),
				new ItemStack(Items.DIAMOND_HOE),
				new ItemStack(Items.DIAMOND_PICKAXE),
				new ItemStack(Items.DIAMOND_SHOVEL),
				new ItemStack(Items.DIAMOND_SWORD),
				new ItemStack(Items.DISPENSER),
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
				new ItemStack(Items.GOLDEN_HOE),
				new ItemStack(Items.GOLDEN_PICKAXE),
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
				new ItemStack(Items.LAPIS_BLOCK),
				new ItemStack(Items.LAPIS_LAZULI),
				new ItemStack(Items.LEAD),
				leatherBoots,
				new ItemStack(Items.LECTERN),
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
				tippedArrow,
				new ItemStack(Items.TNT),
				new ItemStack(Items.TNT_MINECART),
				new ItemStack(Items.TROPICAL_FISH),
				new ItemStack(Items.TROPICAL_FISH_BUCKET),
				new ItemStack(Items.VINE),
				new ItemStack(Items.WRITABLE_BOOK)));
		//@formatter:on
		Collections.sort(FETCHR_ITEMS, (stack1, stack2) -> stack1.getDisplayName().getString().compareTo(stack2.getDisplayName().getString()));
	}
}
