package me.abboycn.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class LiteItemListMenu extends ScreenHandler {
    protected int MENU_SIZE;
    protected int REFRESH_INTERVAL;

    protected final Inventory menuInventory;
    protected final ServerPlayerEntity player;

    protected Timer guiRefreshTimer;

    public static Map<ScreenHandlerType<GenericContainerScreenHandler>, Integer> handlerTypeToSize = new HashMap<>() {{
        put(ScreenHandlerType.GENERIC_9X1, 9);
        put(ScreenHandlerType.GENERIC_9X2, 18);
        put(ScreenHandlerType.GENERIC_9X3, 27);
        put(ScreenHandlerType.GENERIC_9X4, 36);
        put(ScreenHandlerType.GENERIC_9X5, 45);
        put(ScreenHandlerType.GENERIC_9X6, 54);
    }};

    public LiteItemListMenu(int syncId, ScreenHandlerType handlerType, int REFRESH_INTERVAL, ServerPlayerEntity player) {
        super(handlerType,syncId);
        this.MENU_SIZE = handlerTypeToSize.get(handlerType);
        this.menuInventory = new SimpleInventory(MENU_SIZE);
        this.player = player;
        this.REFRESH_INTERVAL = REFRESH_INTERVAL;
        checkSize(menuInventory, MENU_SIZE);
        menuInventory.onOpen(player);
    }

    protected void initMenuSlots() {
        if(slots.isEmpty()){
            menuInventory.clear();
            for (int i = 0; i < MENU_SIZE; i++) {
                int x = 8 + (i % 9) * 18;
                int y = 18 + (i / 9) * 18;
                addSlot(new MenuSlot(menuInventory, i, x, y));
            }
        }
        initMenuInventory();
    }

    protected abstract void initMenuInventory();

    protected void refreshGui(){
        initMenuSlots();
        sendContentUpdates();
        updateToClient();
        player.playerScreenHandler.updateToClient();
    }

    protected void startAutoRefresh() {
        if (guiRefreshTimer != null) return;
        guiRefreshTimer = new Timer(true);
        guiRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (player.isDisconnected()) {
                    cancel();
                    guiRefreshTimer.cancel();
                    return;
                }
                player.server.execute(() -> executeAutoRefresh());
            }
        }, REFRESH_INTERVAL, REFRESH_INTERVAL); // 0延迟启动，每3000ms=3秒
    }

    protected abstract void executeAutoRefresh();

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

    @Override
    public void onClosed(PlayerEntity player){
        menuInventory.onClose(player);
        updateToClient();
        player.playerScreenHandler.updateToClient();
        if (guiRefreshTimer != null) {
            guiRefreshTimer.cancel();
            guiRefreshTimer = null;
        }
    }
}
