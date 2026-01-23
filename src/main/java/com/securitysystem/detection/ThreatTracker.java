package com.securitysystem.detection;

import com.securitysystem.motion.DetectionConfig;
import org.opencv.core.Rect;
import java.util.ArrayList;
import java.util.List;

public class ThreatTracker {

    private static class ThreatInfo {
        private Rect boundingBox;
        private final String objectType;
        private final double confidence;
        private long lastSeenTime;

        public ThreatInfo(Rect boundingBox, String objectType, double confidence, long lastSeenTime) {
            this.boundingBox = boundingBox;
            this.objectType = objectType;
            this.confidence = confidence;
            this.lastSeenTime = lastSeenTime;
        }

        public Rect getBoundingBox() {
            return boundingBox;
        }

        public String getObjectType() {
            return objectType;
        }

        public double getConfidence() {
            return confidence;
        }

        public long getLastSeenTime() {
            return lastSeenTime;
        }

        public void updateTimestamp(long newTime) {
            this.lastSeenTime = newTime;
        }

        public void updatePosition(Rect newBox) {
            this.boundingBox = newBox;
        }
    }

    private final List<ThreatInfo> activeThreats;
    private final long timeoutMs;

    public ThreatTracker(long timeoutMs) {
        this.activeThreats = new ArrayList<>();
        this.timeoutMs = timeoutMs;
    }

    public ThreatTracker() {
        this(DetectionConfig.THREAT_TIMEOUT_MS);
    }

    public void updateThreats(List<DetectionResult> newDetections) {
        long currentTime = System.currentTimeMillis();

        for (DetectionResult detection : newDetections) {
            if (detection.isThreat()) {
                ThreatInfo existingThreat = findMatchingThreat(detection);

                if (existingThreat != null) {
                    existingThreat.updateTimestamp(currentTime);
                    existingThreat.updatePosition(detection.getBoundingBox());
                } else {
                    ThreatInfo newThreat = new ThreatInfo(
                            detection.getBoundingBox(),
                            detection.getObjectType(),
                            detection.getConfidenceScore(),
                            currentTime
                    );
                    activeThreats.add(newThreat);
                }
            }
        }

        cleanupOldThreats(currentTime);
    }

    private ThreatInfo findMatchingThreat(DetectionResult detection) {
        Rect newBox = detection.getBoundingBox();
        double newCenterX = newBox.x + newBox.width / 2.0;
        double newCenterY = newBox.y + newBox.height / 2.0;

        for (ThreatInfo threat : activeThreats) {
            if (!threat.getObjectType().equals(detection.getObjectType())) {
                continue;
            }
            Rect existingBox = threat.getBoundingBox();
            double existingCenterX = existingBox.x + existingBox.width / 2.0;
            double existingCenterY = existingBox.y + existingBox.height / 2.0;

            double distance = Math.sqrt(
                    Math.pow(newCenterX - existingCenterX, 2) +
                            Math.pow(newCenterY - existingCenterY, 2)
            );

            if (distance < DetectionConfig.THREAT_MATCHING_DISTANCE) {
                return threat;
            }
        }

        return null;
    }

    public List<DetectionResult> getActiveThreats() {
        long currentTime = System.currentTimeMillis();
        List<DetectionResult> results = new ArrayList<>();

        for (ThreatInfo threat : activeThreats) {
            if ((currentTime - threat.getLastSeenTime()) < timeoutMs) {
                DetectionResult result = new DetectionResult(
                        threat.getObjectType(),
                        threat.getBoundingBox(),
                        threat.getConfidence()
                );
                results.add(result);
            }
        }

        return results;
    }

    private void cleanupOldThreats(long currentTime) {
        activeThreats.removeIf(threat -> (currentTime - threat.getLastSeenTime()) >= timeoutMs);
    }

    public void clearAllThreats() {
        activeThreats.clear();
    }
}