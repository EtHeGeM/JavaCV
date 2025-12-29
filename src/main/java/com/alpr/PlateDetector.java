package com.alpr;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PlateDetector - Dual Detection System using Haar Cascade and Geometric Analysis
 *
 * @author ALPR Academic Project
 * @version 2.1 - Improved accuracy with separate plate crops for each method
 */
public class PlateDetector {

    // Haar Cascade
    private CascadeClassifier haarClassifier;
    private boolean haarAvailable = false;
    private static final String[] HAAR_CASCADE_FILES = {
        "haarcascade_russian_plate_number.xml",
        "haarcascade_licence_plate_rus_16stages.xml"
    };

    // Default values for tunable parameters
    private static final int DEFAULT_BLUR_KERNEL = 11;
    private static final int DEFAULT_CANNY_THRESHOLD1 = 50;
    private static final int DEFAULT_CANNY_THRESHOLD2 = 150;
    private static final double DEFAULT_MIN_ASPECT_RATIO = 2.0;
    private static final double DEFAULT_MAX_ASPECT_RATIO = 7.0;

    // Instance tunable parameters
    private int blurKernel = DEFAULT_BLUR_KERNEL;
    private int cannyThreshold1 = DEFAULT_CANNY_THRESHOLD1;
    private int cannyThreshold2 = DEFAULT_CANNY_THRESHOLD2;
    private double minAspectRatio = DEFAULT_MIN_ASPECT_RATIO;
    private double maxAspectRatio = DEFAULT_MAX_ASPECT_RATIO;

    // Haar parameters
    private double haarScaleFactor = 1.05;
    private int haarMinNeighbors = 3;

    private static final double MIN_PLATE_AREA_RATIO = 0.002;
    private static final double MAX_PLATE_AREA_RATIO = 0.20;

    private Mat originalImage;
    private Rect lastDetectedRect;
    private String currentImageName;

    // Intermediate results for GUI preview
    private Mat lastGrayImage;
    private Mat lastFilteredImage;
    private Mat lastEdgeImage;
    private Mat lastDilatedImage;
    private Mat lastContourImage;

    // Detection results
    private List<DetectionResult> lastResults = new ArrayList<>();

    private static final String DEBUG_BASE_DIR = "debug_output";
    private static final String[] DEBUG_DIRS = {
        "step1_grayscale", "step2_filtered", "step3_canny",
        "step3b_dilated", "step4_detected_plate", "step5_ocr_preprocessed",
        "haar_plates", "geo_plates"
    };

    public PlateDetector() {
        createDebugDirectories();
        initializeHaarClassifier();
    }

    private void createDebugDirectories() {
        File baseDir = new File(DEBUG_BASE_DIR);
        if (!baseDir.exists()) baseDir.mkdirs();
        for (String dir : DEBUG_DIRS) {
            File subDir = new File(DEBUG_BASE_DIR, dir);
            if (!subDir.exists()) subDir.mkdirs();
        }
    }

    private void initializeHaarClassifier() {
        for (String cascadeFile : HAAR_CASCADE_FILES) {
            File file = new File(cascadeFile);
            if (!file.exists()) {
                file = new File("src/main/resources/" + cascadeFile);
            }

            if (file.exists()) {
                haarClassifier = new CascadeClassifier(file.getAbsolutePath());
                if (!haarClassifier.empty()) {
                    haarAvailable = true;
                    System.out.println("[INFO] Haar Cascade loaded: " + file.getAbsolutePath());
                    return;
                }
            }
        }
        System.err.println("[WARN] No Haar Cascade file found. Haar detection disabled.");
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

    public void setHaarScaleFactor(double factor) {
        this.haarScaleFactor = factor;
    }

    public void setHaarMinNeighbors(int neighbors) {
        this.haarMinNeighbors = neighbors;
    }

    // ==================== PARAMETER GETTERS ====================

    public int getBlurKernel() { return blurKernel; }
    public int getCannyThreshold1() { return cannyThreshold1; }
    public int getCannyThreshold2() { return cannyThreshold2; }
    public double getMinAspectRatio() { return minAspectRatio; }
    public double getMaxAspectRatio() { return maxAspectRatio; }
    public boolean isHaarAvailable() { return haarAvailable; }

    // ==================== INTERMEDIATE IMAGE GETTERS ====================

    public Mat getLastGrayImage() { return lastGrayImage; }
    public Mat getLastFilteredImage() { return lastFilteredImage; }
    public Mat getLastEdgeImage() { return lastEdgeImage; }
    public Mat getLastDilatedImage() { return lastDilatedImage; }
    public Mat getLastContourImage() { return lastContourImage; }
    public Mat getOriginalImage() { return originalImage; }
    public String getCurrentImageName() { return currentImageName; }
    public Rect getLastDetectedRect() { return lastDetectedRect; }
    public List<DetectionResult> getLastResults() { return lastResults; }
    public static String getDebugBaseDir() { return DEBUG_BASE_DIR; }

    // ==================== IMAGE LOADING ====================

    public boolean loadImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }
        File imageFile = new File(imagePath);
        currentImageName = imageFile.getName().replaceAll("\\.[^.]+$", "");
        originalImage = Imgcodecs.imread(imagePath);
        lastResults.clear();
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

        // Step 2: CLAHE for contrast enhancement
        Mat enhanced = new Mat();
        Imgproc.createCLAHE(2.0, new Size(8, 8)).apply(lastGrayImage, enhanced);

        // Step 3: Bilateral filter
        lastFilteredImage = new Mat();
        Imgproc.bilateralFilter(enhanced, lastFilteredImage, blurKernel, 17, 17);

        // Step 4: Canny edge detection
        lastEdgeImage = new Mat();
        Imgproc.Canny(lastFilteredImage, lastEdgeImage, cannyThreshold1, cannyThreshold2);

        // Step 5: Morphological Closing - connect horizontal elements
        Mat closedImage = new Mat();
        Mat closeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 5));
        Imgproc.morphologyEx(lastEdgeImage, closedImage, Imgproc.MORPH_CLOSE, closeKernel);

        // Step 6: Dilate
        lastDilatedImage = new Mat();
        Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(closedImage, lastDilatedImage, dilateKernel, new Point(-1, -1), 2);

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

        if (currentImageName != null) {
            saveDebugImage("step1_grayscale", lastGrayImage, currentImageName);
            saveDebugImage("step2_filtered", lastFilteredImage, currentImageName);
            saveDebugImage("step3_canny", lastEdgeImage, currentImageName);
            saveDebugImage("step3b_dilated", lastDilatedImage, currentImageName);
        }

        System.out.println("[INFO] Preprocessing complete.");
        return result;
    }

    // ==================== DUAL DETECTION ====================

    /**
     * Runs both Haar Cascade and Geometric detection methods.
     * Each detection gets its own cropped plate image.
     */
    public List<DetectionResult> detectAll() {
        lastResults.clear();

        if (originalImage == null || originalImage.empty()) {
            return lastResults;
        }

        if (lastGrayImage == null || lastGrayImage.empty()) {
            preprocess();
        }

        // Run Haar Cascade detection
        List<DetectionResult> haarResults = detectWithHaar();
        lastResults.addAll(haarResults);

        // Run Geometric detection
        List<DetectionResult> geoResults = detectWithGeometric();
        lastResults.addAll(geoResults);

        System.out.println("[INFO] Detection complete - Haar: " + haarResults.size() +
                          ", Geometric: " + geoResults.size());

        createContourVisualization();

        return lastResults;
    }

    /**
     * Detects plates using Haar Cascade classifier.
     */
    private List<DetectionResult> detectWithHaar() {
        List<DetectionResult> results = new ArrayList<>();

        if (!haarAvailable || lastGrayImage == null) {
            return results;
        }

        // Apply histogram equalization for better detection
        Mat equalizedGray = new Mat();
        Imgproc.equalizeHist(lastGrayImage, equalizedGray);

        MatOfRect detections = new MatOfRect();
        haarClassifier.detectMultiScale(
            equalizedGray,
            detections,
            haarScaleFactor,
            haarMinNeighbors,
            0,
            new Size(80, 20),
            new Size(500, 150)
        );

        int idx = 0;
        for (Rect rect : detections.toArray()) {
            // Validate aspect ratio
            double ar = (double) rect.width / rect.height;
            if (ar < 1.5 || ar > 8.0) continue;

            DetectionResult result = new DetectionResult(rect, DetectionResult.MethodType.HAAR);

            // Crop and store the plate with padding
            Mat croppedPlate = cropPlateWithPadding(rect, 5);
            if (croppedPlate != null) {
                result.setCroppedPlate(croppedPlate);
                saveDebugImage("haar_plates", croppedPlate, currentImageName + "_haar_" + idx);
            }

            results.add(result);
            System.out.println("[HAAR] Detected #" + idx + ": " + rect.x + "," + rect.y +
                              " size: " + rect.width + "x" + rect.height + " AR: " + String.format("%.2f", ar));
            idx++;
        }

        return results;
    }

    /**
     * Detects plates using Geometric/Contour analysis with perspective transform.
     */
    private List<DetectionResult> detectWithGeometric() {
        List<DetectionResult> results = new ArrayList<>();

        if (lastDilatedImage == null || lastDilatedImage.empty()) {
            return results;
        }

        double imageArea = lastDilatedImage.rows() * lastDilatedImage.cols();
        double minArea = imageArea * MIN_PLATE_AREA_RATIO;
        double maxArea = imageArea * MAX_PLATE_AREA_RATIO;

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(lastDilatedImage.clone(), contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        contours.sort((c1, c2) -> Double.compare(
                Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

        int idx = 0;
        for (int i = 0; i < Math.min(50, contours.size()); i++) {
            MatOfPoint contour = contours.get(i);
            double area = Imgproc.contourArea(contour);

            if (area < minArea || area > maxArea) continue;

            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, 0.018 * peri, true);

            Point[] pts = approx.toArray();

            // Accept 4-6 vertices (more flexible for noisy contours)
            if (pts.length >= 4 && pts.length <= 6) {
                Rect rect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) rect.width / rect.height;

                if (aspectRatio >= minAspectRatio && aspectRatio <= maxAspectRatio) {
                    DetectionResult result = new DetectionResult(rect, DetectionResult.MethodType.GEOMETRIC);

                    // Use four-point transform if we have exactly 4 points
                    Mat croppedPlate;
                    if (pts.length == 4) {
                        croppedPlate = fourPointTransform(originalImage, pts);
                    } else {
                        croppedPlate = cropPlateWithPadding(rect, 3);
                    }

                    if (croppedPlate != null && !croppedPlate.empty()) {
                        result.setCroppedPlate(croppedPlate);
                        saveDebugImage("geo_plates", croppedPlate, currentImageName + "_geo_" + idx);
                    }

                    results.add(result);
                    System.out.println("[GEO] Detected #" + idx + ": " + rect.x + "," + rect.y +
                                      " size: " + rect.width + "x" + rect.height +
                                      " AR: " + String.format("%.2f", aspectRatio));
                    idx++;

                    // Limit to top 3 geometric detections
                    if (idx >= 3) break;
                }
            }
        }

        return results;
    }

    /**
     * Crops a plate region with padding.
     */
    private Mat cropPlateWithPadding(Rect rect, int padding) {
        if (originalImage == null) return null;

        int x = Math.max(0, rect.x - padding);
        int y = Math.max(0, rect.y - padding);
        int w = Math.min(originalImage.cols() - x, rect.width + 2 * padding);
        int h = Math.min(originalImage.rows() - y, rect.height + 2 * padding);

        if (w <= 0 || h <= 0) return null;

        return new Mat(originalImage, new Rect(x, y, w, h));
    }

    /**
     * Creates visualization with colored bounding boxes.
     */
    private void createContourVisualization() {
        if (originalImage == null) return;

        lastContourImage = originalImage.clone();

        List<Rect> highConfidenceRects = findHighConfidenceDetections();

        for (DetectionResult result : lastResults) {
            Rect rect = result.getBounds();
            Scalar color;
            int thickness = 2;

            boolean isHighConfidence = highConfidenceRects.stream()
                    .anyMatch(hc -> DetectionResult.calculateIoU(rect, hc) > 0.5);

            if (isHighConfidence) {
                color = new Scalar(0, 0, 255); // Red
                thickness = 3;
            } else if (result.getMethod() == DetectionResult.MethodType.HAAR) {
                color = new Scalar(0, 255, 0); // Green
            } else {
                color = new Scalar(255, 0, 0); // Blue
            }

            Imgproc.rectangle(lastContourImage, rect, color, thickness);

            String label = result.getMethod() == DetectionResult.MethodType.HAAR ? "H" : "G";
            if (isHighConfidence) label = "H+G";
            if (result.getOcrResult() != null && !result.getOcrResult().isEmpty()) {
                label += ": " + result.getOcrResult();
            }
            Imgproc.putText(lastContourImage, label,
                    new Point(rect.x, rect.y - 5),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
        }
    }

    private List<Rect> findHighConfidenceDetections() {
        List<Rect> highConfidence = new ArrayList<>();

        List<DetectionResult> haarResults = new ArrayList<>();
        List<DetectionResult> geoResults = new ArrayList<>();

        for (DetectionResult result : lastResults) {
            if (result.getMethod() == DetectionResult.MethodType.HAAR) {
                haarResults.add(result);
            } else {
                geoResults.add(result);
            }
        }

        for (DetectionResult haar : haarResults) {
            for (DetectionResult geo : geoResults) {
                if (haar.overlaps(geo, 0.3)) {
                    highConfidence.add(haar.getBounds());
                }
            }
        }

        return highConfidence;
    }

    // ==================== FOUR POINT TRANSFORM ====================

    private Point[] orderPoints(Point[] pts) {
        if (pts == null || pts.length != 4) {
            return null;
        }

        Point[] ordered = new Point[4];
        double[] sums = new double[4];
        double[] diffs = new double[4];

        for (int i = 0; i < 4; i++) {
            sums[i] = pts[i].x + pts[i].y;
            diffs[i] = pts[i].y - pts[i].x;
        }

        ordered[0] = pts[indexOfMin(sums)];
        ordered[2] = pts[indexOfMax(sums)];
        ordered[1] = pts[indexOfMin(diffs)];
        ordered[3] = pts[indexOfMax(diffs)];

        return ordered;
    }

    private int indexOfMin(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < arr[idx]) idx = i;
        }
        return idx;
    }

    private int indexOfMax(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    private Mat fourPointTransform(Mat image, Point[] pts) {
        Point[] ordered = orderPoints(pts);
        if (ordered == null) return null;

        Point tl = ordered[0], tr = ordered[1], br = ordered[2], bl = ordered[3];

        double widthTop = distance(tl, tr);
        double widthBottom = distance(bl, br);
        int maxWidth = (int) Math.max(widthTop, widthBottom);

        double heightLeft = distance(tl, bl);
        double heightRight = distance(tr, br);
        int maxHeight = (int) Math.max(heightLeft, heightRight);

        maxWidth = Math.max(maxWidth, 100);
        maxHeight = Math.max(maxHeight, 30);

        MatOfPoint2f srcPoints = new MatOfPoint2f(tl, tr, br, bl);
        MatOfPoint2f dstPoints = new MatOfPoint2f(
            new Point(0, 0),
            new Point(maxWidth - 1, 0),
            new Point(maxWidth - 1, maxHeight - 1),
            new Point(0, maxHeight - 1)
        );

        Mat transformMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        Mat warped = new Mat();
        Imgproc.warpPerspective(image, warped, transformMatrix, new Size(maxWidth, maxHeight));

        return warped;
    }

    private double distance(Point p1, Point p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ==================== LEGACY METHODS ====================

    public Mat findPlate() {
        if (lastDilatedImage == null || originalImage == null) {
            return null;
        }
        return findPlateContour(lastDilatedImage, originalImage);
    }

    public Mat findPlateContour(Mat edgeImage, Mat original) {
        // Run detection and return the best result
        if (lastResults.isEmpty()) {
            detectAll();
        }

        // Prefer high confidence, then Haar, then Geometric
        for (DetectionResult result : lastResults) {
            if (result.getCroppedPlate() != null) {
                lastDetectedRect = result.getBounds();
                return result.getCroppedPlate();
            }
        }

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

    public int[] getDetectionStats() {
        int haarCount = 0;
        int geoCount = 0;
        int overlapCount = 0;

        List<DetectionResult> haarResults = new ArrayList<>();
        List<DetectionResult> geoResults = new ArrayList<>();

        for (DetectionResult result : lastResults) {
            if (result.getMethod() == DetectionResult.MethodType.HAAR) {
                haarCount++;
                haarResults.add(result);
            } else {
                geoCount++;
                geoResults.add(result);
            }
        }

        for (DetectionResult haar : haarResults) {
            for (DetectionResult geo : geoResults) {
                if (haar.overlaps(geo, 0.3)) {
                    overlapCount++;
                }
            }
        }

        return new int[]{haarCount, geoCount, overlapCount};
    }
}

