package me.abboycn.gui;

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;

public class MenuListStatus {
    @SerializedName("filterTypeClime")
    public FilterType_Clime filterTypeClime;
    @SerializedName("filterTypeFinished")
    public FilterType_Finished filterTypeFinished;
    @SerializedName("filterTypeMark")
    public FilterType_Mark filterTypeMark;
    @SerializedName("page")
    public int page;

    public enum FilterType_Clime{
        DEFAULT,
        CLIMED,
        UNCLIMED
    }

    public enum FilterType_Finished{
        DEFAULT,
        UNFINISHED,
        PROCESSING,
        NOTSTART,
        FINISHED
    }

    public enum FilterType_Mark {
        DEFAULT,
        IMPTORHARD,
        IMPT,
        HARD
    }

    public MenuListStatus(FilterType_Clime filterTypeClime, FilterType_Finished filterTypeFinished, FilterType_Mark filterTypeMark, int page) {
        this.filterTypeClime = filterTypeClime;
        this.filterTypeFinished = filterTypeFinished;
        this.filterTypeMark = filterTypeMark;
        this.page = page;
    }

}
