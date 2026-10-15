package com.litematicahub;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInit implements ModInitializer {
    public static final String MOD_ID = "litematicahub";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Litematica Online Hub");
        SchematicDownloadManager.initCacheDirectory();
    }
}
