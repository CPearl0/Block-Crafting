package com.cpearl.blockcrafting.event;

import com.cpearl.blockcrafting.BlockCrafting;
import com.cpearl.blockcrafting.multiblock.MultiblockStructure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockCrafting.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var level = player.serverLevel();
            var pos = event.getPos();
            var block = level.getBlockState(pos).getBlock();
            if (!MultiblockStructure.STRUCTURES.containsKey(block))
                return;
            for (var structure : MultiblockStructure.STRUCTURES.get(block)) {
                if (!structure.getCraftingItem().test(event.getItemStack().getItem()))
                    continue;
                if (structure.finish(level, pos, structure.finishedDirection(level, pos)))
                    break;
            }
        }
    }
}
