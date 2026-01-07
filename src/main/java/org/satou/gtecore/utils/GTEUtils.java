package org.satou.gtecore.utils;

import com.gregtechceu.bettergtae.utils.RegistriesUtil;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import static com.gregtechceu.gtceu.api.pattern.Predicates.blocks;

public class GTEUtils {
    public static TraceabilityPredicate easy(String id){
        return blocks(RegistriesUtil.getBlock(id));
    }
}
