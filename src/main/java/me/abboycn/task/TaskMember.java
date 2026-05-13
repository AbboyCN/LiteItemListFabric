package me.abboycn.task;


import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import me.abboycn.gui.MenuListStatus;

public class TaskMember {
    @SerializedName("name")
    private final String name;
    @SerializedName("menuListStatus")
    private MenuListStatus menuListStatus;

    public TaskMember(String name, MenuListStatus menuListStatus) {
        this.name = name;
        this.menuListStatus = menuListStatus;
    }

    public String getName() { return name; }

    public MenuListStatus getListStatus() { return menuListStatus; }

    public void setListFilter(MenuListStatus menuListStatus) { this.menuListStatus = menuListStatus; }
}
