package org.satou.gtecore.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.common.data.GTMaterials;
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

        public static Material DistilledPurifiedWater = new Material.Builder(GTECore.id("distilled_purified_water"))
                        .fluid()
                        .color(0x4A94FF)
                        .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                        .components(GTMaterials.Hydrogen, 2, GTMaterials.Oxygen, 1)
                        .buildAndRegister();

        public static Material UvPurifiedWater = new Material.Builder(GTECore.id("uv_purified_water"))
                        .fluid()
                        .color(0x7B68EE)
                        .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                        .components(GTMaterials.Hydrogen, 2, GTMaterials.Oxygen, 1)
                        .buildAndRegister();

        public static Material UltrapureWater = new Material.Builder(GTECore.id("ultrapure_water"))
                        .fluid()
                        .color(0x80D8FF)
                        .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                        .components(GTMaterials.Hydrogen, 2, GTMaterials.Oxygen, 1)
                        .buildAndRegister();
}