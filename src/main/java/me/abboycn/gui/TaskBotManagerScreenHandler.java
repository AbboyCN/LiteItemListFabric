package me.abboycn.gui;

import me.abboycn.bot.StorageBot;
import me.abboycn.task.ItemListTask;
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

import static me.abboycn.gui.TaskItemListScreenHandler.openTaskItemListMenu;

public class TaskBotManagerScreenHandler extends LiteItemListMenu{//TODO:FINISH THIS PART
    private static final ScreenHandlerType<GenericContainerScreenHandler> MENU_TYPE = ScreenHandlerType.GENERIC_9X4;

    private static final int FUNCTION_AREA_END = 9;
    public static final int STORAGE_BOT_AREA_START = FUNCTION_AREA_END;
    public static final int STORAGE_BOT_AREA_END = 36;
    public static final int STORAGE_BOT_AREA_SIZE = STORAGE_BOT_AREA_END-STORAGE_BOT_AREA_START;

    private FilterType_Storage filterTypeStorage;
    private FilterType_Online filterTypeOnline;
    private final ItemListTask task;
    private int currentPage = 0;
    private Collection<StorageBot> originalStorageBotList;
    private Collection<StorageBot> upStageStorageBotList;
    private Map<Integer, FunctionType> slotToFuncMap;
    private Map<Integer, StorageBot> slotToStorageBotMap;

    private enum FunctionType{
        PAST_PAGE,
        BACK,
        NEWBOT,
        SUMMONALL,
        FILTER_STORAGE,
        FILTER_ONLINE,
        NEXT_PAGE
    }

    public enum FilterType_Storage {
        DEFAULT,
        HASSPACE,
        FULL
    }

    public enum FilterType_Online {
        DEFAULT,
        ONLINE,
        OFFLINE
    }

    public record StorageBot_ItemStack(StorageBot storageBot, ItemStack itemStack) { }

    public TaskBotManagerScreenHandler(int syncId, ServerPlayerEntity player, ItemListTask task){
        super(syncId, MENU_TYPE, 5000, player);
        this.filterTypeStorage = FilterType_Storage.DEFAULT;
        this.filterTypeOnline = FilterType_Online.DEFAULT;
        this.task = task;
        this.originalStorageBotList = task.getStorageBotManager().getBots();
        this.upStageStorageBotList = getFilteredListFromOriginal(player);
        this.slotToFuncMap = new HashMap<>();
        this.slotToStorageBotMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    //初始化
    @Override
    protected void initMenuInventory(){
        initFunctionArea();
        initStorageBotArea();
    }

    private void initFunctionArea(){
        slotToFuncMap = new HashMap<>();

        // [0] 上一页
        MenuFunctionItem pastPageItem = new MenuFunctionItem(Items.ARROW, Text.literal(Formatting.GOLD + "<< 上一页"), new ArrayList<>());
        menuInventory.setStack(0, pastPageItem.getItemStack());
        slotToFuncMap.put(0, TaskBotManagerScreenHandler.FunctionType.PAST_PAGE);

        // [1] 单击返回
        MenuFunctionItem backItem = new MenuFunctionItem(Items.SPECTRAL_ARROW, Text.literal(Formatting.GOLD + "<= 返回物品列表"), new ArrayList<>());
        menuInventory.setStack(1, backItem.getItemStack());
        slotToFuncMap.put(1, TaskBotManagerScreenHandler.FunctionType.BACK);

        // [2] 新建假人
        MenuFunctionItem newItem = new MenuFunctionItem(Items.NETHER_STAR, Text.literal(Formatting.YELLOW + "新建存储假人"), List.of(
                Text.literal(Formatting.GRAY + "点击创建新的存储假人并召唤到自己的位置")
        ));
        menuInventory.setStack(2, newItem.getItemStack());
        slotToFuncMap.put(2, TaskBotManagerScreenHandler.FunctionType.NEWBOT);

        // [2] 召唤全部
        MenuFunctionItem summonItem = new MenuFunctionItem(Items.PANDA_SPAWN_EGG, Text.literal(Formatting.YELLOW + "召唤全部假人"), List.of(
                Text.literal(Formatting.GRAY + "点击召唤全部假人到自己的位置"),
                Text.literal(Formatting.RED + "[!] " + Formatting.BOLD + "警告：请务必保证周边区域安全，大量假人同时召唤可能导致"),
                Text.literal(Formatting.RED + "" + Formatting.BOLD + "玩家或假人被挤到不安全的位置，造成不必要的损失！")
        ));
        menuInventory.setStack(3, summonItem.getItemStack());
        slotToFuncMap.put(3, TaskBotManagerScreenHandler.FunctionType.SUMMONALL);

        // [6] 筛选：已用空间
        MenuFunctionItem filterItem_Storage = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + "筛选已用空间"), List.of(
                Text.literal(filterTypeStorage == FilterType_Storage.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeStorage == FilterType_Storage.HASSPACE ? Formatting.WHITE + "-> 空间未满" : Formatting.GRAY + "    空间未满"),
                Text.literal(filterTypeStorage == FilterType_Storage.FULL ? Formatting.WHITE + "-> 空间已满" : Formatting.GRAY + "    空间已满")
        ));
        menuInventory.setStack(6, filterItem_Storage.getItemStack());
        slotToFuncMap.put(6, TaskBotManagerScreenHandler.FunctionType.FILTER_STORAGE);

        // [7] 筛选：在线状态
        MenuFunctionItem filterItem_Online = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + "筛选在线状态"), List.of(
                Text.literal(filterTypeOnline == FilterType_Online.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeOnline == FilterType_Online.ONLINE ? Formatting.WHITE + "-> 在线" : Formatting.GRAY + "    在线"),
                Text.literal(filterTypeOnline == FilterType_Online.OFFLINE ? Formatting.WHITE + "-> 离线" : Formatting.GRAY + "    离线")
        ));
        menuInventory.setStack(7, filterItem_Online.getItemStack());
        slotToFuncMap.put(7, TaskBotManagerScreenHandler.FunctionType.FILTER_ONLINE);

        // [8] 下一页
        MenuFunctionItem nextPageItem = new MenuFunctionItem(Items.ARROW, Text.literal(Formatting.GOLD + "下一页 >>"), new ArrayList<>());
        menuInventory.setStack(8, nextPageItem.getItemStack());
        slotToFuncMap.put(8, TaskBotManagerScreenHandler.FunctionType.NEXT_PAGE);

        // 剩余留空
        for (int i = 0; i < FUNCTION_AREA_END; i++) {
            if(menuInventory.getStack(i) == null){
                menuInventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private void initStorageBotArea(){
        slotToStorageBotMap = new HashMap<>();
        Collection<StorageBot_ItemStack> ls = new ArrayList<>();

        if(currentPage*STORAGE_BOT_AREA_SIZE>upStageStorageBotList.size()){
            currentPage=upStageStorageBotList.size()/STORAGE_BOT_AREA_SIZE;
        }
        for(int i = currentPage*STORAGE_BOT_AREA_SIZE; i<Integer.min((currentPage+1)*STORAGE_BOT_AREA_SIZE, upStageStorageBotList.size()); i++){
            StorageBot bot = upStageStorageBotList.stream().toList().get(i);

            ItemStack displayStack = new ItemStack(Items.PLAYER_HEAD);
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal((bot.isFull(player.server)?Formatting.RED:Formatting.GREEN) + bot.getName()));
            displayStack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal(bot.isOnline(player.server)?(Formatting.GREEN + "在线"):(Formatting.GRAY + "离线")),
                    Text.literal(Formatting.GRAY + "已用：" + (bot.isFull(player.server)?Formatting.RED:Formatting.GREEN) + bot.getUsedStorage(player.server) + " / 41"),
                    Text.empty(),
                    Text.literal(Formatting.GRAY + "点击召唤存储假人")
            )));

            ls.add(new StorageBot_ItemStack(bot, displayStack));
        }

        int slot = STORAGE_BOT_AREA_START;
        for(StorageBot_ItemStack storageBot_itemStack : ls){
            if(slot<MENU_SIZE){
                slotToStorageBotMap.put(slot, storageBot_itemStack.storageBot);
                menuInventory.setStack(slot, storageBot_itemStack.itemStack);
                slot++;
            }
        }

        while (slot < STORAGE_BOT_AREA_END) {
            menuInventory.setStack(slot++, ItemStack.EMPTY);
        }
    }

    private void updateStorageBotList(ServerPlayerEntity player) {
        this.originalStorageBotList = task.getStorageBotManager().getBots();
        this.upStageStorageBotList = getFilteredListFromOriginal(player);
    }

    @Override
    protected void executeAutoRefresh(){
        updateStorageBotList(player);
        refreshGui();
    }

    private Collection<StorageBot> getFilteredListFromOriginal(ServerPlayerEntity player){
        Collection<StorageBot> ret = switch (filterTypeStorage){
            case DEFAULT -> originalStorageBotList;
            case HASSPACE -> originalStorageBotList.stream().filter(s -> (!s.isFull(player.server))).toList();
            case FULL ->  originalStorageBotList.stream().filter(s -> s.isFull(player.server)).toList();
        };
        ret = switch (filterTypeOnline){
            case DEFAULT -> ret;
            case ONLINE -> ret.stream().filter(s -> s.isOnline(player.server)).toList();
            case OFFLINE -> ret.stream().filter(s -> (!s.isOnline(player.server))).toList();
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
            TaskBotManagerScreenHandler.FunctionType funcType = slotToFuncMap.get(slotIndex); // 从映射获取功能类型
            if (funcType != null) {
                handleMultiFunctionClick(serverPlayer, funcType); // 处理对应功能
            }
            return;
        }

        // 物品展示区
        StorageBot bot = slotToStorageBotMap.get(slotIndex);
        if (bot != null) {
            handleStorageBotClick(serverPlayer, bot, actionType);
            initMenuSlots();
            sendContentUpdates();
        }
    }

    private void handleMultiFunctionClick(ServerPlayerEntity player, TaskBotManagerScreenHandler.FunctionType funcType) {
        switch (funcType) {
            case PAST_PAGE -> toPastPage(player);                   // 上一页
            case BACK -> backToSuperMenu(player);                   // 返回上一级
            case NEWBOT -> newBot(player);                          // 新建假人
            case SUMMONALL -> spawnAll(player);                     // 召唤全部
            case FILTER_STORAGE -> filterStorage(player);           // 筛选存储空间
            case FILTER_ONLINE -> filterOnline(player);             // 筛选在线状态
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
        refreshGui();
    }

    // 返回上一级
    private void backToSuperMenu(ServerPlayerEntity player) {
        openTaskItemListMenu(player, task);
    }

    // 新建假人
    private void newBot(ServerPlayerEntity player) {
        task.getStorageBotManager().newBot().playerSummonFake(player);
    }

    // 召唤全部
    private void spawnAll(ServerPlayerEntity player) {
        task.getStorageBotManager().summonAllBots(player);
    }

    // 切换筛选
    private void filterStorage(ServerPlayerEntity player) {
        filterTypeStorage = switch(filterTypeStorage){
            case DEFAULT -> FilterType_Storage.HASSPACE;
            case HASSPACE -> FilterType_Storage.FULL;
            case FULL -> FilterType_Storage.DEFAULT;
        };
        upStageStorageBotList=getFilteredListFromOriginal(player);
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    private void filterOnline(ServerPlayerEntity player) {
        filterTypeOnline = switch(filterTypeOnline){
            case DEFAULT -> FilterType_Online.ONLINE;
            case ONLINE -> FilterType_Online.OFFLINE;
            case OFFLINE -> FilterType_Online.DEFAULT;
        };
        upStageStorageBotList=getFilteredListFromOriginal(player);
        refreshGui();
        player.sendMessage(Text.literal(Formatting.YELLOW + "筛选器已应用！"), true);
    }

    // 下一页
    private void toNextPage(ServerPlayerEntity player) {
        if((currentPage+1)*STORAGE_BOT_AREA_SIZE >= upStageStorageBotList.size()) {
            player.sendMessage(Text.literal(Formatting.RED+"已经是最后一页了!"),true);
            refreshGui();
            return;
        }
        currentPage++;
        refreshGui();
    }

    // 处理物品展示区点击
    private void handleStorageBotClick(ServerPlayerEntity player, StorageBot bot, SlotActionType actionType) {
        if (actionType == SlotActionType.PICKUP) {
            bot.playerSummonFake(player);
        }
        else if (actionType == SlotActionType.QUICK_MOVE) {
            player.sendMessage(Text.literal("aaa"));
        }
        refreshGui();
    }

    public static void openTaskBotManagerMenu(ServerPlayerEntity player, ItemListTask task) {
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("任务"+task.getName()+"的存储假人管理");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskBotManagerScreenHandler(syncId, (ServerPlayerEntity) player, task);
            }
        });
    }
}
