package com.cpearl.blockcrafting.compat.kjs;

import com.cpearl.blockcrafting.multiblock.MultiblockStructure;

public class BlockCraftingKubeJSBindings {
    public void addMultiblockStructure(MultiblockStructure structure) {
        MultiblockStructure.addStructure(structure);
    }
}
