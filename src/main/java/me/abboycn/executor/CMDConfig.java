package me.abboycn.executor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.abboycn.LiteItemListFabric;
import me.abboycn.config.ConfigKey;
import me.abboycn.config.ConfigManager;
import me.abboycn.config.LiteItemListConfig;
import me.abboycn.resource.LangProvider;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CMDConfig {
    private static LiteItemListConfig cfg() {
        return ConfigManager.INSTANCE.getConfig();
    }

    public static int printAllConfig(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity p = ctx.getSource().getPlayer();
        if (p == null) return 0;
        LiteItemListConfig config = cfg();
        p.sendMessage(Text.literal("==== LiteItemList 全局配置 ===="));
        p.sendMessage(Text.literal(ConfigKey.LANGUAGE + "：" + config.getLanguage()));
        p.sendMessage(Text.literal(ConfigKey.BOT_NAME_SUFFIX + "：\"" + config.getBotNameSuffix() + "\""));
        return 1;
    }

    public static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        try{
            ConfigManager.INSTANCE.reloadConfig();
        } catch (Exception e){
            LiteItemListFabric.LOGGER.error(LangProvider.get("msg.liteitemlist.cmd.config.reload.failed").getString(),e);
            return 0;
        }
        if(player == null) {
            LiteItemListFabric.LOGGER.info(LangProvider.get("msg.liteitemlist.cmd.config.reload.success").getString());
            return 1;
        }
        player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.config.reload.success"));
        return 1;
    }

    public static int resetAllConfig(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        cfg().resetDefault();
        ConfigManager.INSTANCE.saveConfig();
        LangProvider.setLang(cfg().getLanguage());
        if(player == null) {
            LiteItemListFabric.LOGGER.info(LangProvider.get("msg.liteitemlist.cmd.config.reset.success").getString());
        }
        else {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.config.reset.success"));
        }
        return 1;
    }

    public static int setLang(CommandContext<ServerCommandSource> ctx, LangProvider.Lang lang) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        cfg().setLanguage(lang);
        ConfigManager.INSTANCE.saveConfig();
        LangProvider.setLang(lang);
        if(player == null) {
            LiteItemListFabric.LOGGER.info(LangProvider.get("msg.liteitemlist.cmd.config.set.success", ConfigKey.LANGUAGE, lang.name()).getString());
        }
        else {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.config.set.success", ConfigKey.LANGUAGE, lang.name()));
        }
        return 1;
    }

    public static int setBotSuffix(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        String suffix = StringArgumentType.getString(ctx, "suffix");
        cfg().setBotNameSuffix(suffix);
        ConfigManager.INSTANCE.saveConfig();
        if(player == null) {
            LiteItemListFabric.LOGGER.info(LangProvider.get("msg.liteitemlist.cmd.config.set.success", ConfigKey.BOT_NAME_SUFFIX, suffix).getString());
        }
        else {
            player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.config.set.success", ConfigKey.BOT_NAME_SUFFIX, suffix));
        }
        return 1;
    }
}