package com.alpr;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PlateDetector - License Plate Detection using Edge Detection and Contour Analysis
 *
 * @author ALPR Academic Project
 * @version 1.3 - Added tunable parameters support
 */
public class PlateDetector {

    // Default values for tunable parameters
    private static final int DEFAULT_BLUR_KERNEL = 11;
    private static final int DEFAULT_CANNY_THRESHOLD1 = 30;
    private static final int DEFAULT_CANNY_THRESHOLD2 = 200;
    private static final double DEFAULT_MIN_ASPECT_RATIO = 2.5;
    private static final double DEFAULT_MAX_ASPECT_RATIO = 6.0;

    // Instance tunable parameters
    private int blurKernel = DEFAULT_BLUR_KERNEL;
    private int cannyThreshold1 = DEFAULT_CANNY_THRESHOLD1;
    private int cannyThreshold2 = DEFAULT_CANNY_THRESHOLD2;
    private double minAspectRatio = DEFAULT_MIN_ASPECT_RATIO;
    private double maxAspectRatio = DEFAULT_MAX_ASPECT_RATIO;

    private static final double MIN_PLATE_AREA_RATIO = 0.005;
    private static final double MAX_PLATE_AREA_RATIO = 0.15;

    private Mat originalImage;
    private Rect lastDetectedRect;
    private String currentImageName;

    // Intermediate results for GUI preview
    private Mat lastGrayImage;
    private Mat lastFilteredImage;
    private Mat lastEdgeImage;
    private Mat lastDilatedImage;
    private Mat lastContourImage;

    private static final String DEBUG_BASE_DIR = "debug_output";
    private static final String[] DEBUG_DIRS = {
        "step1_grayscale", "step2_filtered", "step3_canny",
        "step3b_dilated", "step4_detected_plate", "step5_ocr_preprocessed"
    };

    static {
        createDebugDirectories();
    }

    private static void createDebugDirectories() {
        File baseDir = new File(DEBUG_BASE_DIR);
        if (!baseDir.exists()) baseDir.mkdirs();
        for (String dir : DEBUG_DIRS) {
            File subDir = new File(DEBUG_BASE_DIR, dir);
            if (!subDir.exists()) subDir.mkdirs();
        }
    }

    private void saveDebugImage(String stepDir, Mat image, String imageName) {
        if (image == null || image.empty()) return;
        String filename = DEBUG_BASE_DIR + "/" + stepDir + "/" + imageName + ".jpg";
        Imgcodecs.imwrite(filename, image);
    }

    // ==================== PARAMETER SETTERS ====================

    public void setBlurKernel(int size) {
        this.blurKernel = (size % 2 == 0) ? size + 1 : size;
    }

    public void setCannyThreshold1(int threshold) {
        this.cannyThreshold1 = threshold;
    }

    public void setCannyThreshold2(int threshold) {
        this.cannyThreshold2 = threshold;
    }

    public void setMinAspectRatio(double ratio) {
        this.minAspectRatio = ratio;
    }

    public void setMaxAspectRatio(double ratio) {
        this.maxAspectRatio = ratio;
    }

    // ==================== PARAMETER GETTERS ====================

    public int getBlurKernel() { return blurKernel; }
    public int getCannyThreshold1() { return cannyThreshold1; }
    public int getCannyThreshold2() { return cannyThreshold2; }
    public double getMinAspectRatio() { return minAspectRatio; }
    public double getMaxAspectRatio() { return maxAspectRatio; }

    // ==================== INTERMEDIATE IMAGE GETTERS ====================

    public Mat getLastGrayImage() { return lastGrayImage; }
    public Mat getLastFilteredImage() { return lastFilteredImage; }
    public Mat getLastEdgeImage() { return lastEdgeImage; }
    public Mat getLastDilatedImage() { return lastDilatedImage; }
    public Mat getLastContourImage() { return lastContourImage; }
    public Mat getOriginalImage() { return originalImage; }
    public String getCurrentImageName() { return currentImageName; }
    public Rect getLastDetectedRect() { return lastDetectedRect; }
    public static String getDebugBaseDir() { return DEBUG_BASE_DIR; }

    // ==================== IMAGE LOADING ====================

    public boolean loadImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }
        File imageFile = new File(imagePath);
        currentImageName = imageFile.getName().replaceAll("\\.[^.]+$", "");
        originalImage = Imgcodecs.imread(imagePath);
        return originalImage != null && !originalImage.empty();
    }

    // ==================== PREPROCESSING ====================

    public Mat preprocess() {
        if (originalImage == null || originalImage.empty()) {
            return null;
        }

        // Step 1: Grayscale
        lastGrayImage = new Mat();
        Imgproc.cvtColor(originalImage, lastGrayImage, Imgproc.COLOR_BGR2GRAY);

        // Step 2: Bilateral filter
        lastFilteredImage = new Mat();
        Imgproc.bilateralFilter(lastGrayImage, lastFilteredImage, blurKernel, 17, 17);

        // Step 3: Canny edge detection
        lastEdgeImage = new Mat();
        Imgproc.Canny(lastFilteredImage, lastEdgeImage, cannyThreshold1, cannyThreshold2);

        // Step 4: Dilate
        lastDilatedImage = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(lastEdgeImage, lastDilatedImage, kernel, new Point(-1, -1), 1);

        return lastDilatedImage;
    }

    public Mat preprocessImageWithOriginal(String imagePath) {
        if (!loadImage(imagePath)) {
            System.err.println("[ERROR] Could not load image: " + imagePath);
            return null;
        }
        System.out.println("[INFO] Image loaded: " + imagePath);
        System.out.println("[INFO] Dimensions: " + originalImage.cols() + "x" + originalImage.rows());

        Mat result = preprocess();

        // Save debug images
        if (currentImageName != null) {
            saveDebugImage("step1_grayscale", lastGrayImage, currentImageName);
            saveDebugImage("step2_filtered", lastFilteredImage, currentImageName);
            saveDebugImage("step3_canny", lastEdgeImage, currentImageName);
            saveDebugImage("step3b_dilated", lastDilatedImage, currentImageName);
        }

        System.out.println("[INFO] Preprocessing complete.");
        return result;
    }

    // ==================== PLATE DETECTION ====================

    public Mat findPlate() {
        if (lastDilatedImage == null || originalImage == null) {
            return null;
        }
        return findPlateContour(lastDilatedImage, originalImage);
    }

    public Mat findPlateContour(Mat edgeImage, Mat original) {
        if (edgeImage == null || edgeImage.empty() || original == null || original.empty()) {
            return null;
        }

        double imageArea = edgeImage.rows() * edgeImage.cols();
        double minArea = imageArea * MIN_PLATE_AREA_RATIO;
        double maxArea = imageArea * MAX_PLATE_AREA_RATIO;

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeImage.clone(), contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println("[INFO] Found " + contours.size() + " contours");

        // Create contour visualization
        lastContourImage = original.clone();

        contours.sort((c1, c2) -> Double.compare(
                Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

        int checked = 0;

        for (int i = 0; i < Math.min(30, contours.size()); i++) {
            MatOfPoint contour = contours.get(i);
            double area = Imgproc.contourArea(contour);

            if (area < minArea || area > maxArea) continue;

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true);

            if (approx.toArray().length == 4) {
                checked++;
                Rect rect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) rect.width / rect.height;

                // Draw all candidates in yellow
                Imgproc.rectangle(lastContourImage, rect, new Scalar(0, 255, 255), 1);

                System.out.println("[DEBUG] Candidate #" + checked +
                        ": Area=" + (int) area +
                        ", AR=" + String.format("%.2f", aspectRatio) +
                        ", Size=" + rect.width + "x" + rect.height);

                if (aspectRatio >= minAspectRatio && aspectRatio <= maxAspectRatio) {
                    System.out.println("[SUCCESS] Plate found!");

                    int pad = 3;
                    int x = Math.max(0, rect.x - pad);
                    int y = Math.max(0, rect.y - pad);
                    int w = Math.min(original.cols() - x, rect.width + 2 * pad);
                    int h = Math.min(original.rows() - y, rect.height + 2 * pad);

                    lastDetectedRect = new Rect(x, y, w, h);

                    // Draw detected plate in green
                    Imgproc.rectangle(lastContourImage, lastDetectedRect, new Scalar(0, 255, 0), 3);

                    Mat plate = new Mat(original, lastDetectedRect);
                    saveDebugImage("step4_detected_plate", plate, currentImageName);

                    return plate;
                }
            }
        }

        System.out.println("[WARN] No plate detected. Checked " + checked + " quadrilaterals.");
        lastDetectedRect = null;
        return null;
    }

    public Mat drawBoundingBox(Scalar color, int thickness) {
        if (originalImage == null || lastDetectedRect == null) {
            return null;
        }
        Mat result = originalImage.clone();
        Imgproc.rectangle(result, lastDetectedRect, color, thickness);
        return result;
    }
}

