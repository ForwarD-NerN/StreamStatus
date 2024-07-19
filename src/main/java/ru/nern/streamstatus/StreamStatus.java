package ru.nern.streamstatus;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

import static net.minecraft.server.command.CommandManager.literal;

public class StreamStatus implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("streamstatus");
	public static ServerBossBar STREAM_BOSSBAR;
	public static ConfigurationManager.Config config = new ConfigurationManager.Config();
	private static boolean luckPermsLoaded = false;

	@Override
	public void onInitialize() {
		ConfigurationManager.onInit();
		if(FabricLoader.getInstance().isModLoaded("luckperms")) luckPermsLoaded = true;
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(literal("stream").requires(Permissions.require("streamstatus.main", 0))
				.executes(ctx -> startOrStop(ctx.getSource()))
						.then(literal("reload").executes(ctx -> reloadConfig(ctx.getSource())).requires(Permissions.require("streamstatus.reload", 2)))));
		initBossbar();

		// Если игрок заходит на сервер и он стримит, добавляем ему боссбар
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if(handler.player instanceof IServerPlayerAccessor && ((IServerPlayerAccessor)handler.player).streamstatus$isStreaming())
				STREAM_BOSSBAR.addPlayer(handler.player);
		});
	}

	public static void addStreamerPrefix(ServerPlayerEntity player){
		if(luckPermsLoaded) LuckPermsIntegration.addStreamerPrefix(player);
	}

	public static void removeStreamerPrefix(ServerPlayerEntity player){
		if(luckPermsLoaded) LuckPermsIntegration.removeStreamerPrefix(player);
	}

	private static void initBossbar(){
		Set<ServerPlayerEntity> players = null;
		if(STREAM_BOSSBAR != null) players = (Set<ServerPlayerEntity>) STREAM_BOSSBAR.getPlayers();

		ServerBossBar newBossbar = new ServerBossBar(Text.literal(config.bossbarName), BossBar.Color.byName(config.bossbarColor), BossBar.Style.NOTCHED_12);
		newBossbar.setPercent(1);

		if(players != null){
			Iterator<ServerPlayerEntity> iterator = players.iterator();
			while (iterator.hasNext()){
				ServerPlayerEntity player = iterator.next();
				STREAM_BOSSBAR.removePlayer(player);
				newBossbar.addPlayer(player);
			}
		}
		STREAM_BOSSBAR = newBossbar;
	}

	private static int reloadConfig(ServerCommandSource source) {
		ConfigurationManager.onInit();
		initBossbar();
		if(luckPermsLoaded) LuckPermsIntegration.rebuildPrefix();

		source.sendFeedback(() -> Text.literal("Конфиг ").formatted(Formatting.GREEN)
				.append(Text.literal("был перезагружен").formatted(Formatting.YELLOW)), false);

		return 1;
	}

	public static int startOrStop(ServerCommandSource source) throws CommandSyntaxException {

		ServerPlayerEntity player = source.getPlayerOrThrow();
		IServerPlayerAccessor accessor = (IServerPlayerAccessor) player;

		// Если игрок стримит, останавливаем стрим
		if(accessor.streamstatus$isStreaming()){
			accessor.streamstatus$setStreaming(false);
			STREAM_BOSSBAR.removePlayer(player); // Убираем боссбар
			removeStreamerPrefix(player); // Убираем префикс

			// Если в конфиге указано проигрывание звука при остановке стрима то проигрываем звук для всех игроков в измеренеии
			if(config.notifySound){
				for(ServerPlayerEntity pl : player.getServerWorld().getPlayers())
					pl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1, 0);
			}

			// Отправляем всем игрокам сообщение об окончании стрима
			source.getServer().getPlayerManager().broadcast(Text.literal(config.streamEndAlert.replace("%f", source.getName())), false);
		}else{
			//Начинаем стрим
			accessor.streamstatus$setStreaming(true);
			addStreamerPrefix(player);
			STREAM_BOSSBAR.addPlayer(player);

			if(config.notifySound){
				for(ServerPlayerEntity pl : player.getServerWorld().getPlayers())
					pl.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1, 0);
			}

			source.getServer().getPlayerManager().broadcast(Text.literal(config.streamStartAlert.replace("%f", source.getName())), false);
		}
		return 1;
	}
}
