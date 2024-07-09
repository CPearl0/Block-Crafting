package com.cpearl.blockcrafting.compat.kjs;

import com.cpearl.blockcrafting.multiblock.MultiblockStructure;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class BlockCraftingPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        KubeJSPlugin.super.registerBindings(bindings);
        bindings.add("BlockCrafting", new BlockCraftingKubeJSBindings());
        bindings.add("MultiblockStructure", MultiblockStructure.class);
        bindings.add("MultiblockStructureBuilder", MultiblockStructure.StructureBuilder.class);
        bindings.add("MultiblockStructureFileBuilder", MultiblockStructure.StructureFileBuilder.class);
    }
}
