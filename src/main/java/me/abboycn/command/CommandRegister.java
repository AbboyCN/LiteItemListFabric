package me.abboycn.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.abboycn.executor.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static me.abboycn.executor.CMDTaskNew.CMDTaskNewExecutor;

public class CommandRegister {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((d, r, e) -> registerMain(d,r));
        CommandRegistrationCallback.EVENT.register((d, r, e) -> registerConfig(d,r));
    }

    private static void registerMain(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess){
        dispatcher.register(CommandManager.literal("liteitemlist")
                .then(CommandManager.literal("task")
                        .then(CommandManager.literal("new")
                                .then(CommandManager.argument("project", StringArgumentType.string())
                                        .executes(context -> CMDTaskNewExecutor(context,false))
                                        .then(CommandManager.argument("file", StringArgumentType.string())
                                                .suggests((context, builder)->TABLitematicaSuggester.litematicaFileSuggester(builder))
                                                .executes(context -> CMDTaskNewExecutor(context,true)))))
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument("project", StringArgumentType.string())
                                        .suggests((context, builder)->TABTaskSuggester.projectSuggester(builder))
                                        .executes(CMDTaskCancel::CMDTaskCancelExecutor)))
                        .then(CommandManager.literal("list")
                                .executes(CMDTaskList::CMDTaskListExecutor))
                        .then(CommandManager.literal("join")
                                .then(CommandManager.argument("project", StringArgumentType.string())
                                        .suggests((context, builder)->TABTaskSuggester.projectSuggester(builder))
                                        .executes(CMDTaskJoin::CMDTaskJoinExecutor)))
                        .then(CommandManager.literal("leave")
                                .then(CommandManager.argument("project", StringArgumentType.string())
                                        .suggests((context, builder)->TABTaskSuggester.projectSuggester(builder))
                                        .executes(CMDTaskLeave::CMDTaskLeaveExecutor)))
                        .then(CommandManager.literal("switch")
                                .then(CommandManager.argument("project", StringArgumentType.string())
                                        .suggests((context, builder)->TABTaskSuggester.projectSuggester(builder))
                                        .executes(CMDTaskSwitch::CMDTaskSwitchExecutor))))
                .then(CommandManager.literal("gui")
                        .executes(CMDGui::CMDGuiExecutor))
                .then(CommandManager.literal("bot")
                        .then(CommandManager.argument("project", StringArgumentType.string())
                                .suggests((context, builder)->TABTaskSuggester.projectSuggester(builder))
                                .then(CommandManager.literal("spawn")
                                        .executes((c -> CMDBot.CMDBotSpawnExecutor(c,true)))
                                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                                                .suggests(TABBotSuggester::botSuggester)
                                                .executes(c -> CMDBot.CMDBotSpawnExecutor(c,false))))
                                .then(CommandManager.literal("new")
                                        .executes(CMDBot::CMDBotNewExecutor))
                                .then(CommandManager.literal("list")
                                        .executes(CMDBot::CMDBotListExecutor))
                                .then(CommandManager.literal("despawn")
                                        .executes((c -> CMDBot.CMDBotDespawnExecutor(c,true)))
                                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                                                .suggests(TABBotSuggester::botSuggester)
                                                .executes(c -> CMDBot.CMDBotDespawnExecutor(c,false))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                                                .suggests(TABBotSuggester::botSuggester)
                                                .executes(CMDBot::CMDBotRemoveExecutor)))))
                .then(CommandManager.literal("chat")
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(CMDChat::CMDChatExecutor)))
                .then(CommandManager.literal("reload")
                        .executes(CMDReload::CMDReloadExecutor)
                        .then(CommandManager.literal("-force")
                                .executes(CMDReload::CMDForceReloadExecutor)))
                .then(CommandManager.literal("config")
                        .requires(s->s.hasPermissionLevel(3))
                        .then(CommandManager.literal("language")
                                .then(CommandManager.literal("zh-cn")
                                        .executes(s->CMDConfigLanguage.zh_cn()))
                                .then(CommandManager.literal("en-us")
                                        .executes(s->CMDConfigLanguage.en_us()))))
        );
    }

    private static void registerConfig(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("liteitemlist")
                .then(CommandManager.literal("config")
                        .requires(s -> s.hasPermissionLevel(3))
                        .executes(CMDConfig::printAllConfig)
                        .then(CommandManager.literal("reload")
                                .executes(CMDConfig::reloadConfig))
                        .then(CommandManager.literal("reset")
                                .executes(CMDConfig::resetAllConfig))
                        .then(CommandManager.literal("language")
                                .then(CommandManager.literal("zh-cn")
                                        .executes(c -> CMDConfig.setLang(c, me.abboycn.resource.LangProvider.Lang.zh_cn)))
                                .then(CommandManager.literal("en-us")
                                        .executes(c -> CMDConfig.setLang(c, me.abboycn.resource.LangProvider.Lang.en_us)))
                        )
                        .then(CommandManager.literal("botsuffix")
                                .then(CommandManager.argument("suffix", StringArgumentType.greedyString())
                                        .executes(CMDConfig::setBotSuffix))
                        )));
    }
}
