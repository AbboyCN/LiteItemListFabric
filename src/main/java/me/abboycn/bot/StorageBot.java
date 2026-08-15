package me.abboycn.bot;

import carpet.patches.EntityPlayerMPFake;
import me.abboycn.config.ConfigManager;
import me.abboycn.resource.LangProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.Set;
import com.google.gson.annotations.SerializedName;

public class StorageBot {
    @SerializedName("task")
    private String m_task;
    @SerializedName("id")
    private short m_id;
    @SerializedName("name")
    private String m_name;
    @SerializedName("inventory")
    private StorageBotInventory m_inventory = new StorageBotInventory();

    public StorageBot(String task, int id) {
        this.m_task = task;
        this.m_id = (short) id;
        this.m_name = m_task + "_" + Integer.toString(m_id) + ConfigManager.INSTANCE.getConfig().getBotNameSuffix();
    }

    public String getTask() {
        return this.m_task;
    }

    public short getId() {
        return this.m_id;
    }

    public String getName() {
        return this.m_name;
    }

    public StorageBotInventory getInventory() {
        return this.m_inventory;
    }

    public void setTask(String task) {
        this.m_task = task;
    }

    public void setId(short id) {
        this.m_id = id;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public void setInventory(StorageBotInventory inventory) { this.m_inventory = inventory; }

    public void setInventory(PlayerInventory inventory) {
        this.m_inventory.syncFromInventory(inventory);
    }

    public boolean refreshInventory(MinecraftServer server) {
        if(!isOnline(server)) return false;
        m_inventory.syncFromInventory(getPlayer(server).getInventory());
        return true;
    }

    public void playerSummonFake(ServerPlayerEntity player) {
        if (player == null) return;
        if (m_name.length() > 16) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.bot.summon.failed.name_to_long"));
            return;
        }
        if (EntityPlayerMPFake.isSpawningPlayer(m_name)) {
            player.sendMessage(LangProvider.get("msg.liteitemlist.bot.summon.failed.summoning"));
            return;
        }
        ServerPlayerEntity fake = getPlayer(player.server);
        if (fake != null) {
            fake.teleport(player.getServerWorld()
                    , player.getX()
                    , player.getY()
                    , player.getZ()
                    , Set.of()
                    , player.getYaw()
                    , player.getPitch()
                    , false);
            return;
        }
        EntityPlayerMPFake.createFake(m_name
                , player.server
                , player.getPos()
                , player.getYaw()
                , player.getPitch()
                , player.getEntityWorld().getRegistryKey()
                , GameMode.SURVIVAL
                , false);

        MinecraftServer server = player.server;
        String fakeName = m_name;

        new Thread(() -> {
            ServerPlayerEntity asyncFake;
            for (int retry = 0; retry < 10; ++retry) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                asyncFake = server.getPlayerManager().getPlayer(fakeName);
                if (asyncFake != null) {
                    ServerPlayerEntity finalAsyncFake = asyncFake;
                    server.execute(() -> {
                        m_inventory.syncFromInventory(finalAsyncFake.getInventory());
                        finalAsyncFake.addCommandTag("storage_bot");
                    });
                    return;
                }
            }
            server.execute(() -> player.sendMessage(LangProvider.get("msg.liteitemlist.bot.summon.failed.timedout", fakeName)));
        }).start();
    }

    public int getUsedStorage() {
        return m_inventory.getUsedSlots();
    }

    public ItemStack getHead(MinecraftServer server) {
        ItemStack ret = new ItemStack(Items.PLAYER_HEAD);
        ServerPlayerEntity player = getPlayer(server);
        if(player != null) {
            ret.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
        }
        return ret;
    }

    public boolean isFull() {
        return m_inventory.isFull();
    }

    public boolean isOnline(MinecraftServer server) {
        return server.getPlayerManager().getPlayer(this.m_name)!=null;
    }

    public ServerPlayerEntity getPlayer(MinecraftServer server) {
        return server.getPlayerManager().getPlayer(this.m_name);
    }
}
