package com.cpearl.blockcrafting.compat.kjs;

import com.cpearl.blockcrafting.multiblock.MultiblockStructure;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class BlockCraftingPlugin extends KubeJSPlugin {
    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("BlockCrafting", new BlockCraftingKubeJSBindings());
        event.add("MultiblockStructure", MultiblockStructure.class);
        event.add("MultiblockStructureBuilder", MultiblockStructure.StructureBuilder.class);
        event.add("MultiblockStructureFileBuilder", MultiblockStructure.StructureFileBuilder.class);
    }
}
