package com.securitysystem.camera;

import java.util.logging.Logger;

import com.securitysystem.motion.DetectionConfig;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CameraManager {
    private static final Logger LOGGER = Logger.getLogger(CameraManager.class.getName());

    private VideoCapture videoCapture;

    public CameraManager() {
        openCamera();
    }

    private void openCamera() {
        videoCapture = new VideoCapture(DetectionConfig.CAMERA_INDEX);

        if(!videoCapture.isOpened()) {
            LOGGER.severe("Unable to open camera at index " + DetectionConfig.CAMERA_INDEX);
            throw new RuntimeException("Failed to initialize camera");
        }

        videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, DetectionConfig.CAMERA_WIDTH);
        videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, DetectionConfig.CAMERA_HEIGHT);
        videoCapture.set(Videoio.CAP_PROP_FPS, DetectionConfig.CAMERA_FPS);
    }

    public boolean isOpen() {
        return videoCapture != null && videoCapture.isOpened();
    }

    public VideoCapture getVideoCapture() {
        if(!isOpen()) {
            openCamera();
        }
        return videoCapture;
    }

    public void releaseCamera() {
        if (isOpen()) {
            videoCapture.release();
        }
    }


}
