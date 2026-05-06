package me.abboycn.gui;

import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.*;
import java.util.List;

import static me.abboycn.gui.TaskBotManagerScreenHandler.openTaskBotManagerMenu;
import static me.abboycn.gui.TaskItemScreenHandler.openTaskItemMenu;
import static me.abboycn.gui.TaskManagerScreenHandler.openTaskManagerMenu;

public class TaskItemListScreenHandler extends LiteItemListMenu {
    public static final ScreenHandlerType<GenericContainerScreenHandler> MENU_TYPE = ScreenHandlerType.GENERIC_9X6;

    public static final int FUNCTION_AREA_END = 9;                  // 功能区截至
    public static final int TASK_ITEM_START = FUNCTION_AREA_END;    // 物品展示区起始
    public static final int TASK_ITEM_END = 54;                     // 物品展示区终止
    public static final int TASK_ITEM_AREA_SIZE = TASK_ITEM_END-TASK_ITEM_START;

    private MenuListStatus.FilterType_Clime filterTypeClime;
    private MenuListStatus.FilterType_Finished filterTypeFinished;
    private MenuListStatus.FilterType_Mark filterTypeMark;
    private int currentPage;
    private final ItemListTask task;
    private Collection<TaskItem> originalTaskList;
    private Collection<TaskItem> upStageTaskItemList;
    private Map<Integer, FunctionType> slotToFuncMap;               // 功能区映射
    private Map<Integer, TaskItem> slotToTaskItemMap;               // 物品区映射

    private enum FunctionType {
        PAST_PAGE,
        BACK,
        INFO_OVERVIEW,
        REFRESH_LIST,
        MANAGE_BOT,
        FILTER_UNCLAIMED,
        FILTER_FINISHED,
        FILTER_MARK,
        NEXT_PAGE
    }

    public record TaskItem_ItemStack(TaskItem taskItem,ItemStack itemStack) { }

    public TaskItemListScreenHandler(int syncId, ServerPlayerEntity player, ItemListTask task, MenuListStatus status) {
        super(syncId, MENU_TYPE, 3000, player);
        this.filterTypeClime = status.filterTypeClime;
        this.filterTypeFinished = status.filterTypeFinished;
        this.filterTypeMark = status.filterTypeMark;
        this.task = task;
        this.slotToFuncMap = new HashMap<>();
        this.slotToTaskItemMap = new HashMap<>();
        this.currentPage = status.page;
        this.originalTaskList = task.getItemList().getTaskItems();
        this.upStageTaskItemList = getFilteredListFromOriginal();
        initMenuSlots();
        startAutoRefresh();
    }

    @Override
    protected void initMenuInventory() {
        initFunctionArea();
        initTaskItemArea();
    }

    // 初始化功能区
    private void initFunctionArea() {
        slotToFuncMap = new HashMap<>();

        // [0] 上一页
        MenuFunctionItem pastPageItem = new MenuFunctionItem(Items.ARROW, Text.literal(Formatting.GOLD + "<< 上一页"), List.of(
                Text.literal(Formatting.GRAY + "当前第 " + (currentPage+1) + " / " + (upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE+1) + " 页")
        ));
        menuInventory.setStack(0, pastPageItem.getItemStack());
        slotToFuncMap.put(0, TaskItemListScreenHandler.FunctionType.PAST_PAGE);

        // [1] 单击返回
        MenuFunctionItem backItem = new MenuFunctionItem(Items.SPECTRAL_ARROW, Text.literal(Formatting.GOLD + "<= 返回任务管理面板"), new ArrayList<>());
        menuInventory.setStack(1, backItem.getItemStack());
        slotToFuncMap.put(1, TaskItemListScreenHandler.FunctionType.BACK);

        // [2] 刷新列表
        MenuFunctionItem refreshItem = new MenuFunctionItem(Items.PAPER, Text.literal(Formatting.YELLOW + "刷新列表"), List.of(
                Text.literal(Formatting.GRAY + "点击刷新物品列表"),
                Text.literal(Formatting.GRAY + "将立即同步最新物品物品状态并刷新假人库存")
        ));
        menuInventory.setStack(2, refreshItem.getItemStack());
        slotToFuncMap.put(2, TaskItemListScreenHandler.FunctionType.REFRESH_LIST);

        // [3] 存储假人管理
        MenuFunctionItem botManagerItem = new MenuFunctionItem(Items.PLAYER_HEAD, Text.literal(Formatting.YELLOW + "管理存储假人"), new ArrayList<>());
        menuInventory.setStack(3, botManagerItem.getItemStack());
        slotToFuncMap.put(3, TaskItemListScreenHandler.FunctionType.MANAGE_BOT);

        // [4] 信息总览
        MenuFunctionItem infoItem = new MenuFunctionItem(Items.BOOK, Text.literal(Formatting.AQUA + task.getName()), List.of(
                Text.literal(Formatting.GRAY + "点击查看物品统计信息"),
                Text.literal(Formatting.GRAY + "物品项数: " + upStageTaskItemList.size())
        ));
        menuInventory.setStack(4, infoItem.getItemStack());
        slotToFuncMap.put(4, TaskItemListScreenHandler.FunctionType.INFO_OVERVIEW); // 绑定格子0→信息总览

        // [5] 筛选:认领状态
        MenuFunctionItem filterItem_Clime = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + ("筛选认领状态")), List.of(
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.CLIMED ? Formatting.WHITE + "-> 我认领的" : Formatting.GRAY + "    我认领的"),
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.UNCLIMED ? Formatting.WHITE + "-> 未被认领" : Formatting.GRAY + "    未被认领")
        ));
        menuInventory.setStack(5, filterItem_Clime.getItemStack());
        slotToFuncMap.put(5, TaskItemListScreenHandler.FunctionType.FILTER_UNCLAIMED);

        // [6] 筛选:完成情况
        MenuFunctionItem filterItem_Finished = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + ("筛选完成状态")), List.of(
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.UNFINISHED ? Formatting.WHITE + "-> 未开始/进行中" : Formatting.GRAY + "    未开始/进行中"),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.PROCESSING ? Formatting.WHITE + "-> 进行中" : Formatting.GRAY + "    进行中"),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.NOTSTART ? Formatting.WHITE + "-> 未开始" : Formatting.GRAY + "    未开始"),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.FINISHED ? Formatting.WHITE + "-> 已完成" : Formatting.GRAY + "    已完成")
        ));
        menuInventory.setStack(6, filterItem_Finished.getItemStack());
        slotToFuncMap.put(6, TaskItemListScreenHandler.FunctionType.FILTER_FINISHED);

        // [7] 筛选:物品属性
        MenuFunctionItem filterItem_Mark = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + ("筛选物品标记")), List.of(
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.IMPTORHARD ? Formatting.WHITE + "-> 重要/困难" : Formatting.GRAY + "    重要/困难"),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.IMPT ? Formatting.WHITE + "-> 重要" : Formatting.GRAY + "    重要"),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.HARD ? Formatting.WHITE + "-> 困难" : Formatting.GRAY + "    困难")
        ));
        menuInventory.setStack(7, filterItem_Mark.getItemStack());
        slotToFuncMap.put(7, TaskItemListScreenHandler.FunctionType.FILTER_MARK);

        // [8] 下一页
        MenuFunctionItem nextPageItem = new MenuFunctionItem(Items.ARROW, Text.literal(Formatting.GOLD + "下一页 >>"), List.of(
                Text.literal(Formatting.GRAY + "当前第 " + (currentPage+1) + " / " + (upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE+1) + " 页")
        ));
        menuInventory.setStack(8, nextPageItem.getItemStack());
        slotToFuncMap.put(8, TaskItemListScreenHandler.FunctionType.NEXT_PAGE);

        // 剩余留空
        for (int i = 0; i < FUNCTION_AREA_END; i++) {
            if (menuInventory.getStack(i) == null) {
                menuInventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    // 初始化物品展示区
    private void initTaskItemArea() {
        slotToTaskItemMap = new HashMap<>();
        Collection<TaskItem_ItemStack> finished = new ArrayList<>();
        Collection<TaskItem_ItemStack> unfinished = new ArrayList<>();

        if(currentPage*TASK_ITEM_AREA_SIZE>upStageTaskItemList.size()){
            currentPage=upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE;
        }
        for (int i = currentPage*TASK_ITEM_AREA_SIZE;i<Integer.min((currentPage+1)*TASK_ITEM_AREA_SIZE, upStageTaskItemList.size());i++) {
            TaskItem taskItem = upStageTaskItemList.stream().toList().get(i);

            ItemStack displayStack = new ItemStack(taskItem.getItem());
            String namePrefix = (taskItem.isImpt() ? Formatting.YELLOW + "[重要] " : "") + (taskItem.isHard() ? Formatting.RED + "[困难] " : "");
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(namePrefix + Formatting.WHITE + taskItem.getItem().getName().getString()));

            List<Text> lore = new ArrayList<>();

            lore.add(Text.literal(Formatting.GRAY + "总量: " + (taskItem.isFinished()?Formatting.GREEN:Formatting.RED) + taskItem.getAvailable() + " / " + taskItem.getAmount()));
            if(!taskItem.isFinished()){
                int box = (taskItem.getAmount()-taskItem.getAvailable())/1728;
                int stack = (taskItem.getAmount()-taskItem.getAvailable()-1728*box)/64;
                int single = (taskItem.getAmount()-taskItem.getAvailable())%64;
                lore.add(Text.literal(Formatting.GRAY + "还需: " + (box!=0?box+"盒 ":"") + (stack!=0?stack+"组 ":"") + (single!=0?single+"个":"") + " (" + (taskItem.getAmount()-taskItem.getAvailable()) + "个)"));
            }

            lore.add(Text.literal(Formatting.GRAY + "备注: " + (taskItem.isHard() ? "困难 " : "") + (taskItem.isImpt() ? "重要 " : "")));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getMsg()));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getItem().toString()));
            lore.add(Text.empty());

            lore.add(Text.literal(Formatting.GOLD + "认领: " + (taskItem.getPrincipals().isEmpty()?(Formatting.GRAY + "未认领"):(Formatting.YELLOW + String.join(",", taskItem.getPrincipals())))));
            lore.add(Text.literal(Formatting.AQUA + "[单击] " + Formatting.GRAY + (taskItem.getPrincipals().contains(player.getName().getString())?"取消认领物品":"认领物品")));
            lore.add(Text.literal(Formatting.AQUA + "[Shift+单击] " + Formatting.GRAY + "打开物品属性"));

            displayStack.set(DataComponentTypes.LORE,new LoreComponent(lore));

            if(taskItem.isFinished()){
                finished.add(new TaskItem_ItemStack(taskItem,displayStack));
            }
            else {
                unfinished.add(new TaskItem_ItemStack(taskItem,displayStack));
            }
        }

        int slot = TASK_ITEM_START;
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

    @Override
    protected void executeAutoRefresh() {
        updateTaskItemList();
        refreshGui();
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
            case IMPTORHARD -> ret.stream().filter(t -> t.isHard()||t.isImpt()).toList();
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
            case PAST_PAGE -> toPastPage(player);                   // 上一页
            case BACK -> backToSuperMenu(player);                   // 返回至上级菜单
            case INFO_OVERVIEW -> sendInfoOverview(player);         // 信息总览
            case REFRESH_LIST -> refreshTaskItemList(player);       // 刷新列表
            case MANAGE_BOT -> manageStorageBot(player);            // 管理存储假人
            case FILTER_UNCLAIMED -> filterUnclaimedItems(player);  // 筛选认领状态
            case FILTER_FINISHED -> filterFinishedItems(player);    // 筛选完成状态
            case FILTER_MARK -> filterMarkItems(player);            // 筛选物品标记
            case NEXT_PAGE -> toNextPage(player);                   // 下一页
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
        task.getMember(player).getListStatus().page = currentPage;
        refreshGui();
    }

    // 返回到上一级
    private void backToSuperMenu(ServerPlayerEntity player) {
        player.closeHandledScreen();
        openTaskManagerMenu(player);
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
        executeAutoRefresh();
        player.sendMessage(Text.literal(Formatting.GREEN + "列表已刷新！"), true);
    }

    // 管理存储假人
    private void manageStorageBot(ServerPlayerEntity player) {
        player.closeHandledScreen();
        openTaskBotManagerMenu(player, task);
    }

    // 切换筛选
    private void filterUnclaimedItems(ServerPlayerEntity player) {
        filterTypeClime = switch(filterTypeClime){
            case DEFAULT -> MenuListStatus.FilterType_Clime.CLIMED;
            case CLIMED -> MenuListStatus.FilterType_Clime.UNCLIMED;
            case UNCLIMED -> MenuListStatus.FilterType_Clime.DEFAULT;
        };
        task.getMember(player).getListStatus().filterTypeClime=filterTypeClime;
        executeAutoRefresh();
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
        task.getMember(player).getListStatus().filterTypeFinished=filterTypeFinished;
        executeAutoRefresh();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    private void filterMarkItems(ServerPlayerEntity player) {
        filterTypeMark = switch (filterTypeMark){
            case DEFAULT -> MenuListStatus.FilterType_Mark.IMPTORHARD;
            case IMPTORHARD -> MenuListStatus.FilterType_Mark.IMPT;
            case IMPT -> MenuListStatus.FilterType_Mark.HARD;
            case HARD -> MenuListStatus.FilterType_Mark.DEFAULT;
        };
        task.getMember(player).getListStatus().filterTypeMark=filterTypeMark;
        executeAutoRefresh();
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
        task.getMember(player).getListStatus().page = currentPage;
        refreshGui();
    }

    // 处理物品展示区点击
    private void handleTaskItemClick(ServerPlayerEntity player, TaskItem targetItem, SlotActionType actionType) {
        if (actionType == SlotActionType.PICKUP) {
            String playerName = player.getName().getString();
            if (targetItem.getPrincipals().contains(playerName)) {
                targetItem.getPrincipals().remove(playerName);
            }
            else{
                targetItem.getPrincipals().add(playerName);
            }
        }
        else if (actionType == SlotActionType.QUICK_MOVE) {
            player.closeHandledScreen();
            openTaskItemMenu(player,task,targetItem);
        }
        refreshGui();
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
                return new TaskItemListScreenHandler(syncId, (ServerPlayerEntity) player, task, task.getMember((ServerPlayerEntity) player).getListStatus());
            }
        });
    }
}