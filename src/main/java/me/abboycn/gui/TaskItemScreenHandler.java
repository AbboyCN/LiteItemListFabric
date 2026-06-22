package me.abboycn.gui;

import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.abboycn.gui.TaskItemListScreenHandler.openTaskItemListMenu;

public class TaskItemScreenHandler extends LiteItemListMenu {
    private static final ScreenHandlerType<GenericContainerScreenHandler> MENU_TYPE = ScreenHandlerType.GENERIC_9X1;

    private Map<Integer, TaskItemScreenHandler.FunctionType> slotToFuncMap;               // 功能区映射
    private final ItemListTask task;
    private final TaskItem item;

    private enum FunctionType {
        BACK,
        INFO_OVERVIEW,
        REFRESH_LIST,
        SWITCH_HARD,
        SWITCH_IMPT
    }

    public TaskItemScreenHandler(int syncId, ServerPlayerEntity player, ItemListTask task, TaskItem item) {
        super(syncId, MENU_TYPE, 3000, player);
        this.task = task;
        this.item = item;
        this.slotToFuncMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    // 初始化功能区
    @Override
    protected void initMenuInventory() {
        slotToFuncMap = new HashMap<>();

        // [0] 单击返回
        MenuFunctionItem backItem = new MenuFunctionItem(Items.SPECTRAL_ARROW, LangProvider.get("gui.liteitemlist.universal.back"), new ArrayList<>());
        menuInventory.setStack(0, backItem.getItemStack());
        slotToFuncMap.put(0, TaskItemScreenHandler.FunctionType.BACK);

        // [2] 刷新物品
        MenuFunctionItem refreshItem = new MenuFunctionItem(Items.PAPER, LangProvider.get("gui.liteitemlist.universal.refresh"), new ArrayList<>());
        menuInventory.setStack(2, refreshItem.getItemStack());
        slotToFuncMap.put(2, TaskItemScreenHandler.FunctionType.REFRESH_LIST);

        // [4] 信息
        ItemStack infoItem = new ItemStack(item.getItem());
        infoItem.set(DataComponentTypes.LORE, Objects.requireNonNull(infoItem.get(DataComponentTypes.LORE)).with(LangProvider.get("gui.liteitemlist.taskitem.func.info.hint")));
        menuInventory.setStack(4, infoItem);
        slotToFuncMap.put(4, TaskItemScreenHandler.FunctionType.INFO_OVERVIEW);

        // [6] 切换重要
        MenuFunctionItem switchImptItem = new MenuFunctionItem(item.isImpt()? Items.YELLOW_BANNER : Items.GRAY_BANNER, LangProvider.get("gui.liteitemlist.taskitem.func.impt"), List.of(
                LangProvider.get(item.isImpt()?"gui.liteitemlist.universal.t":"gui.liteitemlist.universal.f")
        ));
        menuInventory.setStack(6, switchImptItem.getItemStack());
        slotToFuncMap.put(6, FunctionType.SWITCH_IMPT);

        // [7] 切换困难
        MenuFunctionItem switchHardItem = new MenuFunctionItem(item.isHard()? Items.RED_BANNER : Items.GRAY_BANNER, LangProvider.get("gui.liteitemlist.taskitem.func.hard"), List.of(
                LangProvider.get(item.isHard()?"gui.liteitemlist.universal.t":"gui.liteitemlist.universal.f")
        ));
        menuInventory.setStack(7, switchHardItem.getItemStack());
        slotToFuncMap.put(7, FunctionType.SWITCH_HARD);


        // 剩余留空
        for (int i = 0; i < MENU_SIZE; i++) {
            if(!slotToFuncMap.containsKey(i)){
                menuInventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    // 点击事件处理
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || slotIndex < 0 || slotIndex >= MENU_SIZE) {
            return;
        }
        ItemStack clickedStack = getSlot(slotIndex).getStack();
        if (clickedStack.isEmpty()) {
            return;
        }

        TaskItemScreenHandler.FunctionType funcType = slotToFuncMap.get(slotIndex); // 从映射获取功能类型
        if (funcType != null) {
            handleMultiFunctionClick(serverPlayer, funcType); // 处理对应功能
        }
        refreshGui();
    }

    // 处理功能区点击
    private void handleMultiFunctionClick(ServerPlayerEntity player, TaskItemScreenHandler.FunctionType funcType) {
        switch (funcType) {
            case BACK -> backToSuperMenu(player);               // 返回上级菜单
            case REFRESH_LIST -> refreshTaskItem(player);       // 刷新列表
            case INFO_OVERVIEW -> sendInfoOverview(player);     // 信息总览
            case SWITCH_HARD -> switchHard(player);             // 切换困难
            case SWITCH_IMPT -> switchImpt(player);             // 切换重要
        }
    }

    // 返回上一级
    private void backToSuperMenu(ServerPlayerEntity player) {
        openTaskItemListMenu(player, task);
    }

    // 刷新列表
    private void refreshTaskItem(ServerPlayerEntity player) {
        refreshGui();
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.refresh"), true);
    }

    // 信息总览
    private void sendInfoOverview(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(item.getItemInfo().stream().reduce(Text.literal(""), (s1, s2) -> s1.copy().append(Text.literal("\n")).append(s2)));
    }

    // 切换困难
    private void switchHard(ServerPlayerEntity player) {
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.switch"), true);
        item.setHard(!item.isHard());
        refreshGui();
    }

    // 切换重要
    private void switchImpt(ServerPlayerEntity player) {
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.switch"), true);
        item.setImpt(!item.isImpt());
        refreshGui();
    }

    @Override
    protected void executeAutoRefresh() {
        refreshGui();
    }

    // 打开菜单
    public static void openTaskItemMenu(ServerPlayerEntity player, ItemListTask task, TaskItem item) {
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return LangProvider.get("gui.liteitemlist.taskitem.title",item.getItem().getName().getString(),task.getName());
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskItemScreenHandler(syncId, (ServerPlayerEntity) player, task, item);
            }
        });
    }
}
