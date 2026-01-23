package com.securitysystem.camera;

import com.securitysystem.detection.DetectionResult;
import com.securitysystem.motion.DetectionConfig;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import java.awt.*;
import org.opencv.core.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

public class VideoDisplay {
    private final JFrame frame;
    private final JLabel imageLabel;
    private int counter = 0;

    public VideoDisplay() {
        frame = new JFrame("Security Camera Feed");
        imageLabel = new JLabel();

        frame.setLayout(new BorderLayout());
        frame.add(imageLabel, BorderLayout.CENTER);
        frame.setSize(DetectionConfig.CAMERA_WIDTH, DetectionConfig.CAMERA_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void showFrame(Mat matFrame) {
        if(matFrame.empty()) {
            return;
        }
        BufferedImage image = matToBufferedImage(matFrame);
        ImageIcon imageIcon = new ImageIcon(image);
        imageLabel.setIcon(imageIcon);
        frame.repaint();
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        BufferedImage image;
        if (channels == 3) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }

        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    public void showFrameWithDetections(Mat matFrame, List<DetectionResult> detections) {
        if(matFrame == null || matFrame.empty()) {
            return;
        }

        counter++;

        Mat displayFrame = matFrame.clone();

        Scalar color;

        for (DetectionResult detection: detections) {
            Rect rect = detection.getBoundingBox();
            String label = detection.getObjectType() + " " + String.format("%.2f", detection.getConfidenceScore());
            Point point = new Point(rect.x, Math.max(rect.y - 10, 15));

            if(detection.isThreat()) {
                if(counter % DetectionConfig.FLASH_CYCLE_FRAMES < DetectionConfig.FLASH_CYCLE_FRAMES / 2) {
                    color = new Scalar(0, 0, 255);
                } else {
                    color = new Scalar(255, 255, 255);
                }
            } else {
                color = new Scalar(0, 255, 0);
            }

            Imgproc.rectangle(displayFrame, rect.tl(), rect.br(), color, 2);
            Imgproc.putText(displayFrame, label, point, Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
        }

        showFrame(displayFrame);
    }

    public void close() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            frame.dispose();
        });
    }
}
