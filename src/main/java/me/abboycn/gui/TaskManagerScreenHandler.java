package me.abboycn.gui;

import me.abboycn.LiteItemListFabric;
import me.abboycn.executor.CMDTaskSwitch;
import me.abboycn.resource.LangProvider;
import me.abboycn.task.ItemListTask;
import me.abboycn.task.TaskMember;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
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
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;

import static me.abboycn.gui.TaskItemListScreenHandler.openTaskItemListMenu;

public class TaskManagerScreenHandler extends AbstractLiteItemListMenu {
    private static final ScreenHandlerType<GenericContainerScreenHandler> MENU_TYPE = ScreenHandlerType.GENERIC_9X4;

    private static final int FUNCTION_AREA_END = 9;
    public static final int TASK_AREA_START = FUNCTION_AREA_END;
    public static final int TASK_AREA_END = 36;
    public static final int TASK_AREA_SIZE = TASK_AREA_END-TASK_AREA_START;

    private FilterType_Joined filterTypeJoined;
    private int currentPage = 0;
    private Collection<ItemListTask> originalTaskList;
    private Collection<ItemListTask> upStageTaskList;
    private Map<Integer, TaskManagerScreenHandler.FunctionType> slotToFuncMap;
    private Map<Integer, ItemListTask> slotToTaskMap;

    private enum FunctionType{
        PAST_PAGE,
        FILTER_JOINED,
        NEXT_PAGE
    }

    public enum FilterType_Joined {
        DEFAULT,
        JOINED,
        NOTJOINED
    }

    public record Task_ItemStack(ItemListTask task, ItemStack itemStack) { }

    public TaskManagerScreenHandler(int syncId, ServerPlayerEntity player){
        super(syncId, MENU_TYPE, 5000, player);
        this.filterTypeJoined = TaskManagerScreenHandler.FilterType_Joined.DEFAULT;
        this.originalTaskList = LiteItemListFabric.taskManager.getTasks();
        this.upStageTaskList = getFilteredListFromOriginal(player);
        this.slotToFuncMap = new HashMap<>();
        this.slotToTaskMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    //初始化
    @Override
    protected void initMenuInventory(){
        initFunctionArea();
        initTaskArea();
    }

    private void initFunctionArea(){
        slotToFuncMap = new HashMap<>();

        // [0] 上一页
        MenuFunctionItem pastPageItem = new MenuFunctionItem(Items.ARROW, LangProvider.get("gui.liteitemlist.universal.last_page"), new ArrayList<>());
        menuInventory.setStack(0, pastPageItem.getItemStack());
        slotToFuncMap.put(0, TaskManagerScreenHandler.FunctionType.PAST_PAGE);

        // [4] 筛选：加入
        MenuFunctionItem filterItem_Joined = new MenuFunctionItem(Items.HOPPER, LangProvider.get("gui.liteitemlist.taskmanager.func.filter_participation"), List.of(
                Text.literal(filterTypeJoined == TaskManagerScreenHandler.FilterType_Joined.DEFAULT ? Formatting.WHITE + "-> " : Formatting.GRAY + "   ").append(LangProvider.get("gui.liteitemlist.universal.filter.default")),
                Text.literal(filterTypeJoined == TaskManagerScreenHandler.FilterType_Joined.JOINED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.taskmanager.func.filter_participation.participated")),
                Text.literal(filterTypeJoined == TaskManagerScreenHandler.FilterType_Joined.NOTJOINED ? Formatting.WHITE + "-> " : Formatting.GRAY + "    ").append(LangProvider.get("gui.liteitemlist.taskmanager.func.filter_participation.not_participated"))
        ));
        menuInventory.setStack(4, filterItem_Joined.getItemStack());
        slotToFuncMap.put(4, TaskManagerScreenHandler.FunctionType.FILTER_JOINED);

        // [8] 下一页
        MenuFunctionItem nextPageItem = new MenuFunctionItem(Items.ARROW, LangProvider.get("gui.liteitemlist.universal.next_page"), new ArrayList<>());
        menuInventory.setStack(8, nextPageItem.getItemStack());
        slotToFuncMap.put(8, TaskManagerScreenHandler.FunctionType.NEXT_PAGE);
    }

    private void initTaskArea(){
        slotToTaskMap = new HashMap<>();
        Collection<Task_ItemStack> ls = new ArrayList<>();

        if(currentPage*TASK_AREA_SIZE>upStageTaskList.size()){
            currentPage=upStageTaskList.size()/TASK_AREA_SIZE;
        }
        for(int i = currentPage*TASK_AREA_SIZE; i<Integer.min((currentPage+1)*TASK_AREA_SIZE, upStageTaskList.size()); i++){
            ItemListTask task = upStageTaskList.stream().toList().get(i);

            ItemStack displayStack = new ItemStack(Items.BOOK);
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + task.getName()));
            displayStack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    LangProvider.get("gui.liteitemlist.taskmanager.element.creator",task.getCreator()),
                    LangProvider.get("gui.liteitemlist.taskmanager.element.members",(task.getMembers().stream().limit(3).map(TaskMember::getName).collect(Collectors.joining(", ")) + (task.getMembers().size()>3?"...":"")),task.getMembers().size()),
                    LangProvider.get("gui.liteitemlist.taskmanager.element.time",task.getFormattedTime()),
                    Text.empty(),
                    LangProvider.get("gui.liteitemlist.taskmanager.element.process", task.getItemList().getFinishedCount(), task.getItemList().getTaskItemCount(),
                            task.getItemList().getProgressPercentage()),
                    Text.empty(),
                    LangProvider.get("gui.liteitemlist.universal.click").copy().append(LangProvider.get("gui.liteitemlist.taskmanager.element.switch")),
                    LangProvider.get("gui.liteitemlist.universal.shift_click").copy().append(LangProvider.get("gui.liteitemlist.taskmanager.element.open")),
                    LangProvider.get("gui.liteitemlist.universal.drop").copy().append(LangProvider.get(task.containsMember(player)?"gui.liteitemlist.taskmanager.element.leave":"gui.liteitemlist.taskmanager.element.join"))
            )));
            if(LiteItemListFabric.taskManager.getTaskByPlayer(player)!=null&&LiteItemListFabric.taskManager.getTaskByPlayer(player).equals(task)){
                displayStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,true);
            }

            ls.add(new TaskManagerScreenHandler.Task_ItemStack(task, displayStack));
        }

        int slot = TASK_AREA_START;
        for(TaskManagerScreenHandler.Task_ItemStack task_itemStack : ls){
            if(slot<MENU_SIZE){
                slotToTaskMap.put(slot, task_itemStack.task);
                menuInventory.setStack(slot, task_itemStack.itemStack);
                slot++;
            }
        }

        while (slot < TASK_AREA_END) {
            menuInventory.setStack(slot++, ItemStack.EMPTY);
        }
    }

    private void updateTaskList(ServerPlayerEntity player) {
        this.originalTaskList = LiteItemListFabric.taskManager.getTasks();
        this.upStageTaskList = getFilteredListFromOriginal(player);
    }

    @Override
    protected void executeAutoRefresh(){
        updateTaskList(player);
        refreshGui();
    }

    private Collection<ItemListTask> getFilteredListFromOriginal(ServerPlayerEntity player){
        Collection<ItemListTask> ret = switch (filterTypeJoined){
            case DEFAULT -> originalTaskList;
            case JOINED -> originalTaskList.stream().filter(t -> t.containsMember(player)).toList();
            case NOTJOINED ->  originalTaskList.stream().filter(t -> (!t.containsMember(player))).toList();
        };
        return ret;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player){
        if (!(player instanceof ServerPlayerEntity serverPlayer) || slotIndex < 0 || slotIndex >= MENU_SIZE) {
            return;
        }
        ItemStack clickedStack = getSlot(slotIndex).getStack();
        if (clickedStack.isEmpty()) {
            return;
        }

        // 功能区
        if (slotIndex < FUNCTION_AREA_END) {
            TaskManagerScreenHandler.FunctionType funcType = slotToFuncMap.get(slotIndex); // 从映射获取功能类型
            if (funcType != null) {
                handleMultiFunctionClick(serverPlayer, funcType); // 处理对应功能
            }
            return;
        }

        // 任务展示区
        ItemListTask task = slotToTaskMap.get(slotIndex);
        if (task != null) {
            handleTaskClick(serverPlayer, task, actionType);
            initMenuSlots();
            sendContentUpdates();
        }
    }

    private void handleMultiFunctionClick(ServerPlayerEntity player, TaskManagerScreenHandler.FunctionType funcType) {
        switch (funcType) {
            case PAST_PAGE -> toPastPage(player);                   // 上一页
            case FILTER_JOINED -> filterJoined(player);             // 筛选在线状态
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
        refreshGui();
    }

    // 切换筛选
    private void filterJoined(ServerPlayerEntity player) {
        filterTypeJoined = switch(filterTypeJoined){
            case DEFAULT -> TaskManagerScreenHandler.FilterType_Joined.JOINED;
            case JOINED -> TaskManagerScreenHandler.FilterType_Joined.NOTJOINED;
            case NOTJOINED -> TaskManagerScreenHandler.FilterType_Joined.DEFAULT;
        };
        executeAutoRefresh();
        player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.filter.applied"), true);
    }

    // 下一页
    private void toNextPage(ServerPlayerEntity player) {
        if((currentPage+1)*TASK_AREA_SIZE >= upStageTaskList.size()) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.gui.universal.no_next_page"), true);
            refreshGui();
            return;
        }
        currentPage++;
        refreshGui();
    }

    // 处理物品展示区点击
    private void handleTaskClick(ServerPlayerEntity player, ItemListTask task, SlotActionType actionType) {
        if (actionType == SlotActionType.PICKUP) {
            if(!task.containsMember(player)){
                player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"),true);
                return;
            }
            player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
            player.getCommandTags().add(task.getTaskCommandTag());
        }
        else if (actionType == SlotActionType.QUICK_MOVE) {
            if (task.containsMember(player)) {
                if(!LiteItemListFabric.taskManager.getTaskByPlayer(player).equals(task)){
                    CMDTaskSwitch.executeOperation(player, task);
                }
                player.closeHandledScreen();
                openTaskItemListMenu(player, task);
            }
            else {
                player.sendMessage(LangProvider.get("msg.liteitemlist.cmd.error.not_in_task"),true);
            }
        }
        else if (actionType == SlotActionType.THROW) {
            if (task.containsMember(player)) {
                player.getCommandTags().removeIf(tag -> tag.contains("in_task_"));
                task.removeMember(player);
            }
            else {
                player.addCommandTag(task.getTaskCommandTag());
                task.addMember(player);
            }
        }
        refreshGui();
    }

    public static void openTaskManagerMenu(ServerPlayerEntity player) {
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return LangProvider.get("gui.liteitemlist.taskmanager.title");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskManagerScreenHandler(syncId, (ServerPlayerEntity) player);
            }
        });
    }
}
