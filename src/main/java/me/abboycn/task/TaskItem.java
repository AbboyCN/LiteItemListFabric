package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashSet;

public class TaskItem {
    @SerializedName("itemId")
    private String itemId;
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

    public void addAvailable(int amount) { this.available += amount; }

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

    public boolean isFinished() {return available>amount;}

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Collection<String> getPrincipals() {
        return principals;
    }

    public Text getItemInfo() {
        return Text.literal(Formatting.GRAY + getItem().getName().getString())
                .append(Text.literal(Formatting.GRAY + "数量: " + available + "/" + amount))
                .append(Text.literal(isHard?Formatting.RED+"困难 ":Formatting.GOLD+"重要 "))
                .append(Text.literal(Formatting.GRAY+msg))
                .append(Text.literal(Formatting.GRAY+principals.stream().reduce("", (s1, s2) -> s1 + "," + s2).trim()));
    }

    public void setPrincipals(Collection<String> principals) {
        this.principals = principals;
    }
}