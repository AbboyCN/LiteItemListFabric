package me.abboycn.executor;

import me.abboycn.resource.LangProvider;

public class CMDConfigLanguage {
    public static int en_us(){
        LangProvider.setLang(LangProvider.Lang.en_us);
        return 1;
    }

    public static int zh_cn(){
        LangProvider.setLang(LangProvider.Lang.zh_cn);
        return 1;
    }
}
