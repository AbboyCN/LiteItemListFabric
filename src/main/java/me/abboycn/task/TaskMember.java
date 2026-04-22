package me.abboycn.task;


import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import me.abboycn.gui.MenuListStatus;

public class TaskMember {
    @SerializedName("name")
    private String name;
    @SerializedName("menuListStatus")
    private MenuListStatus menuListStatus;

    public TaskMember(){}

    public TaskMember(String name, MenuListStatus menuListStatus) {
        this.name = name;
        this.menuListStatus = menuListStatus;
    }

    public String getName() { return name; }

    public MenuListStatus getListFilter() { return menuListStatus; }

    public void setName(String name) { this.name = name; }

    public void setListFilter(MenuListStatus menuListStatus) { this.menuListStatus = menuListStatus; }
}
