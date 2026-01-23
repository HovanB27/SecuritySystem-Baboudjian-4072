package com.securitysystem.detection;

import com.securitysystem.motion.DetectionConfig;
import org.opencv.core.Rect;

public class DetectionResult {
    private final String objectType;
    private final Rect boundingBox;
    private final double confidenceScore;

    public DetectionResult(String objectType, Rect boundingBox, double confidenceScore) {
        this.objectType = objectType;
        this.boundingBox = boundingBox;
        this.confidenceScore = confidenceScore;
    }

    public String getObjectType() {
        return objectType;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public boolean isThreat() {
        return DetectionConfig.THREAT_OBJECTS.contains(objectType.toLowerCase());
    }


}
