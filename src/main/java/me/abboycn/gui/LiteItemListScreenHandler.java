package me.abboycn.gui;

import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class LiteItemListScreenHandler extends ScreenHandler {
    public static final int MENU_SIZE = 54;
    public static final int FUNCTION_AREA_END = 9;                  // 功能区截至
    public static final int TASK_ITEM_START = FUNCTION_AREA_END;    // 物品展示区起始
    public static final int TASK_ITEM_END = 45;                     // 物品展示区终止

    private final Inventory menuInventory;
    private boolean isFilterUnclaimed = false;
    private ItemListTask task;
    private final ServerPlayerEntity player;
    private Collection<TaskItem> originalTaskList;
    private Collection<TaskItem> upStageTaskItemList;
    private Map<Integer, TaskItem> slotToTaskItemMap;               // 物品区映射
    private Map<Integer, FunctionType> slotToFuncMap;               // 功能区映射

    private Timer guiRefreshTimer;
    private static final int REFRESH_INTERVAL = 3000;

    private enum FunctionType {
        INFO_OVERVIEW,
        REFRESH_LIST,
        FILTER_UNCLAIMED
    }

    public record TaskItem_ItemStack(TaskItem taskItem,ItemStack itemStack) { }

    public LiteItemListScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = new SimpleInventory(MENU_SIZE);
        this.upStageTaskItemList = new ArrayList<>();
        this.slotToTaskItemMap = new HashMap<>();
        this.slotToFuncMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    public LiteItemListScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory, ItemListTask task) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = inventory;
        this.task = task;
        this.upStageTaskItemList = task.getItemList().getTaskItems();
        this.originalTaskList = task.getItemList().getTaskItems();
        this.slotToTaskItemMap = new HashMap<>();
        this.slotToFuncMap = new HashMap<>();
        checkSize(inventory, MENU_SIZE);
        inventory.onOpen(player);
        initMenuSlots();
        startAutoRefresh();
    }

    // 初始化菜单
    private void initMenuSlots() {
        if(slots.isEmpty()){
            menuInventory.clear();
            for (int i = 0; i < MENU_SIZE; i++) {
                int x = 8 + (i % 9) * 18;
                int y = 18 + (i / 9) * 18;
                addSlot(new MenuSlot(menuInventory, i, x, y));
            }
        }
        initFunctionArea();
        initTaskItemArea();
    }

    // 初始化功能区
    private void initFunctionArea() {
        slotToFuncMap = new HashMap<>();

        // [0] 信息总览
        ItemStack infoItem = new ItemStack(Items.BOOK);
        infoItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.BLUE + "任务物品总览"));
        infoItem.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(Formatting.GRAY + "点击查看物品统计信息"),
                Text.literal(Formatting.GRAY + "当前总数: " + upStageTaskItemList.size())
        )));
        menuInventory.setStack(0, infoItem);
        slotToFuncMap.put(0, FunctionType.INFO_OVERVIEW); // 绑定格子0→信息总览

        // [1] 刷新列表
        ItemStack refreshItem = new ItemStack(Items.PAPER);
        refreshItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + "刷新列表"));
        refreshItem.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(Formatting.GRAY + "点击刷新物品列表"),
                Text.literal(Formatting.GRAY + "同步最新认领状态")
        )));
        menuInventory.setStack(1, refreshItem);
        slotToFuncMap.put(1, FunctionType.REFRESH_LIST); // 绑定格子1→刷新列表

        // [2] 筛选
        ItemStack filterItem = new ItemStack(Items.HOPPER);
        filterItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.GREEN + (isFilterUnclaimed?"清除筛选":"筛选未认领")));
        menuInventory.setStack(2, filterItem);
        slotToFuncMap.put(2, FunctionType.FILTER_UNCLAIMED); // 绑定格子2→筛选未认领

        // 剩余留空
        for (int i = 3; i < FUNCTION_AREA_END; i++) {
            menuInventory.setStack(i, ItemStack.EMPTY);
        }
    }

    // 初始化物品展示区
    private void initTaskItemArea() {
        slotToTaskItemMap = new HashMap<>();
        Collection<TaskItem_ItemStack> finished = new ArrayList<>();
        Collection<TaskItem_ItemStack> unfinished = new ArrayList<>();

        for (TaskItem taskItem : upStageTaskItemList) {
            if (isFilterUnclaimed&&!taskItem.getPrincipals().isEmpty()) continue;

            ItemStack displayStack = new ItemStack(taskItem.getItem());
            String namePrefix = (taskItem.isImpt() ? Formatting.RED + "[重要] " : "") + (taskItem.isHard() ? Formatting.YELLOW + "[困难] " : "");
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(namePrefix + Formatting.WHITE + taskItem.getItem().getName().getString()));

            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal(Formatting.GRAY + "总量: " + (taskItem.isFinished()?Formatting.GREEN:Formatting.RED) + taskItem.getAvailable() + " / " + taskItem.getAmount()));
            lore.add(Text.literal(Formatting.GRAY + "备注: " + (taskItem.isHard() ? "困难 " : "") + (taskItem.isImpt() ? "重要 " : "")));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getMsg()));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getItem().toString()));
            lore.add(Text.empty());

            String playerName = player.getName().getString();
            if (taskItem.getPrincipals().isEmpty()) {
                lore.add(Text.literal(Formatting.GOLD + "认领: " + Formatting.GRAY + "未认领"));
                lore.add(Text.literal(Formatting.GRAY + "单击认领物品"));
            }
            else if (taskItem.getPrincipals().contains(playerName)) {
                lore.add(Text.literal(Formatting.GOLD + "认领: " + Formatting.YELLOW + String.join(",", taskItem.getPrincipals())));
                lore.add(Text.literal(Formatting.GRAY + "单击认领物品"));
            }
            else {
                lore.add(Text.literal(Formatting.GOLD + "认领: " + Formatting.GREEN + "你"));
                lore.add(Text.literal(Formatting.GRAY + "单击取消认领"));
            }
            displayStack.set(DataComponentTypes.LORE,new LoreComponent(lore));

            if(taskItem.isFinished()){
                finished.add(new TaskItem_ItemStack(taskItem,displayStack));
            }
            else {
                unfinished.add(new TaskItem_ItemStack(taskItem,displayStack));
            }
        }

        int slot = FUNCTION_AREA_END;
        for(TaskItem_ItemStack taskItem_itemStack : unfinished) {
            if(slot<MENU_SIZE){
                slotToTaskItemMap.put(slot,taskItem_itemStack.taskItem);
                menuInventory.setStack(slot,taskItem_itemStack.itemStack);
                slot++;
            }
        }
        for(TaskItem_ItemStack taskItem_itemStack : finished) {
            if(slot<MENU_SIZE){
                slotToTaskItemMap.put(slot,taskItem_itemStack.taskItem);
                menuInventory.setStack(slot,taskItem_itemStack.itemStack);
                slot++;
            }
        }

        while (slot < TASK_ITEM_END) {
            menuInventory.setStack(slot++, ItemStack.EMPTY);
        }
    }

    private void updateTaskItemList(){
        this.originalTaskList=task.getItemList().getTaskItems();
        this.upStageTaskItemList=this.isFilterUnclaimed?this.originalTaskList.stream().filter(t -> t.getPrincipals().isEmpty()).toList():this.originalTaskList;
    }

    private void refreshGui(){
        initMenuSlots();
        sendContentUpdates();
        updateToClient();
        player.playerScreenHandler.updateToClient();
    }

    private void startAutoRefresh() {
        if (guiRefreshTimer != null) return;
        guiRefreshTimer = new Timer(true); // 守护线程，不卡关服
        guiRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (player.isDisconnected()) {
                    cancel();
                    guiRefreshTimer.cancel();
                    return;
                }
                player.server.execute(() -> {
                    updateTaskItemList();
                    refreshGui();
                });
            }
        }, 0, REFRESH_INTERVAL); // 0延迟启动，每3000ms=3秒
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

        // 功能区
        if (slotIndex < FUNCTION_AREA_END) {
            FunctionType funcType = slotToFuncMap.get(slotIndex); // 从映射获取功能类型
            if (funcType != null) {
                handleMultiFunctionClick(serverPlayer, funcType); // 处理对应功能
            }
            return;
        }
        // 物品展示区
        if (slotIndex < TASK_ITEM_END) {
            TaskItem targetItem = slotToTaskItemMap.get(slotIndex);
            if (targetItem != null) {
                handleTaskItemClick(serverPlayer, targetItem, actionType);
                initMenuSlots();
                sendContentUpdates();
            }
        }

        refreshGui();
    }

    // 处理功能区点击
    private void handleMultiFunctionClick(ServerPlayerEntity player, FunctionType funcType) {
        switch (funcType) {
            case INFO_OVERVIEW: // 信息总览
                sendInfoOverview(player);
                break;
            case REFRESH_LIST: // 刷新列表
                refreshTaskItemList(player);
                break;
            case FILTER_UNCLAIMED: // 筛选
                filterUnclaimedItems(player);
                break;
        }
    }

    // 信息总览
    private void sendInfoOverview(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(Text.literal(Formatting.BLUE + "统计信息: "+task.getName()));
        player.sendMessage(Text.literal(Formatting.GRAY + "> 总物品数:" + Formatting.WHITE + upStageTaskItemList.size()));
        player.sendMessage(Text.literal(Formatting.GRAY + "> 困难物品数:" + Formatting.WHITE + upStageTaskItemList.stream().filter(TaskItem::isHard).count()));
        player.sendMessage(Text.literal(Formatting.GRAY + "> 重要物品数:" + Formatting.WHITE + upStageTaskItemList.stream().filter(TaskItem::isImpt).count()));
        player.sendMessage(Text.literal(Formatting.GRAY + "> 未认领物品数:" + Formatting.WHITE + upStageTaskItemList.stream().filter(t -> t.getPrincipals().isEmpty()).count()));
    }

    // 刷新列表
    private void refreshTaskItemList(ServerPlayerEntity player) {
        updateTaskItemList();
        refreshGui();
        player.sendMessage(Text.literal(Formatting.GREEN + "列表已刷新！"), true);
    }

    // 切换筛选
    private void filterUnclaimedItems(ServerPlayerEntity player) {
        if (!isFilterUnclaimed) {
            isFilterUnclaimed = true;
            upStageTaskItemList = originalTaskList.stream()
                    .filter(t -> t.getPrincipals().isEmpty())
                    .toList();
        }
        else {
            isFilterUnclaimed = false;
            upStageTaskItemList = originalTaskList;
        }
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    // 处理物品展示区点击
    private void handleTaskItemClick(ServerPlayerEntity player, TaskItem targetItem, SlotActionType actionType) {
        if (actionType == SlotActionType.PICKUP) {
            String playerName = player.getName().getString();
            if (targetItem.getPrincipals().contains(playerName)) {
                targetItem.getPrincipals().remove(playerName);
                return;
            }
            targetItem.getPrincipals().add(playerName);
        }

        if (actionType == SlotActionType.QUICK_MOVE) {
            player.sendMessage(targetItem.getItemInfo());
        }
    }

    // 禁用其他点击
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.menuInventory.canPlayerUse(player);
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory, int left, int top) {}

    // 关闭菜单时刷新玩家物品栏
    @Override
    public void onClosed(PlayerEntity player) {
        menuInventory.onClose(player);
        updateToClient();
        player.playerScreenHandler.updateToClient();
        if (guiRefreshTimer != null) {
            guiRefreshTimer.cancel();
            guiRefreshTimer = null;
        }
    }

    // 打开菜单
    public static void openTaskItemMenu(ServerPlayerEntity player, ItemListTask task) {
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("任务: "+task.getName());
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new LiteItemListScreenHandler(syncId, playerInv, new SimpleInventory(MENU_SIZE), task);
            }
        });
    }
}