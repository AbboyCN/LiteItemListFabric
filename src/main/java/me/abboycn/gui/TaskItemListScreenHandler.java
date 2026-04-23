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
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

import static me.abboycn.gui.TaskItemScreenHandler.openTaskItemMenu;

public class TaskItemListScreenHandler extends ScreenHandler {
    public static final int MENU_SIZE = 54;
    public static final int FUNCTION_AREA_END = 9;                  // 功能区截至
    public static final int TASK_ITEM_START = FUNCTION_AREA_END;    // 物品展示区起始
    public static final int TASK_ITEM_END = 54;                     // 物品展示区终止
    public static final int TASK_ITEM_AREA_SIZE = TASK_ITEM_END-TASK_ITEM_START;

    private final Inventory menuInventory;
    private MenuListStatus.FilterType_Clime filterTypeClime = MenuListStatus.FilterType_Clime.DEFAULT;
    private MenuListStatus.FilterType_Finished filterTypeFinished = MenuListStatus.FilterType_Finished.DEFAULT;
    private MenuListStatus.FilterType_Mark filterTypeMark = MenuListStatus.FilterType_Mark.DEFAULT;
    private int currentPage = 0;
    private ItemListTask task;
    private final ServerPlayerEntity player;
    private Collection<TaskItem> originalTaskList;
    private Collection<TaskItem> upStageTaskItemList;
    private Map<Integer, TaskItem> slotToTaskItemMap;               // 物品区映射
    private Map<Integer, FunctionType> slotToFuncMap;               // 功能区映射

    private Timer guiRefreshTimer;
    private static final int REFRESH_INTERVAL = 3000;

    private enum FunctionType {
        PAST_PAGE,
        INFO_OVERVIEW,
        REFRESH_LIST,
        FILTER_UNCLAIMED,
        FILTER_FINISHED,
        FILTER_MARK,
        NEXT_PAGE
    }

    public record TaskItem_ItemStack(TaskItem taskItem,ItemStack itemStack) { }

    public TaskItemListScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = new SimpleInventory(MENU_SIZE);
        this.upStageTaskItemList = new ArrayList<>();
        this.slotToTaskItemMap = new HashMap<>();
        this.slotToFuncMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    public TaskItemListScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory, ItemListTask task, MenuListStatus status) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = inventory;
        this.task = task;
        this.upStageTaskItemList = task.getItemList().getTaskItems();
        this.originalTaskList = task.getItemList().getTaskItems();
        this.slotToTaskItemMap = new HashMap<>();
        this.slotToFuncMap = new HashMap<>();
        this.filterTypeClime = status.filterTypeClime;
        this.filterTypeFinished = status.filterTypeFinished;
        this.filterTypeMark = status.filterTypeMark;
        this.currentPage = status.page;
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

        // [0] 上一页
        ItemStack pastPageItem = new ItemStack(Items.ARROW);
        pastPageItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.GOLD + "<< 上一页"));
        menuInventory.setStack(0, pastPageItem);
        slotToFuncMap.put(0, FunctionType.PAST_PAGE);

        // [1] 信息总览
        ItemStack infoItem = new ItemStack(Items.BOOK);
        infoItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + "任务物品总览"));
        infoItem.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(Formatting.GRAY + "点击查看物品统计信息"),
                Text.literal(Formatting.GRAY + "当前总数: " + upStageTaskItemList.size())
        )));
        menuInventory.setStack(1, infoItem);
        slotToFuncMap.put(1, FunctionType.INFO_OVERVIEW); // 绑定格子0→信息总览

        // [2] 刷新列表
        ItemStack refreshItem = new ItemStack(Items.PAPER);
        refreshItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + "刷新列表"));
        refreshItem.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(Formatting.GRAY + "点击刷新物品列表"),
                Text.literal(Formatting.GRAY + "将立即同步最新物品物品状态并刷新假人库存")
        )));
        menuInventory.setStack(2, refreshItem);
        slotToFuncMap.put(2, FunctionType.REFRESH_LIST);

        // [5] 筛选:认领状态
        ItemStack filterItem_Clime = new ItemStack(Items.HOPPER);
        filterItem_Clime.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + ("筛选认领状态")));
        filterItem_Clime.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(filterTypeClime== MenuListStatus.FilterType_Clime.DEFAULT?Formatting.WHITE+"-> 全部":Formatting.GRAY+"    全部"),
                Text.literal(filterTypeClime== MenuListStatus.FilterType_Clime.CLIMED?Formatting.WHITE+"-> 我认领的":Formatting.GRAY+"    我认领的"),
                Text.literal(filterTypeClime== MenuListStatus.FilterType_Clime.UNCLIMED?Formatting.WHITE+"-> 未被认领":Formatting.GRAY+"    未被认领")
        )));
        menuInventory.setStack(5, filterItem_Clime);
        slotToFuncMap.put(5, FunctionType.FILTER_UNCLAIMED);

        // [6] 筛选:完成情况
        ItemStack filterItem_Finished = new ItemStack(Items.HOPPER);
        filterItem_Finished.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + ("筛选完成状态")));
        filterItem_Finished.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(filterTypeFinished== MenuListStatus.FilterType_Finished.DEFAULT?Formatting.WHITE+"-> 全部":Formatting.GRAY+"    全部"),
                Text.literal(filterTypeFinished== MenuListStatus.FilterType_Finished.UNFINISHED?Formatting.WHITE+"-> 未开始/进行中":Formatting.GRAY+"    未开始/进行中"),
                Text.literal(filterTypeFinished== MenuListStatus.FilterType_Finished.PROCESSING?Formatting.WHITE+"-> 进行中":Formatting.GRAY+"    进行中"),
                Text.literal(filterTypeFinished== MenuListStatus.FilterType_Finished.NOTSTART?Formatting.WHITE+"-> 未开始":Formatting.GRAY+"    未开始"),
                Text.literal(filterTypeFinished== MenuListStatus.FilterType_Finished.FINISHED?Formatting.WHITE+"-> 已完成":Formatting.GRAY+"    已完成")
        )));
        menuInventory.setStack(6, filterItem_Finished);
        slotToFuncMap.put(6, FunctionType.FILTER_FINISHED);

        // [7] 筛选:物品属性
        ItemStack filterItem_Mark = new ItemStack(Items.HOPPER);
        filterItem_Mark.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + ("筛选物品标记")));
        filterItem_Mark.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(filterTypeMark== MenuListStatus.FilterType_Mark.DEFAULT?Formatting.WHITE+"-> 全部":Formatting.GRAY+"    全部"),
                Text.literal(filterTypeMark== MenuListStatus.FilterType_Mark.IMPTORHARD ?Formatting.WHITE+"-> 重要/困难":Formatting.GRAY+"    重要/困难"),
                Text.literal(filterTypeMark== MenuListStatus.FilterType_Mark.IMPT?Formatting.WHITE+"-> 重要":Formatting.GRAY+"    重要"),
                Text.literal(filterTypeMark== MenuListStatus.FilterType_Mark.HARD?Formatting.WHITE+"-> 困难":Formatting.GRAY+"    困难")
        )));
        menuInventory.setStack(7, filterItem_Mark);
        slotToFuncMap.put(7, FunctionType.FILTER_MARK);

        // [8] 下一页
        ItemStack nextPageItem = new ItemStack(Items.ARROW);
        nextPageItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.GOLD + "下一页 >>"));
        menuInventory.setStack(8, nextPageItem);
        slotToFuncMap.put(8, FunctionType.NEXT_PAGE);

        // 剩余留空
        for (int i = 0; i < FUNCTION_AREA_END; i++) {
            if(menuInventory.getStack(i) == null){
                menuInventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    // 初始化物品展示区
    private void initTaskItemArea() {
        slotToTaskItemMap = new HashMap<>();
        Collection<TaskItem_ItemStack> finished = new ArrayList<>();
        Collection<TaskItem_ItemStack> unfinished = new ArrayList<>();

        if(currentPage*45>upStageTaskItemList.size()){
            currentPage=upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE;
        }
        for (int i = currentPage*45;i<Integer.min((currentPage+1)*45, upStageTaskItemList.size());i++) {
            TaskItem taskItem = upStageTaskItemList.stream().toList().get(i);
            if (filterTypeClime== MenuListStatus.FilterType_Clime.CLIMED&&!taskItem.getPrincipals().contains(player.getName().getString())) continue;
            if (filterTypeClime== MenuListStatus.FilterType_Clime.UNCLIMED&&!taskItem.getPrincipals().isEmpty()) continue;

            ItemStack displayStack = new ItemStack(taskItem.getItem());
            String namePrefix = (taskItem.isImpt() ? Formatting.YELLOW + "[重要] " : "") + (taskItem.isHard() ? Formatting.RED + "[困难] " : "");
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(namePrefix + Formatting.WHITE + taskItem.getItem().getName().getString()));

            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal(Formatting.GRAY + "总量: " + (taskItem.isFinished()?Formatting.GREEN:Formatting.RED) + taskItem.getAvailable() + " / " + taskItem.getAmount()));
            if(!taskItem.isFinished()){
                int box = (taskItem.getAmount()-taskItem.getAvailable())/1728;
                int stack = (taskItem.getAmount()-taskItem.getAvailable()-1728*box)/64;
                int single = (taskItem.getAmount()-taskItem.getAvailable())%64;
                lore.add(Text.literal(Formatting.GRAY + "还需: " + (box!=0?box+"盒 ":"") + (stack!=0?stack+"组 ":"") + (single!=0?single+"个":"") + " (" + taskItem.getAmount() + "个)"));
            }
            lore.add(Text.literal(Formatting.GRAY + "备注: " + (taskItem.isHard() ? "困难 " : "") + (taskItem.isImpt() ? "重要 " : "")));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getMsg()));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getItem().toString()));
            lore.add(Text.empty());

            if (taskItem.getPrincipals().isEmpty()) {
                lore.add(Text.literal(Formatting.GOLD + "认领: " + Formatting.GRAY + "未认领"));
                lore.add(Text.literal(Formatting.GRAY + "单击认领物品"));
            }
            else {
                lore.add(Text.literal(Formatting.GOLD + "认领: " + Formatting.YELLOW + String.join(",", taskItem.getPrincipals())));
                lore.add(Text.literal(Formatting.GRAY + "单击认领物品"));
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
        this.originalTaskList=task.getItemList().getTaskItems().stream().sorted(Comparator.comparingInt(TaskItem::getAmount).reversed()).toList();
        this.upStageTaskItemList=getFilteredListFromOriginal();
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

    private Collection<TaskItem> getFilteredListFromOriginal(){
        Collection<TaskItem> ret = switch (filterTypeClime) {
            case DEFAULT -> originalTaskList;
            case CLIMED -> originalTaskList.stream().filter(t -> t.getPrincipals().contains(player.getName().getString())).toList();
            case UNCLIMED -> originalTaskList.stream().filter(t -> t.getPrincipals().isEmpty()).toList();
        };
        ret = switch (filterTypeFinished) {
            case DEFAULT -> ret;
            case UNFINISHED -> ret.stream().filter(t -> !t.isFinished()).toList();
            case PROCESSING -> ret.stream().filter(t -> (!t.isFinished())&&t.getAvailable()!=0).toList();
            case NOTSTART -> ret.stream().filter(t -> t.getAvailable()==0).toList();
            case FINISHED -> ret.stream().filter(TaskItem::isFinished).toList();
        };
        ret = switch (filterTypeMark) {
            case DEFAULT -> ret;
            case IMPTORHARD -> ret.stream().filter(t -> t.isHard()&&t.isImpt()).toList();
            case IMPT -> ret.stream().filter(TaskItem::isImpt).toList();
            case HARD -> ret.stream().filter(TaskItem::isHard).toList();
        };
        return ret;
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
        TaskItem targetItem = slotToTaskItemMap.get(slotIndex);
        if (targetItem != null) {
            handleTaskItemClick(serverPlayer, targetItem, actionType);
            initMenuSlots();
            sendContentUpdates();
        }

        refreshGui();
    }

    // 处理功能区点击
    private void handleMultiFunctionClick(ServerPlayerEntity player, FunctionType funcType) {
        switch (funcType) {
            case PAST_PAGE:
                toPastPage(player);
                break;
            case INFO_OVERVIEW: // 信息总览
                sendInfoOverview(player);
                break;
            case REFRESH_LIST: // 刷新列表
                refreshTaskItemList(player);
                break;
            case FILTER_UNCLAIMED: // 筛选认领状态
                filterUnclaimedItems(player);
                break;
            case FILTER_FINISHED: // 筛选完成状态
                filterFinishedItems(player);
                break;
            case FILTER_MARK: // 筛选物品标记
                filterMarkItems(player);
                break;
            case NEXT_PAGE:
                toNextPage(player);
                break;
        }
    }

    // 上一页
    private void toPastPage(ServerPlayerEntity player) {
        if(currentPage==0){
            player.sendMessage(Text.literal(Formatting.RED+"已经是第一页了!"),true);
            refreshGui();
            return;
        }
        currentPage--;
        refreshGui();
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
        filterTypeClime = switch(filterTypeClime){
            case DEFAULT -> MenuListStatus.FilterType_Clime.CLIMED;
            case CLIMED -> MenuListStatus.FilterType_Clime.UNCLIMED;
            case UNCLIMED -> MenuListStatus.FilterType_Clime.DEFAULT;
        };
        task.getMember(player).getListFilter().filterTypeClime=filterTypeClime;
        upStageTaskItemList=getFilteredListFromOriginal();
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    private void filterFinishedItems(ServerPlayerEntity player) {
        filterTypeFinished = switch (filterTypeFinished){
            case DEFAULT -> MenuListStatus.FilterType_Finished.UNFINISHED;
            case UNFINISHED -> MenuListStatus.FilterType_Finished.PROCESSING;
            case PROCESSING -> MenuListStatus.FilterType_Finished.NOTSTART;
            case NOTSTART -> MenuListStatus.FilterType_Finished.FINISHED;
            case FINISHED -> MenuListStatus.FilterType_Finished.DEFAULT;
        };
        task.getMember(player).getListFilter().filterTypeFinished=filterTypeFinished;
        upStageTaskItemList=getFilteredListFromOriginal();
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    private void filterMarkItems(ServerPlayerEntity player) {
        filterTypeMark = switch (filterTypeMark){
            case DEFAULT -> MenuListStatus.FilterType_Mark.IMPTORHARD;
            case IMPTORHARD -> MenuListStatus.FilterType_Mark.IMPT;
            case IMPT -> MenuListStatus.FilterType_Mark.HARD;
            case HARD -> MenuListStatus.FilterType_Mark.DEFAULT;
        };
        task.getMember(player).getListFilter().filterTypeMark=filterTypeMark;
        upStageTaskItemList=getFilteredListFromOriginal();
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    // 下一页
    private void toNextPage(ServerPlayerEntity player) {
        if((currentPage+1)*TASK_ITEM_AREA_SIZE >= upStageTaskItemList.size()) {
            player.sendMessage(Text.literal(Formatting.RED+"已经是最后一页了!"),true);
            refreshGui();
            return;
        }
        currentPage++;
        refreshGui();
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
            player.closeHandledScreen();
            openTaskItemMenu(player,task,targetItem);
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
    public static void openTaskItemListMenu(ServerPlayerEntity player, ItemListTask task) {
        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("任务: "+task.getName()+" ("+task.getItemList().getTaskItemCount()+")");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskItemListScreenHandler(syncId, playerInv, new SimpleInventory(MENU_SIZE), task, task.getMember((ServerPlayerEntity) player).getListFilter());
            }
        });
    }
}