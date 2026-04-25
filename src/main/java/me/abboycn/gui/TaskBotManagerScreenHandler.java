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

public class TaskBotManagerScreenHandler extends LiteItemListMenu{//TODO:FINISH THIS PART
    private static final ScreenHandlerType<GenericContainerScreenHandler> MENU_TYPE = ScreenHandlerType.GENERIC_9X4;

    private static final int FUNCTION_AREA_END = 9;
    public static final int STORAGE_BOT_AREA_START = FUNCTION_AREA_END;
    public static final int STORAGE_BOT_AREA_END = 36;
    public static final int STORAGE_BOT_AREA_SIZE = STORAGE_BOT_AREA_END-STORAGE_BOT_AREA_START;

    private FilterType_Storage filterTypeStorage;
    private final ItemListTask task;
    private int currentPage = 0;
    private Collection<StorageBot> originalStorageBotList;
    private Collection<StorageBot> upStageStorageBotList;
    private Map<Integer, FunctionType> slotToFuncMap;
    private Map<Integer, StorageBot> slotToStorageBotMap;

    private enum FunctionType{
        PAST_PAGE,
        BACK,
        FILTER_STORAGE,
        NEXT_PAGE
    }

    public enum FilterType_Storage {
        DEFAULT,
        HASSPACE,
        FULL
    }

    public record StorageBot_ItemStack(StorageBot storageBot, ItemStack itemStack) { }

    public TaskBotManagerScreenHandler(int syncId, ServerPlayerEntity player, ItemListTask task){
        super(syncId, MENU_TYPE, 5000, player);
        this.task = task;
        this.originalStorageBotList = task.getStorageBotManager().getBots();
        this.upStageStorageBotList = originalStorageBotList;
        this.slotToFuncMap = new HashMap<>();
        this.slotToStorageBotMap = new HashMap<>();
        this.filterTypeStorage = FilterType_Storage.DEFAULT;
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

        // [7] 筛选：已用空间
        MenuFunctionItem filterItem_Storage = new MenuFunctionItem(Items.HOPPER, Text.literal(Formatting.YELLOW + "筛选已用空间"), List.of(
                Text.literal(filterTypeStorage == FilterType_Storage.DEFAULT ? Formatting.WHITE + "-> 全部" : Formatting.GRAY + "    全部"),
                Text.literal(filterTypeStorage == FilterType_Storage.HASSPACE ? Formatting.WHITE + "-> 空间未满" : Formatting.GRAY + "    空间未满"),
                Text.literal(filterTypeStorage == FilterType_Storage.FULL ? Formatting.WHITE + "-> 空间已满" : Formatting.GRAY + "    空间已满")
        ));
        menuInventory.setStack(7, filterItem_Storage.getItemStack());
        slotToFuncMap.put(7, TaskBotManagerScreenHandler.FunctionType.FILTER_STORAGE);

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
            displayStack.set(DataComponentTypes.CUSTOM_NAME,Text.literal((bot.getUsedStorage(player.server)>=41?Formatting.RED:Formatting.GREEN) + bot.getName()));
            displayStack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                    Text.literal(bot.isOnline(player.server)?(Formatting.GREEN + "在线"):(Formatting.GRAY + "离线")),
                    Text.literal(Formatting.GRAY + "已用：" + (bot.getUsedStorage(player.server)>=41?Formatting.RED:Formatting.GREEN) + bot.getUsedStorage(player.server)),
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

    protected void executeAutoRefresh(){
        refreshGui();
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
