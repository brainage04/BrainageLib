package io.github.brainage04.brainagelib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BrainageLib {
    public static final String MOD_ID = "brainagelib";
    public static final String MOD_NAME = "BrainageLib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private BrainageLib() {}

    public static void initialize() {
        LOGGER.info("{} initialising...", MOD_NAME);
        LOGGER.info("{} initialised.", MOD_NAME);
    }
}
