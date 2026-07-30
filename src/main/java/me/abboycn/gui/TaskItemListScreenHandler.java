package me.abboycn.gui;

import me.abboycn.resource.LangProvider;
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
        MenuFunctionItem pastPageItem = new MenuFunctionItem(Items.ARROW, LangProvider.get("gui.liteitemlist.universal.last_page"), List.of(
                LangProvider.get("gui.liteitemlist.universal.pageinfo",currentPage+1,upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE+1)
        ));
        menuInventory.setStack(0, pastPageItem.getItemStack());
        slotToFuncMap.put(0, TaskItemListScreenHandler.FunctionType.PAST_PAGE);

        // [1] 单击返回
        MenuFunctionItem backItem = new MenuFunctionItem(Items.SPECTRAL_ARROW, LangProvider.get("gui.liteitemlist.universal.back"), new ArrayList<>());
        menuInventory.setStack(1, backItem.getItemStack());
        slotToFuncMap.put(1, TaskItemListScreenHandler.FunctionType.BACK);

        // [2] 刷新列表
        MenuFunctionItem refreshItem = new MenuFunctionItem(Items.PAPER, LangProvider.get("gui.liteitemlist.universal.refresh"), List.of(
                LangProvider.get("gui.liteitemlist.itemlist.func.refresh.hint")
        ));
        menuInventory.setStack(2, refreshItem.getItemStack());
        slotToFuncMap.put(2, TaskItemListScreenHandler.FunctionType.REFRESH_LIST);

        // [3] 存储假人管理
        MenuFunctionItem botManagerItem = new MenuFunctionItem(Items.PLAYER_HEAD, LangProvider.get("gui.liteitemlist.itemlist.func.botmanager"), new ArrayList<>());
        menuInventory.setStack(3, botManagerItem.getItemStack());
        slotToFuncMap.put(3, TaskItemListScreenHandler.FunctionType.MANAGE_BOT);

        // [4] 信息总览
        MenuFunctionItem infoItem = new MenuFunctionItem(Items.BOOK, Text.literal(Formatting.AQUA + task.getName()), List.of(
                LangProvider.get("gui.liteitemlist.itemlist.func.info.hint"),
                LangProvider.get("gui.liteitemlist.itemlist.func.info.process",task.getItemList().getFinishedCount(),task.getItemList().getTaskItemCount(),
                        task.getItemList().getProgressPercentage())
        ));
        menuInventory.setStack(4, infoItem.getItemStack());
        slotToFuncMap.put(4, TaskItemListScreenHandler.FunctionType.INFO_OVERVIEW);

        // [5] 筛选:认领状态
        MenuFunctionItem filterItem_Clime = new MenuFunctionItem(Items.HOPPER, LangProvider.get("gui.liteitemlist.itemlist.func.filter_claim"), List.of(
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.DEFAULT ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.universal.filter.default")),
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.CLIMED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_claim.claimed")),
                Text.literal(filterTypeClime == MenuListStatus.FilterType_Clime.UNCLIMED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_claim.unclaimed"))
        ));
        menuInventory.setStack(5, filterItem_Clime.getItemStack());
        slotToFuncMap.put(5, TaskItemListScreenHandler.FunctionType.FILTER_UNCLAIMED);

        // [6] 筛选:完成情况
        MenuFunctionItem filterItem_Finished = new MenuFunctionItem(Items.HOPPER, LangProvider.get("gui.liteitemlist.itemlist.func.filter_process"), List.of(
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.DEFAULT ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.universal.filter.default")),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.UNFINISHED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_process.notstart_processing")),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.PROCESSING ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_process.processing")),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.NOTSTART ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_process.notstart")),
                Text.literal(filterTypeFinished == MenuListStatus.FilterType_Finished.FINISHED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_process.finished"))
        ));
        menuInventory.setStack(6, filterItem_Finished.getItemStack());
        slotToFuncMap.put(6, TaskItemListScreenHandler.FunctionType.FILTER_FINISHED);

        // [7] 筛选:物品属性
        MenuFunctionItem filterItem_Mark = new MenuFunctionItem(Items.HOPPER, LangProvider.get("gui.liteitemlist.itemlist.func.filter_marker"), List.of(
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.DEFAULT ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.universal.filter.default")),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.IMPTORHARD ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_marker.impt_hard")),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.IMPT ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_marker.impt")),
                Text.literal(filterTypeMark == MenuListStatus.FilterType_Mark.HARD ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.itemlist.func.filter_marker.hard"))
        ));
        menuInventory.setStack(7, filterItem_Mark.getItemStack());
        slotToFuncMap.put(7, TaskItemListScreenHandler.FunctionType.FILTER_MARK);

        // [8] 下一页
        MenuFunctionItem nextPageItem = new MenuFunctionItem(Items.ARROW, LangProvider.get("gui.liteitemlist.universal.next_page"), List.of(
                LangProvider.get("gui.liteitemlist.universal.pageinfo",currentPage+1,upStageTaskItemList.size()/TASK_ITEM_AREA_SIZE+1)
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
            Text namePrefix = (taskItem.isImpt() ? LangProvider.get("gui.liteitemlist.itemlist.element.name.impt") : Text.literal("")).copy().append(taskItem.isHard() ? LangProvider.get("gui.liteitemlist.itemlist.element.name.hard") : Text.literal(""));
            displayStack.set(DataComponentTypes.CUSTOM_NAME,namePrefix.copy().append(Text.literal(Formatting.WHITE + taskItem.getItem().getName().getString())));

            List<Text> lore = new ArrayList<>();

            lore.add(LangProvider.get("gui.liteitemlist.itemlist.element.count").copy().append(Text.literal((taskItem.isFinished()?Formatting.GREEN:taskItem.getAvailable()==0?Formatting.RED:Formatting.YELLOW) + "" + taskItem.getAvailable() + " / " + taskItem.getAmount())));
            if(!taskItem.isFinished()){
                int box = (taskItem.getAmount()-taskItem.getAvailable())/1728;
                int stack = (taskItem.getAmount()-taskItem.getAvailable()-1728*box)/64;
                int single = (taskItem.getAmount()-taskItem.getAvailable())%64;
                lore.add(LangProvider.get("gui.liteitemlist.itemlist.element.require").copy()
                        .append(box!=0?LangProvider.get("msg.liteitemlist.gui.universal.box",box):Text.literal(""))
                        .append(stack!=0?LangProvider.get("msg.liteitemlist.gui.universal.stack",stack):Text.literal(""))
                        .append(single!=0?LangProvider.get("msg.liteitemlist.gui.universal.single",single):Text.literal("")));
            }

            lore.add(LangProvider.get("gui.liteitemlist.itemlist.element.comment").copy()
                    .append(taskItem.isImpt()?LangProvider.get("gui.liteitemlist.itemlist.element.impt"):Text.literal(""))
                    .append(taskItem.isHard()?LangProvider.get("gui.liteitemlist.itemlist.element.hard"):Text.literal("")));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getMsg()));
            lore.add(Text.literal(Formatting.GRAY + taskItem.getItem().toString()));
            lore.add(Text.empty());

            lore.add(LangProvider.get("gui.liteitemlist.itemlist.element.claim").copy().append((taskItem.getPrincipals().isEmpty()?LangProvider.get("gui.liteitemlist.itemlist.element.claim.none"):Text.literal(Formatting.YELLOW + String.join(",", taskItem.getPrincipals())))));
            lore.add(LangProvider.get("gui.liteitemlist.universal.click").copy().append(LangProvider.get(taskItem.getPrincipals().contains(player.getName().getString())?"gui.liteitemlist.itemlist.element.operation.unclaim":"gui.liteitemlist.itemlist.element.operation.claim")));
            lore.add(LangProvider.get("gui.liteitemlist.universal.shift_click").copy().append(LangProvider.get("gui.liteitemlist.itemlist.element.operation.properties")));



            displayStack.set(DataComponentTypes.LORE,new LoreComponent(lore));

            if(taskItem.getPrincipals().contains(player.getName().getString())){
                displayStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,true);
            }

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
                handleMultiFunctionClick(serverPlayer, funcType, actionType); // 处理对应功能
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
    private void handleMultiFunctionClick(ServerPlayerEntity player, FunctionType funcType, SlotActionType actionType) {
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
            player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.no_last_page"),true);
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
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.itemlist.statistics"));
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.itemlist.total_impt_hard_unclaimed",
                task.getItemList().getTaskItemCount(),
                task.getItemList().getImptCount(),
                task.getItemList().getHardCount(),
                task.getItemList().getUnclaimedCount()));
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.itemlist.process",
                task.getItemList().getFinishedCount(),
                task.getItemList().getTaskItemCount(),
                task.getItemList().getProgressPercentage(),
                task.getItemList().getFinishedCount(),
                task.getItemList().getOngoingCount(),
                task.getItemList().getNotStartCount()));
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.itemlist.bot",
                task.getStorageBotManager().getBots().size(),
                task.getStorageBotManager().getOnlineCount(player.server),
                task.getStorageBotManager().getOfflineCount(player.server)));
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.itemlist.member",
                task.getMembers().size()));
    }

    // 刷新列表
    private void refreshTaskItemList(ServerPlayerEntity player) {
        executeAutoRefresh();
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.refresh"), true);
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
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.filter.applied"), true);
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
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.filter.applied"), true);
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
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.filter.applied"), true);
    }

    // 下一页
    private void toNextPage(ServerPlayerEntity player) {
        if((currentPage+1)*TASK_ITEM_AREA_SIZE >= upStageTaskItemList.size()) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.no_next_page"), true);
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
        executeAutoRefresh();
    }

    // 打开菜单
    public static void openTaskItemListMenu(ServerPlayerEntity player, ItemListTask task) {
        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return LangProvider.get("gui.liteitemlist.itemlist.title",task.getName(),task.getItemList().getTaskItemCount());
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskItemListScreenHandler(syncId, (ServerPlayerEntity) player, task, task.getMember((ServerPlayerEntity) player).getListStatus());
            }
        });
    }
}