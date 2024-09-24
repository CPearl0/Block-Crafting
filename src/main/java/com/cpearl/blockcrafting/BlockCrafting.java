package com.cpearl.blockcrafting;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BlockCrafting.MODID)
public class BlockCrafting
{
    public static final String MODID = "blockcrafting";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static void logInfo(String message){
        LOGGER.info(message);
    }
}
