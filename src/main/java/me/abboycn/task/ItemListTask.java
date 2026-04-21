package me.abboycn.task;

import me.abboycn.LiteItemListFabric;
import me.abboycn.bot.TaskStorageBotManager;
import me.abboycn.data.LitematicaReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.annotations.SerializedName;

public class ItemListTask {
    @SerializedName("name")
    private String m_name;
    @SerializedName("id")
    private int m_id;
    @SerializedName("creator")
    private String m_creator;
    @SerializedName("members")
    private Collection<String> m_members = new ArrayList<>();
    @SerializedName("time")
    private Long m_time;
    @SerializedName("botManager")
    private TaskStorageBotManager m_botManager;
    @SerializedName("itemList")
    private TaskItemList m_itemList;

    private transient Timer storageRefreshTimer;
    private static final int REFRESH_INTERVAL = 3000;

    public ItemListTask(String name, int id, ServerPlayerEntity creator) {
        m_name = name;
        m_id = id;
        m_creator = creator.getName().getString();
        addMember(m_creator);
        m_time = System.currentTimeMillis();
        m_botManager = new TaskStorageBotManager(name);
        m_itemList = new TaskItemList();
        startAutoRefreshStorage(creator.server);
    }

    public String getName() {
        return this.m_name;
    }

    public int getId() {
        return this.m_id;
    }

    public String getCreator() {
        return this.m_creator;
    }

    public Collection<String> getMembers() {
        return this.m_members;
    }

    public Long getTime() {
        return m_time;
    }

    public TaskStorageBotManager getStorageBotManager() {
        return this.m_botManager;
    }

    public TaskItemList getItemList() {
        return m_itemList;
    }

    public void setName(String name) {
        this.m_name = name;
    }

    public void setId(int id) {
        this.m_id = id;
    }

    public void setCreator(String creator) {
        this.m_creator = creator;
    }

    public void setCreator(ServerPlayerEntity creator) {
        this.m_creator = creator.getName().getString();
    }

    public void setMembers(Collection<String> members) {
        this.m_members = members;
    }

    public void addMember(String member) {
        this.m_members.add(member);
    }

    public void addMember(ServerPlayerEntity member) {
        this.m_members.add(member.getName().getString());
    }

    public void removeMember(String member) {
        this.m_members.remove(member);
    }

    public void removeMember(ServerPlayerEntity member) {
        this.m_members.remove(member.getName().getString());
    }

    public void refreshTaskItemList() {
        try {
            this.m_itemList = LitematicaReader.parseLitematicaFile(Paths.get(m_itemList.getFilePath()).toFile());
        }
        catch (Exception e) {
            LiteItemListFabric.LOGGER.error("failed to load litematic file: {}", m_itemList.getFilePath(), e);
        }
    }

    public void startAutoRefreshStorage(MinecraftServer server) {
        if (storageRefreshTimer != null) return;
        storageRefreshTimer = new Timer(true);
        storageRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                 server.execute(() -> m_itemList.calcAvailable(server, m_botManager));
            }
        }, 0, REFRESH_INTERVAL);
    }

    public void setTime(Long time) {
        this.m_time = time;
    }

    public void setBotManager(TaskStorageBotManager botManager) {
        this.m_botManager = botManager;
    }

    public void setItemList(TaskItemList itemList) {
        m_itemList = itemList;
    }

    public String getTaskCommandTag() {
        return "in_task_" + m_id;
    }

}
