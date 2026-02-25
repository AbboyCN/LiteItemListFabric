package me.abboycn.bot;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
    private PlayerInventory m_inventory = new PlayerInventory(null);

    public StorageBot(String task, int id) {
        this.m_task = task;
        this.m_id = (short) id;
        this.m_name = m_task + "_" + Integer.toString(m_id);
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

    public PlayerInventory getInventory() {
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

    public void setInventory(PlayerInventory inventory) {
        this.m_inventory = inventory;
    }

    public boolean refreshInventory(MinecraftServer server) {
        if(!isOnline(server)) return false;
        m_inventory = getPlayer(server).getInventory();
        return true;
    }

    public void playerSummonFake(ServerPlayerEntity player) {
        if (player == null) return;
        if (m_name.length() > 16) {
            player.sendMessage(Text.literal("§c创建假人失败:名称过长!"));
            return;
        }
        if (EntityPlayerMPFake.isSpawningPlayer(m_name)) {
            player.sendMessage(Text.literal("§c创建假人失败:该假人正在创建!"));
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
                , true);

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
                        m_inventory = finalAsyncFake.getInventory();
                        finalAsyncFake.addCommandTag("storage_bot");
                    });
                    return;
                }
            }
            server.execute(() -> player.sendMessage(Text.literal("§c假人创建超时: " + fakeName)));
        }).start();
    }

    public int getUsedStorage(MinecraftServer server) {
        if(isOnline(server)&&!refreshInventory(server)) return -1;

        int emptySlots = m_inventory.size();
        for (int i = 0; i < m_inventory.size(); i++) {
            if (m_inventory.getStack(i).isEmpty()) {
                emptySlots--;
            }
        }
        return emptySlots;
    }

    public boolean isOnline(MinecraftServer server) {
        return server.getPlayerManager().getPlayer(this.m_name)!=null;
    }

    public ServerPlayerEntity getPlayer(MinecraftServer server) {
        return server.getPlayerManager().getPlayer(this.m_name);
    }


}
