package com.alpr;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PlateDetector - License Plate Detection using Edge Detection and Contour Analysis
 *
 * <p>This class implements a simple but effective license plate detection algorithm
 * optimized for Turkish license plates.</p>
 *
 * <p>Turkish plate format: NN AAA NNNN (e.g., 34 ABC 1234)</p>
 * <p>Standard dimensions: 520mm x 110mm (aspect ratio ~4.7)</p>
 *
 * @author ALPR Academic Project
 * @version 1.2 - Simplified and more reliable detection
 */
public class PlateDetector {

    /**
     * Aspect ratio range for Turkish license plates.
     * Standard ratio is ~4.7, but we allow 2.5-6.0 for perspective distortion.
     */
    private static final double MIN_ASPECT_RATIO = 2.5;
    private static final double MAX_ASPECT_RATIO = 6.0;

    /**
     * Minimum plate area as percentage of image (0.5%).
     * Plates should be reasonably visible in the image.
     */
    private static final double MIN_PLATE_AREA_RATIO = 0.005;

    /**
     * Maximum plate area as percentage of image (15%).
     * Prevents detecting large non-plate regions.
     */
    private static final double MAX_PLATE_AREA_RATIO = 0.15;

    /**
     * Stores the original image for cropping.
     */
    private Mat originalImage;

    /**
     * Current image base name for saving debug images.
     */
    private String currentImageName;

    /**
     * Debug output directories.
     */
    private static final String DEBUG_BASE_DIR = "debug_output";
    private static final String[] DEBUG_DIRS = {
        "step1_grayscale",
        "step2_filtered",
        "step3_canny",
        "step3b_dilated",
        "step4_detected_plate",
        "step5_ocr_preprocessed"
    };

    /**
     * Static initializer to create debug directories.
     */
    static {
        createDebugDirectories();
    }

    /**
     * Creates all debug output directories.
     */
    private static void createDebugDirectories() {
        File baseDir = new File(DEBUG_BASE_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        for (String dir : DEBUG_DIRS) {
            File subDir = new File(DEBUG_BASE_DIR, dir);
            if (!subDir.exists()) {
                subDir.mkdirs();
            }
        }
        System.out.println("[DEBUG] Created debug directories in: " + baseDir.getAbsolutePath());
    }

    /**
     * Saves a debug image to the appropriate directory.
     *
     * @param stepDir   Directory name (e.g., "step1_grayscale")
     * @param image     Image to save
     * @param imageName Base name of the current image
     */
    private void saveDebugImage(String stepDir, Mat image, String imageName) {
        String filename = DEBUG_BASE_DIR + "/" + stepDir + "/" + imageName + ".jpg";
        Imgcodecs.imwrite(filename, image);
        System.out.println("[DEBUG] Saved: " + filename);
    }

    /**
     * Preprocesses image for license plate detection.
     *
     * <p>Pipeline:</p>
     * <ol>
     *   <li>Convert to grayscale</li>
     *   <li>Apply bilateral filter (edge-preserving smoothing)</li>
     *   <li>Canny edge detection</li>
     *   <li>Dilate to connect edges</li>
     * </ol>
     *
     * @param imagePath Path to input image
     * @return Edge-detected image ready for contour analysis
     */
    public Mat preprocessImageWithOriginal(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }

        // Extract image name for debug file naming
        File imageFile = new File(imagePath);
        currentImageName = imageFile.getName().replaceAll("\\.[^.]+$", "");

        // Load original image
        originalImage = Imgcodecs.imread(imagePath);
        if (originalImage.empty()) {
            System.err.println("[ERROR] Could not load image: " + imagePath);
            return null;
        }

        System.out.println("[INFO] Image loaded: " + imagePath);
        System.out.println("[INFO] Dimensions: " + originalImage.cols() + "x" + originalImage.rows());

        // Step 1: Grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(originalImage, gray, Imgproc.COLOR_BGR2GRAY);
        saveDebugImage("step1_grayscale", gray, currentImageName);

        // Step 2: Bilateral filter - reduces noise while keeping edges sharp
        Mat filtered = new Mat();
        Imgproc.bilateralFilter(gray, filtered, 11, 17, 17);
        saveDebugImage("step2_filtered", filtered, currentImageName);

        // Step 3: Canny edge detection
        Mat edges = new Mat();
        Imgproc.Canny(filtered, edges, 30, 200);
        saveDebugImage("step3_canny", edges, currentImageName);

        // Step 4: Dilate to close small gaps in edges
        Mat dilated = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edges, dilated, kernel, new Point(-1, -1), 1);
        saveDebugImage("step3b_dilated", dilated, currentImageName);

        System.out.println("[INFO] Preprocessing complete.");
        return dilated;
    }

    /**
     * Finds license plate region using contour analysis.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Find all contours</li>
     *   <li>Sort by area (largest first)</li>
     *   <li>For each contour, approximate to polygon</li>
     *   <li>If 4 vertices and correct aspect ratio → plate found</li>
     * </ol>
     *
     * @param edgeImage Edge-detected image
     * @param original  Original image for cropping
     * @return Cropped plate region, or null if not found
     */
    public Mat findPlateContour(Mat edgeImage, Mat original) {
        if (edgeImage == null || edgeImage.empty() || original == null || original.empty()) {
            throw new IllegalArgumentException("Input images cannot be null or empty");
        }

        double imageArea = edgeImage.rows() * edgeImage.cols();
        double minArea = imageArea * MIN_PLATE_AREA_RATIO;
        double maxArea = imageArea * MAX_PLATE_AREA_RATIO;

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeImage.clone(), contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println("[INFO] Found " + contours.size() + " contours");

        // Sort contours by area (largest first)
        contours.sort((c1, c2) -> Double.compare(
                Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

        int checked = 0;

        // Check top 30 largest contours
        for (int i = 0; i < Math.min(30, contours.size()); i++) {
            MatOfPoint contour = contours.get(i);
            double area = Imgproc.contourArea(contour);

            // Skip if area out of range
            if (area < minArea || area > maxArea) {
                continue;
            }

            // Approximate contour to polygon
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double peri = Imgproc.arcLength(contour2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approx, 0.02 * peri, true);

            // Check if quadrilateral (4 vertices)
            if (approx.toArray().length == 4) {
                checked++;
                Rect rect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) rect.width / rect.height;

                System.out.println("[DEBUG] Candidate #" + checked +
                        ": Area=" + (int) area +
                        ", AR=" + String.format("%.2f", aspectRatio) +
                        ", Size=" + rect.width + "x" + rect.height);

                // Check aspect ratio
                if (aspectRatio >= MIN_ASPECT_RATIO && aspectRatio <= MAX_ASPECT_RATIO) {
                    System.out.println("[SUCCESS] Plate found!");
                    System.out.println("[INFO] Location: (" + rect.x + "," + rect.y +
                            ") Size: " + rect.width + "x" + rect.height);

                    // Add small padding and crop
                    int pad = 3;
                    int x = Math.max(0, rect.x - pad);
                    int y = Math.max(0, rect.y - pad);
                    int w = Math.min(original.cols() - x, rect.width + 2 * pad);
                    int h = Math.min(original.rows() - y, rect.height + 2 * pad);

                    Mat plate = new Mat(original, new Rect(x, y, w, h));
                    saveDebugImage("step4_detected_plate", plate, currentImageName);

                    return plate;
                }
            }
        }

        System.out.println("[WARN] No plate detected. Checked " + checked + " quadrilaterals.");
        return null;
    }

    /**
     * Gets the current image name being processed.
     *
     * @return Current image base name
     */
    public String getCurrentImageName() {
        return currentImageName;
    }

    /**
     * Gets the original loaded image.
     *
     * @return Original BGR image
     */
    public Mat getOriginalImage() {
        return originalImage;
    }

    /**
     * Gets the debug base directory path.
     *
     * @return Debug output directory path
     */
    public static String getDebugBaseDir() {
        return DEBUG_BASE_DIR;
    }
}
