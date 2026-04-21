package me.abboycn.task;

import com.google.gson.annotations.SerializedName;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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

    public void addAmount(int n) { this.amount += n; }

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

    public List<Text> getItemInfo() {
        List<Text> ret = new ArrayList<>();
        ret.add(Text.literal(getItem().getName().getString()));
        ret.add(Text.literal(Formatting.GRAY + "数量: " + available + "/" + amount));
        ret.add(Text.literal((isHard?Formatting.RED+"困难 ":"")+ (isImpt?Formatting.GOLD+"重要 ":"")));
        ret.add(Text.literal(Formatting.GRAY+msg));
        ret.add(Text.literal(Formatting.GRAY+"参与者: "+principals.stream().reduce("", (s1, s2) -> s1 + "," + s2).trim()));
        return ret;
    }

    public void setPrincipals(Collection<String> principals) {
        this.principals = principals;
    }
}