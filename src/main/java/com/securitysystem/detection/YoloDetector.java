package com.securitysystem.detection;

import com.securitysystem.motion.DetectionConfig;
import org.opencv.core.*;
import org.opencv.dnn.Net;
import org.opencv.dnn.Dnn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class YoloDetector {
    private Net yoloNetModel;
    private final ArrayList<String> classNames;
    private final ArrayList<String> outputLayerNames;

    public YoloDetector() throws IOException {
        this.classNames = new ArrayList<>();
        this.outputLayerNames = new ArrayList<>();

        loadYoloModel();
        loadClassNames();
        getOutputLayerNames();
    }

    private void loadClassNames() throws IOException {
        String projectRoot = System.getProperty("user.dir");
        String filePath = projectRoot + "/src/main/yolomodels/coco.names";

        List<String> classNamesList = Files.readAllLines(Paths.get(filePath));
        classNames.addAll(classNamesList);
    }

    private void loadYoloModel() throws IOException {
        String projectRoot = System.getProperty("user.dir");
        String cfgPath = projectRoot + "/src/main/yolomodels/yolov4.cfg";
        String weightsPath = projectRoot + "/src/main/yolomodels/yolov4.weights";

        if (!Files.exists(Paths.get(cfgPath)) || !Files.exists(Paths.get(weightsPath))) {
            throw new FileNotFoundException("YOLO model files not found at: " + cfgPath);
        }

        yoloNetModel = Dnn.readNetFromDarknet(cfgPath, weightsPath);

        if (yoloNetModel.empty()) {
            throw new IOException("Failed to load YOLO model. Check if files are corrupted.");
        }
    }

    private void getOutputLayerNames() {
        List<String> layerNames = yoloNetModel.getLayerNames();

        MatOfInt outLayers = yoloNetModel.getUnconnectedOutLayers();

        int[] outLayerIndices = outLayers.toArray();

        for (int idx : outLayerIndices) {
            outputLayerNames.add(layerNames.get(idx - 1));
        }
    }

    public List<DetectionResult> detectObjects(Mat matFrame) {
        ArrayList<DetectionResult> listOfDetectionResults = new ArrayList<>();
        Mat blob = Dnn.blobFromImage(matFrame, 1.0/255.0, new Size(DetectionConfig.YOLO_INPUT_SIZE, DetectionConfig.YOLO_INPUT_SIZE), new Scalar(0,0,0), true, false);

        yoloNetModel.setInput(blob);

        List<Mat> resultMats = new ArrayList<>();
        yoloNetModel.forward(resultMats, outputLayerNames);

        List<Rect2d> boxes = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Integer> classIds = new ArrayList<>();

        for (Mat results: resultMats) {
            for (int i = 0; i < results.rows(); i++) {
                Mat row = results.row(i);
                Mat scores = row.colRange(5, results.cols());

                Core.MinMaxLocResult minMaxResult = Core.minMaxLoc(scores);
                double confidence = minMaxResult.maxVal;
                Point classIdPoint = minMaxResult.maxLoc;
                int classId = (int) classIdPoint.x;

                if (confidence > 0.5) {
                    double centerX = row.get(0, 0)[0] * matFrame.cols();
                    double centerY = row.get(0, 1)[0] * matFrame.rows();
                    double width = row.get(0, 2)[0] * matFrame.cols();
                    double height = row.get(0, 3)[0] * matFrame.rows();

                    double x = centerX - width / 2;
                    double y = centerY - height / 2;

                    boxes.add(new Rect2d(x, y, width, height));
                    confidences.add((float) confidence);
                    classIds.add(classId);
                }
            }
        }

        MatOfRect2d boxesMat = new MatOfRect2d();
        boxesMat.fromList(boxes);

        MatOfFloat confidencesMat = new MatOfFloat();
        confidencesMat.fromList(confidences);

        MatOfInt indices = new MatOfInt();
        Dnn.NMSBoxes(boxesMat, confidencesMat, DetectionConfig.CONFIDENCE_THRESHOLD, DetectionConfig.NMS_THRESHOLD, indices);

        int[] indicesArray = indices.toArray();
        for (int idx : indicesArray) {
            Rect2d box2d = boxes.get(idx);
            Rect box = new Rect((int)box2d.x, (int)box2d.y, (int)box2d.width, (int)box2d.height);
            float confidence = confidences.get(idx);
            int classId = classIds.get(idx);
            String className = classNames.get(classId);

            listOfDetectionResults.add(new DetectionResult(className, box, confidence));
        }

        return listOfDetectionResults;
    }


}
