package com.securitysystem.core;

import com.securitysystem.detection.DetectionResult;
import com.securitysystem.detection.ThreatTracker;
import com.securitysystem.detection.YoloDetector;
import com.securitysystem.motion.DetectionConfig;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import com.securitysystem.camera.CameraManager;
import com.securitysystem.camera.FrameCapture;
import com.securitysystem.camera.VideoDisplay;
import com.securitysystem.motion.MotionDetector;
import com.securitysystem.motion.PerformanceTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class SecuritySystem {
    private static final Logger LOGGER = Logger.getLogger(SecuritySystem.class.getName());
    private static final int FRAME_SLEEP_MS = 10;
    private static final int FPS_LOG_INTERVAL = 30;

    private List<DetectionResult> lastDetections = new ArrayList<>();
    private int frameCounter = 0;

    private final ThreatTracker threatTracker;
    private final YoloDetector yoloDetector;
    private final CameraManager cameraManager;
    private final FrameCapture frameCapture;
    private final MotionDetector motionDetector;
    private final PerformanceTracker performanceTracker;
    private final VideoDisplay videoDisplay;
    private volatile boolean running;

    public SecuritySystem() {
        this.threatTracker = new ThreatTracker();
        this.cameraManager = new CameraManager();
        this.frameCapture = new FrameCapture(cameraManager);
        this.motionDetector = new MotionDetector();
        this.performanceTracker = new PerformanceTracker();
        this.videoDisplay = new VideoDisplay();

        try {
            this.yoloDetector = new YoloDetector();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize YoloDetector: " + e.getMessage(), e);
        }

        this.running = false;
    }

    public void start() {
        running = true;
        LOGGER.info("Security system starting...");

        if (!cameraManager.isOpen()) {
            LOGGER.severe("Camera failed to open. Exiting.");
            throw new RuntimeException("Camera initialization failed");
        }

        run();
    }

    private void run() {
        while (running) {
            performanceTracker.startFrame();

            Optional<Mat> frameOpt = frameCapture.captureFrame();

            if (frameOpt.isPresent()) {
                Mat frame = frameOpt.get();
                frameCounter++;

                if (frameCounter % DetectionConfig.DETECTION_FRAME_SKIP == 0) {
                    lastDetections = yoloDetector.detectObjects(frame);
                    threatTracker.updateThreats(lastDetections);

                    long threatCount = lastDetections.stream().filter(DetectionResult::isThreat).count();
                    if (threatCount > 0) {
                        LOGGER.warning("THREAT DETECTED! " + threatCount + " threat(s) found:");
                        lastDetections.stream()
                                .filter(DetectionResult::isThreat)
                                .forEach(d -> LOGGER.warning("  - " + d.getObjectType() +
                                        " (confidence: " + String.format("%.2f", d.getConfidenceScore()) + ")"));
                    }
                }

                List<DetectionResult> allThreats = threatTracker.getActiveThreats();
                List<DetectionResult> allToDisplay = new ArrayList<>(lastDetections);

                for (DetectionResult threat : allThreats) {
                    if (!isDuplicateDetection(threat, lastDetections)) {
                        allToDisplay.add(threat);
                    }
                }

                if (!allToDisplay.isEmpty()) {
                    videoDisplay.showFrameWithDetections(frame, allToDisplay);
                } else {
                    videoDisplay.showFrame(frame);
                }

                performanceTracker.endFrame();

                if (performanceTracker.getFrameCount() % FPS_LOG_INTERVAL == 0) {
                    LOGGER.info(String.format("Current FPS: %.2f", performanceTracker.getCurrentFPS()));
                }
            } else {
                LOGGER.warning("Failed to capture frame - camera may be disconnected");
            }

            try {
                Thread.sleep(FRAME_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Main loop interrupted - shutting down");
                break;
            }
        }
    }

    private boolean isDuplicateDetection(DetectionResult detection, List<DetectionResult> detectionList) {
        for (DetectionResult existing : detectionList) {
            if (detection.getObjectType().equals(existing.getObjectType()) &&
                    isNearby(detection.getBoundingBox(), existing.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearby(org.opencv.core.Rect box1, org.opencv.core.Rect box2) {
        return Math.abs(box1.x - box2.x) < DetectionConfig.THREAT_MATCHING_DISTANCE &&
                Math.abs(box1.y - box2.y) < DetectionConfig.THREAT_MATCHING_DISTANCE;
    }

    public void stop() {
        running = false;
        LOGGER.info("Security system stopping...");
    }

    public void shutdown() {
        stop();
        videoDisplay.close();
        cameraManager.releaseCamera();
        frameCapture.release();
        motionDetector.release();
        threatTracker.clearAllThreats();
        LOGGER.info("Security system shutdown complete");
    }

    public static void main(String[] args) {
        try {
            nu.pattern.OpenCV.loadLocally();
            LOGGER.info("OpenCV loaded successfully: " + Core.VERSION);
        } catch (Exception e) {
            LOGGER.severe("Failed to load OpenCV library: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        SecuritySystem system = new SecuritySystem();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            system.shutdown();
        }));

        try {
            system.start();
        } catch (Exception e) {
            LOGGER.severe("System error: " + e.getMessage());
            e.printStackTrace();
            system.shutdown();
        }
    }
}