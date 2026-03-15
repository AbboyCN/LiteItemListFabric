package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashSet;

public class TaskItem {
    @SerializedName("itemId")
    private String itemId; // 存储Item的Identifier字符串，用于序列化
    @SerializedName("amount")
    private int amount;
    @SerializedName("available")
    private int available;
    @SerializedName("isHard")
    private boolean isHard;
    @SerializedName("isImpt")
    private boolean isImpt;
    @SerializedName("msg")
    private String msg;
    @SerializedName("principals")
    private Collection<String> principals;

    // 空构造器（GSON反序列化需要）
    public TaskItem() {}

    public TaskItem(Item item, int amount) {
        this.itemId = Registries.ITEM.getId(item).toString();
        this.amount = amount;
        this.available = 0;
        this.isHard = false;
        this.isImpt = false;
        this.msg = "";
        this.principals = new HashSet<>();
    }

    // Getter & Setter
    public Item getItem() {
        return Registries.ITEM.get(Identifier.of(itemId));
    }

    public void setItem(Item item) {
        this.itemId = Registries.ITEM.getId(item).toString();
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public boolean isHard() {
        return isHard;
    }

    public void setHard(boolean hard) {
        isHard = hard;
    }

    public boolean isImpt() {
        return isImpt;
    }

    public void setImpt(boolean impt) {
        isImpt = impt;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Collection<String> getPrincipals() {
        return principals;
    }

    public void setPrincipals(Collection<String> principals) {
        this.principals = principals;
    }
}