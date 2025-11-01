
package org.satou.gtecore.datagen;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.satou.gtecore.GTECore;

import java.util.HashMap;
import java.util.Map;

public class CommonLanguageProvider extends LanguageProvider {

    private final Map<String, Map<String, String>> translations = new HashMap<>();

    private String locale;

    public CommonLanguageProvider(DataGenerator gen, String locale) {
        super(gen.getPackOutput(), GTECore.MOD_ID, locale);
        this.locale = locale;
        initializeTranslations();
    }

    @Override
    protected void addTranslations() {
        translations.forEach((key, langMap) -> {
            String translation = langMap.get(locale);
            if (translation != null) {
                add(key, translation);
            }
        });
    }

    private void initializeTranslations() {
        Lang.init(this);
    }

    public void add(String key, String en, String zh) {
        Map<String, String> langMap = new HashMap<>();
        langMap.put("en_us", en);
        langMap.put("zh_cn", zh);
        translations.put(key, langMap);
    }
}
