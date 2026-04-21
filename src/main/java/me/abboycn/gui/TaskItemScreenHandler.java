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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.abboycn.gui.TaskItemListScreenHandler.openTaskItemListMenu;

public class TaskItemScreenHandler extends ScreenHandler {
    public static final int MENU_SIZE = 9;
    private final Inventory menuInventory;
    private final ServerPlayerEntity player;
    private Map<Integer, TaskItemScreenHandler.FunctionType> slotToFuncMap;               // 功能区映射
    private TaskItem item;
    private ItemListTask task;
    private SuperInfo superInfo;

    private Timer guiRefreshTimer;
    private static final int REFRESH_INTERVAL = 3000;

    private enum FunctionType {
        BACK,
        INFO_OVERVIEW,
        REFRESH_LIST
    }

    public record SuperInfo(int currentPage, TaskItemListScreenHandler.FilterType_Clime filterTypeClime) {}

    public TaskItemScreenHandler(int syncId, PlayerInventory playerInv) {
        super(ScreenHandlerType.GENERIC_9X1, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = new SimpleInventory(MENU_SIZE);
        this.slotToFuncMap = new HashMap<>();
        initMenuSlots();
        startAutoRefresh();
    }

    public TaskItemScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory, ItemListTask task, TaskItem item, @Nullable SuperInfo superInfo) {
        super(ScreenHandlerType.GENERIC_9X1, syncId);
        this.player = (ServerPlayerEntity) playerInv.player;
        this.menuInventory = inventory;
        this.item = item;
        this.task = task;
        this.slotToFuncMap = new HashMap<>();
        this.superInfo = superInfo;
        checkSize(inventory, MENU_SIZE);
        inventory.onOpen(player);
        initMenuSlots();
        startAutoRefresh();
    }

    private void initMenuSlots() {
        if(slots.isEmpty()){
            menuInventory.clear();
            for (int i = 0; i < MENU_SIZE; i++) {
                int x = 8 + (i % 9) * 18;
                addSlot(new MenuSlot(menuInventory, i, x, 18));
            }
        }
        initFunctionArea();
    }

    // 初始化功能区
    private void initFunctionArea() {
        slotToFuncMap = new HashMap<>();

        // [0] 单击或关闭菜单返回
        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(Formatting.GOLD + "<= 返回物品列表"));
        menuInventory.setStack(0, backItem);
        slotToFuncMap.put(0, TaskItemScreenHandler.FunctionType.BACK);

        // [4] 信息
        ItemStack infoItem = new ItemStack(item.getItem());
        infoItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal(Formatting.BLUE + item.getItem().getName().getString()));
        infoItem.set(DataComponentTypes.LORE,new LoreComponent(item.getItemInfo()));
        menuInventory.setStack(4, infoItem);
        slotToFuncMap.put(4, TaskItemScreenHandler.FunctionType.INFO_OVERVIEW);

        // [2] 刷新物品
        ItemStack refreshItem = new ItemStack(Items.PAPER);
        refreshItem.set(DataComponentTypes.CUSTOM_NAME,Text.literal(Formatting.YELLOW + "刷新物品信息"));
        refreshItem.set(DataComponentTypes.LORE,new LoreComponent(List.of(
                Text.literal(Formatting.GRAY + "点击刷新物品列表")
        )));
        menuInventory.setStack(2, refreshItem);
        slotToFuncMap.put(2, TaskItemScreenHandler.FunctionType.REFRESH_LIST);

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
            case BACK:          // 返回上级菜单
                backToSuperMenu(player);
                break;
            case INFO_OVERVIEW: // 信息总览
                sendInfoOverview(player);
                break;
            case REFRESH_LIST:  // 刷新列表
                refreshTaskItem(player);
                break;
        }
    }

    private void backToSuperMenu(PlayerEntity player) {
        if (superInfo!=null){
            openTaskItemListMenu((ServerPlayerEntity) player,task,superInfo);
        }
        else{
            player.sendMessage(Text.literal(Formatting.RED+"找不到上级菜单"),true);
        }
    }

    // 信息总览
    private void sendInfoOverview(ServerPlayerEntity player) {
        player.closeHandledScreen();
        player.sendMessage(item.getItemInfo().stream().reduce(Text.literal(""), (s1, s2) -> s1.copy().append(Text.literal("\n")).append(s2)));
    }

    // 刷新列表
    private void refreshTaskItem(ServerPlayerEntity player) {
        refreshGui();
        player.sendMessage(Text.literal(Formatting.GREEN + "列表已刷新！"), true);
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
                player.server.execute(() -> refreshGui());
            }
        }, 0, REFRESH_INTERVAL); // 0延迟启动，每3000ms=3秒
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

    // 关闭菜单时刷新玩家物品栏并打开上级菜单
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
    public static void openTaskItemMenu(ServerPlayerEntity player, ItemListTask task, TaskItem item, @Nullable SuperInfo superInfo) {
        player.openHandledScreen(new net.minecraft.screen.NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("任务"+task.getName()+"中的"+item.getItem().getName().getString());
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
                return new TaskItemScreenHandler(syncId, playerInv, new SimpleInventory(MENU_SIZE), task, item, superInfo);
            }
        });
    }
}
