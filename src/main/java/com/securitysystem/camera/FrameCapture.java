package com.securitysystem.camera;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import java.util.Optional;

public class FrameCapture {
    private final CameraManager cameraManager;
    private final Mat matFrame;

    public FrameCapture(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
        this.matFrame = new Mat();
    }

    public Optional<Mat> captureFrame() {
        VideoCapture videoCapture = cameraManager.getVideoCapture();

        if (videoCapture != null && videoCapture.isOpened()) {
            if (videoCapture.read(matFrame)) {
                return Optional.of(matFrame);
            }
        }
        return Optional.empty();
    }

    public void release() {
        matFrame.release();
    }
}
