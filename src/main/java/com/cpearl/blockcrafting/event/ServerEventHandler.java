package com.cpearl.blockcrafting.event;

import com.cpearl.blockcrafting.BlockCrafting;
import com.cpearl.blockcrafting.multiblock.MultiblockStructure;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

@EventBusSubscriber(modid = BlockCrafting.MODID)
public class ServerEventHandler {
    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            var level = player.serverLevel();
            var pos = event.getPos();
            var block = level.getBlockState(pos).getBlock();
            if (!MultiblockStructure.STRUCTURES.containsKey(block))
                return;
            for (var structure : MultiblockStructure.STRUCTURES.get(block)) {
                if (!structure.getCraftingItem().test(event.getItemStack().getItem()))
                    continue;
                if (structure.finish(level, pos, player, structure.finishedDirection(level, pos)))
                    break;
            }
        }
    }
}
