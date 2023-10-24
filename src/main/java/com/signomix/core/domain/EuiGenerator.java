package com.signomix.core.domain;

import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

@ApplicationScoped
public class EuiGenerator {
    @Inject
    Logger logger;

    private AtomicLong euiSeed = new AtomicLong(System.currentTimeMillis());

    public String createEui(String prefix) {
        long euiNumber = euiSeed.addAndGet(2L);
        HexFormat formatFingerprint = HexFormat.of();
        if (prefix != null && !prefix.isEmpty()) {
            prefix = prefix.trim();
        }
        String eui = prefix + formatFingerprint.toHexDigits(euiNumber);
        logger.info("Creating device EUI: " + euiNumber + " -> " + eui);
        return eui;
    }

}
