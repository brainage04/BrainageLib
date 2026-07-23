package io.github.brainage04.brainagelib;

import io.github.brainage04.brainagelib.command.core.ModCommands;
import io.github.brainage04.brainagelib.help.FirstJoinHelpState;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrainageLib implements ModInitializer {
    public static final String MOD_ID = "brainagelib";
    public static final String MOD_NAME = "BrainageLib";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        ModCommands.initialize();
        FirstJoinHelpState.initialize();

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}
