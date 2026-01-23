package com.securitysystem.motion;

import java.util.ArrayList;
import java.util.Arrays;

public class DetectionConfig {

    // ===== MOTION DETECTION =====
    public static final int MOG2_HISTORY = 700;
    public static final double VARIANCE_THRESHOLD = 30.0;
    public static final boolean SHADOW_DETECTION = false;
    public static final double MIN_CONTOUR_AREA = 5000.0;

    // ===== YOLO OBJECT DETECTION =====
    public static final int YOLO_INPUT_SIZE = 416;
    public static final float CONFIDENCE_THRESHOLD = 0.5f;
    public static final float NMS_THRESHOLD = 0.7f;
    public static final int DETECTION_FRAME_SKIP = 5;

    // ===== THREAT DETECTION =====
    public static final ArrayList<String> THREAT_OBJECTS = new ArrayList<>(Arrays.asList("knife", "scissors"));
    public static final long THREAT_TIMEOUT_MS = 0;
    public static final double THREAT_MATCHING_DISTANCE = 100.0;

    // ===== CAMERA =====
    public static final int CAMERA_WIDTH = 640;
    public static final int CAMERA_HEIGHT = 480;
    public static final int CAMERA_FPS = 30;
    public static final int CAMERA_INDEX = 0;

    // ===== DISPLAY =====
    public static final int FLASH_CYCLE_FRAMES = 20;

    private DetectionConfig() {
    }
}
