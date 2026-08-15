package me.abboycn.config;

import com.google.gson.annotations.SerializedName;
import me.abboycn.resource.LangProvider;

public class LiteItemListConfig {
    // 默认常量
    public static final LangProvider.Lang DEFAULT_LANG = LangProvider.Lang.zh_cn;
    public static final String DEFAULT_BOT_SUFFIX = "";

    @SerializedName(ConfigKey.LANGUAGE)
    private LangProvider.Lang language = DEFAULT_LANG;

    @SerializedName(ConfigKey.BOT_NAME_SUFFIX)
    private String botNameSuffix = DEFAULT_BOT_SUFFIX;

    // Getter & Setter
    public LangProvider.Lang getLanguage() {
        return language;
    }

    public void setLanguage(LangProvider.Lang language) {
        this.language = language;
    }

    public String getBotNameSuffix() {
        return botNameSuffix;
    }

    public void setBotNameSuffix(String botNameSuffix) {
        this.botNameSuffix = botNameSuffix;
    }

    // 一键重置全部默认值
    public void resetDefault() {
        language = DEFAULT_LANG;
        botNameSuffix = DEFAULT_BOT_SUFFIX;
    }
}