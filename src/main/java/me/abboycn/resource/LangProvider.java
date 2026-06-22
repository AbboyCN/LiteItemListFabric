package me.abboycn.resource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.abboycn.LiteItemListFabric;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LangProvider {
    private static final Gson GSON = new Gson();
    private static Map<String, String> currentLang;

    // 默认语言为英文
    private static Lang lang = Lang.zh_cn;

    public enum Lang {
        zh_cn,
        en_us
    }

    public static void loadLanguage() {
        try {
            InputStream is = LangProvider.class.getResourceAsStream("/assets/" + LiteItemListFabric.MOD_ID + "/lang/" + lang + ".json");
            if (is == null) return;

            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            currentLang = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
        } catch (Exception e) {
            LiteItemListFabric.LOGGER.error("Error loading language file!", e);
        }
    }

    public static Text get(String key, Object... args) {
        if (currentLang == null) {
            LiteItemListFabric.LOGGER.warn("Failed to get translation map for key {}", key);
            return Text.literal(key);
        }
        String text = currentLang.get(key);
        if (text == null) {
            LiteItemListFabric.LOGGER.warn("Failed to match translation key:{}", key);
            return Text.literal(key);
        }
        try{
            text = String.format(text, args);
        }
        catch (Exception e){
            LiteItemListFabric.LOGGER.warn("Failed to format translation key:{}, caused by:", key, e);
            return Text.literal(key);
        }
        return Text.literal(text);
    }

    public static void setLang(Lang newLang) {
        lang = newLang;
        loadLanguage();
    }
}
