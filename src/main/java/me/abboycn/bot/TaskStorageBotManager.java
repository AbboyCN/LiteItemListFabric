package me.abboycn.bot;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;

public class TaskStorageBotManager {
    @SerializedName("task")
    private String m_task;
    @SerializedName("bots")
    private Collection<StorageBot> storageBots = new ArrayList<>();
    @SerializedName("nextId")
    private int nextId = 0;

    public TaskStorageBotManager(String task){
        this.m_task = task;
    }

    public TaskStorageBotManager(String task, Collection<StorageBot> bots){
        this.m_task = task;
        this.storageBots = bots;
    }

    public String getTask() {return this.m_task;}

    public void setTask(String task) {this.m_task = task;}

    public StorageBot newBot(){
        StorageBot bot = new StorageBot(m_task, nextId++);
        storageBots.add(bot);
        return bot;
    }

    public StorageBot getBot(int id){
        return storageBots.stream().filter(b -> b.getId() == id).findFirst().orElse(null);
    }

    public StorageBot getBot(String name){
        return storageBots.stream().filter(b -> b.getName().equals(name)).findFirst().orElse(null);
    }

    public Collection<StorageBot> getBots(){return storageBots;}

    public boolean hasBot(){return !storageBots.isEmpty();}

    public void summonBot(int id, ServerPlayerEntity player){
        getBot(id).playerSummonFake(player);
    }

    public void summonAllBots(ServerPlayerEntity player){
        storageBots.forEach(bot -> bot.playerSummonFake(player));
    }
}
