package com.securitysystem.motion;

import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;

import java.util.ArrayList;

public class MotionDetector {

    private final BackgroundSubtractorMOG2 backgroundSubtractor;
    private final Mat foregroundMaskMat;
    private final Mat cleanedUpMat;

    public MotionDetector() {
        backgroundSubtractor = Video.createBackgroundSubtractorMOG2(
                DetectionConfig.MOG2_HISTORY,
                DetectionConfig.VARIANCE_THRESHOLD,
                DetectionConfig.SHADOW_DETECTION
        );
        foregroundMaskMat = new Mat();
        cleanedUpMat = new Mat();
    }

    public boolean detectMotion(Mat frameMat) {
        ArrayList<MatOfPoint> contours = getMotionContours(frameMat);
        return !contours.isEmpty();
    }

    public ArrayList<MatOfPoint> getMotionContours(Mat frameMat) {
        ArrayList<MatOfPoint> allContours = new ArrayList<>();
        ArrayList<MatOfPoint> motionDetectedContours = new ArrayList<>();

        if(frameMat.empty()) {
            return motionDetectedContours;
        }

        backgroundSubtractor.apply(frameMat, foregroundMaskMat);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3));
        Imgproc.erode(foregroundMaskMat, cleanedUpMat, kernel);
        Imgproc.dilate(cleanedUpMat, cleanedUpMat, kernel);
        Imgproc.findContours(cleanedUpMat, allContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour: allContours) {
            if (Imgproc.contourArea(contour) >= DetectionConfig.MIN_CONTOUR_AREA) {
                motionDetectedContours.add(contour);
            }
        }

        kernel.release();

        return motionDetectedContours;
    }

    public void release() {
        if (foregroundMaskMat != null) {
            foregroundMaskMat.release();
        }
        if (cleanedUpMat != null) {
            cleanedUpMat.release();
        }
    }
}
