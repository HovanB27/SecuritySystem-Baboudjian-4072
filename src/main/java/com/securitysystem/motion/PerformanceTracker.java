package com.securitysystem.motion;

import java.util.logging.Logger;

public class PerformanceTracker {
    private static final Logger LOGGER = Logger.getLogger(PerformanceTracker.class.getName());

    private int frameCount;
    private final long startTime;
    private long lastFrameTime;

    public PerformanceTracker() {
        this.frameCount = 0;
        this.startTime = System.currentTimeMillis();
        this.lastFrameTime = 0;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void startFrame() {
        lastFrameTime = System.currentTimeMillis();
        frameCount++;
    }

    public void endFrame() {
        long processingTime = System.currentTimeMillis() - lastFrameTime;
        if (processingTime > 100) {
            LOGGER.warning("Frame processing took " + processingTime + "ms - performance issue");
        }
    }

    public double getCurrentFPS() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        return frameCount / elapsedSeconds;
    }
}
