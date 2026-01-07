package org.satou.gtecore.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import org.satou.gtecore.GTECore;

public class GTEMaterials {
    public static void init() {
    }

    public static Material Huo = new Material.Builder(GTECore.id("huo"))
            .liquid(new FluidBuilder().textures(true, false).temperature(2000))
            .buildAndRegister();
    public static Material JinYuanSu = new Material.Builder(GTECore.id("jinyuansu"))
            .liquid(new FluidBuilder().textures(true, false).temperature(2000))
            .buildAndRegister();
    public static Material MuYuanSu = new Material.Builder(GTECore.id("muyuansu"))
            .liquid(new FluidBuilder().textures(true, false).temperature(2000))
            .buildAndRegister();
    public static Material ShuiYuanSu = new Material.Builder(GTECore.id("shuiyuansu"))
            .liquid(new FluidBuilder().textures(true, false).temperature(2000))
            .buildAndRegister();
    public static Material TuYuanSu = new Material.Builder(GTECore.id("tuyuansu"))
            .liquid(new FluidBuilder().textures(true, false).temperature(2000))
            .buildAndRegister();
}