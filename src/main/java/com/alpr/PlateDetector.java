package com.alpr;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PlateDetector - License Plate Detection and Image Processing Class
 *
 * <p>This class is responsible for preprocessing vehicle images to prepare them
 * for license plate detection. The preprocessing pipeline includes grayscale
 * conversion, noise reduction, and edge detection.</p>
 *
 * <p>Why preprocessing is essential for ALPR:</p>
 * <ul>
 *   <li>Grayscale reduces computational complexity (1 channel vs 3 channels)</li>
 *   <li>Gaussian blur removes high-frequency noise that can cause false edges</li>
 *   <li>Canny edge detection highlights the rectangular shape of license plates</li>
 * </ul>
 *
 * <p><b>Note:</b> OpenCV native libraries must be loaded before using this class.
 * This is handled by the Main class static initializer.</p>
 *
 * @author ALPR Academic Project
 * @version 1.0
 */
public class PlateDetector {

    /**
     * Minimum aspect ratio for license plate detection.
     *
     * <p>Why 2.5:</p>
     * <ul>
     *   <li>Most license plates worldwide have width > height</li>
     *   <li>European plates are typically ~520mm x 110mm (ratio ~4.7)</li>
     *   <li>US plates are typically ~305mm x 152mm (ratio ~2.0)</li>
     *   <li>Turkish plates are typically ~520mm x 110mm (ratio ~4.7)</li>
     *   <li>We use 2.5 as lower bound to accommodate various plate types</li>
     * </ul>
     */
    private static final double MIN_ASPECT_RATIO = 2.5;

    /**
     * Maximum aspect ratio for license plate detection.
     *
     * <p>Why 5.5:</p>
     * <ul>
     *   <li>Upper bound prevents false positives from very wide rectangles</li>
     *   <li>No standard plate exceeds ratio of 5.5</li>
     *   <li>Provides margin for slightly angled plate detection</li>
     * </ul>
     */
    private static final double MAX_ASPECT_RATIO = 5.5;

    /**
     * Minimum contour area threshold (in pixels).
     *
     * <p>Why we filter by area:</p>
     * <ul>
     *   <li>Eliminates tiny noise contours that waste processing time</li>
     *   <li>License plates occupy a reasonable portion of vehicle images</li>
     *   <li>Value of 1000 works for images ~640x480 or larger</li>
     * </ul>
     */
    private static final double MIN_CONTOUR_AREA = 1000;

    /**
     * Preprocesses an input image through a series of computer vision operations
     * to prepare it for license plate detection.
     *
     * <p>The preprocessing pipeline consists of three stages:</p>
     * <ol>
     *   <li><b>Grayscale Conversion:</b> Reduces the image from 3 color channels (BGR)
     *       to 1 channel, simplifying subsequent processing and reducing memory usage.</li>
     *   <li><b>Gaussian Blur:</b> Applies a 5x5 Gaussian kernel to smooth the image
     *       and reduce high-frequency noise. This prevents false edge detection.</li>
     *   <li><b>Canny Edge Detection:</b> Identifies edges in the image using gradient
     *       analysis. License plates typically have strong rectangular edges.</li>
     * </ol>
     *
     * <p>Each intermediate result is saved to disk for debugging and academic
     * documentation purposes.</p>
     *
     * @param imagePath The absolute or relative file path to the input image.
     *                  Supported formats: JPEG, PNG, BMP, TIFF
     * @return A Mat object containing the edge-detected image (Canny output),
     *         or null if the image could not be loaded
     * @throws IllegalArgumentException if imagePath is null or empty
     *
     * @see <a href="https://docs.opencv.org/4.x/d7/d4d/tutorial_py_thresholding.html">
     *      OpenCV Image Thresholding Tutorial</a>
     */
    public Mat preprocessImage(String imagePath) {
        // Validate input parameter
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }

        // Step 0: Load the original image from disk
        // Why: OpenCV's Imgcodecs.imread() reads the image into a Mat (Matrix) object
        // The image is loaded in BGR format (Blue-Green-Red), which is OpenCV's default
        Mat originalImage = Imgcodecs.imread(imagePath);

        // Verify that the image was loaded successfully
        // Why: imread() returns an empty Mat if the file doesn't exist or is corrupted
        if (originalImage.empty()) {
            System.err.println("[ERROR] Could not load image from path: " + imagePath);
            return null;
        }

        System.out.println("[INFO] Image loaded successfully: " + imagePath);
        System.out.println("[INFO] Image dimensions: " + originalImage.cols() + "x" + originalImage.rows());

        // Step 1: Convert to Grayscale
        // Why: Color information is not necessary for edge detection
        // Grayscale reduces data from 3 channels to 1, making processing 3x faster
        // License plate detection relies on shape/edges, not color
        Mat grayImage = new Mat();
        Imgproc.cvtColor(originalImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgcodecs.imwrite("step1_gray.jpg", grayImage);
        System.out.println("[DEBUG] Saved: step1_gray.jpg");

        // Step 2: Apply Gaussian Blur
        // Why: Smoothing reduces high-frequency noise in the image
        // Kernel size 5x5 is chosen as a balance between noise reduction and detail preservation
        // Sigma (0,0) means OpenCV calculates optimal sigma from kernel size
        // Without blur, Canny would detect noise as false edges
        Mat blurredImage = new Mat();
        Imgproc.GaussianBlur(grayImage, blurredImage, new Size(5, 5), 0);
        Imgcodecs.imwrite("step2_blur.jpg", blurredImage);
        System.out.println("[DEBUG] Saved: step2_blur.jpg");

        // Step 3: Apply Canny Edge Detection
        // Why: Canny is a multi-stage algorithm that detects a wide range of edges
        // It uses gradient analysis to find areas of rapid intensity change
        // Threshold1 (100): Lower threshold for edge linking
        // Threshold2 (200): Upper threshold for strong edge detection
        // Edges with gradient > threshold2 are definitely edges
        // Edges with gradient < threshold1 are definitely not edges
        // Edges between thresholds are edges only if connected to strong edges
        Mat cannyImage = new Mat();
        Imgproc.Canny(blurredImage, cannyImage, 100, 200);
        Imgcodecs.imwrite("step3_canny.jpg", cannyImage);
        System.out.println("[DEBUG] Saved: step3_canny.jpg");

        System.out.println("[INFO] Preprocessing complete. Edge-detected image ready.");

        // Return the final processed image for further analysis (contour detection, etc.)
        return cannyImage;
    }

    /**
     * Finds and extracts the license plate region from a Canny edge-detected image.
     *
     * <p>This method implements the following algorithm:</p>
     * <ol>
     *   <li><b>Contour Detection:</b> Finds all contours (closed curves) in the edge image.
     *       Contours represent boundaries of objects in the image.</li>
     *   <li><b>Polygon Approximation:</b> Simplifies each contour to a polygon with fewer
     *       vertices using the Douglas-Peucker algorithm (approxPolyDP).</li>
     *   <li><b>Quadrilateral Filter:</b> Keeps only contours that approximate to 4 vertices,
     *       as license plates are rectangular.</li>
     *   <li><b>Aspect Ratio Filter:</b> Validates that the rectangle's width/height ratio
     *       matches typical license plate dimensions (2.5 to 5.5).</li>
     *   <li><b>Cropping:</b> Extracts the detected plate region from the original image.</li>
     * </ol>
     *
     * <p><b>Why aspect ratio filtering is critical:</b></p>
     * <p>License plates have standardized dimensions worldwide. While the exact size varies
     * by country, the aspect ratio (width/height) falls within a predictable range:</p>
     * <ul>
     *   <li>European/Turkish plates: ~520mm x 110mm → ratio ≈ 4.7</li>
     *   <li>US plates: ~305mm x 152mm → ratio ≈ 2.0</li>
     *   <li>Motorcycle plates: smaller but similar ratios</li>
     * </ul>
     * <p>By filtering for ratios between 2.5-5.5, we eliminate false positives like
     * windows, grilles, and other rectangular vehicle features that don't match
     * plate proportions.</p>
     *
     * @param cannyImage The edge-detected image from Canny processing
     * @param originalImage The original color/grayscale image to crop from
     * @return A Mat containing the cropped license plate region, or null if no plate found
     * @throws IllegalArgumentException if either input Mat is null or empty
     */
    public Mat findPlateContour(Mat cannyImage, Mat originalImage) {
        // Validate input parameters
        if (cannyImage == null || cannyImage.empty()) {
            throw new IllegalArgumentException("Canny image cannot be null or empty");
        }
        if (originalImage == null || originalImage.empty()) {
            throw new IllegalArgumentException("Original image cannot be null or empty");
        }

        // Step 1: Find all contours in the edge-detected image
        // Why: Contours are curves joining all continuous points along a boundary
        // RETR_TREE retrieves all contours and reconstructs full hierarchy
        // CHAIN_APPROX_SIMPLE compresses horizontal, vertical, diagonal segments
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyImage, contours, hierarchy,
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println("[INFO] Found " + contours.size() + " contours");

        // Step 2: Sort contours by area in descending order
        // Why: Larger contours are more likely to be the license plate
        // Smaller contours are usually noise or minor features
        contours.sort((c1, c2) -> Double.compare(
                Imgproc.contourArea(c2),
                Imgproc.contourArea(c1)));

        // Step 3: Iterate through contours to find the license plate
        Mat detectedPlate = null;
        int candidateCount = 0;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // Filter out contours that are too small
            // Why: License plates occupy a significant area in the image
            // Very small contours are noise and not worth processing
            if (area < MIN_CONTOUR_AREA) {
                continue;
            }

            // Step 4: Approximate the contour to a polygon
            // Why: Real-world contours are noisy; approximation simplifies them
            // epsilon = 2% of perimeter is a good balance between accuracy and simplification
            // The Douglas-Peucker algorithm removes points that don't significantly
            // affect the shape, leaving only the essential vertices
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double perimeter = Imgproc.arcLength(contour2f, true);
            double epsilon = 0.02 * perimeter;

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);

            Point[] approxPoints = approxCurve.toArray();

            // Step 5: Check if the approximated polygon has exactly 4 vertices
            // Why: License plates are rectangular (4 corners)
            // Polygons with more/fewer vertices are not license plates
            if (approxPoints.length == 4) {
                candidateCount++;

                // Step 6: Get the bounding rectangle and calculate aspect ratio
                // Why: boundingRect gives us the smallest upright rectangle
                // containing the contour, making aspect ratio calculation easy
                Rect boundingRect = Imgproc.boundingRect(contour);
                double aspectRatio = (double) boundingRect.width / boundingRect.height;

                System.out.println("[DEBUG] Candidate #" + candidateCount +
                        ": Area=" + String.format("%.0f", area) +
                        ", AspectRatio=" + String.format("%.2f", aspectRatio) +
                        ", Vertices=" + approxPoints.length);

                // Step 7: Filter by aspect ratio
                // Why: License plates have a specific width-to-height ratio
                // ┌─────────────────────────────────────────────────────────┐
                // │  ASPECT RATIO FILTERING EXPLAINED                       │
                // ├─────────────────────────────────────────────────────────┤
                // │  Aspect Ratio = Width / Height                          │
                // │                                                         │
                // │  Example: A plate 520mm wide × 110mm tall               │
                // │           Ratio = 520/110 = 4.73                        │
                // │                                                         │
                // │  Valid range [2.5 - 5.5] covers:                        │
                // │  • Wide European plates (ratio ~4.7)                    │
                // │  • Squarer US plates (ratio ~2.0, with margin)          │
                // │  • Accounts for perspective distortion                  │
                // │                                                         │
                // │  This filter REJECTS:                                   │
                // │  • Square shapes (windows, logos) → ratio ~1.0          │
                // │  • Very tall rectangles (door handles) → ratio <1.0     │
                // │  • Very wide shapes (bumpers) → ratio >6.0              │
                // └──────────────────���──────────────────────────────────────┘
                if (aspectRatio >= MIN_ASPECT_RATIO && aspectRatio <= MAX_ASPECT_RATIO) {
                    System.out.println("[SUCCESS] License plate candidate found!");
                    System.out.println("[INFO] Bounding box: " + boundingRect.x + "," +
                            boundingRect.y + " - " + boundingRect.width + "x" + boundingRect.height);

                    // Step 8: Crop the detected plate region from the original image
                    // Why: We need the actual plate image (not just edges) for OCR
                    // Using original image preserves the character details
                    detectedPlate = new Mat(originalImage, boundingRect);

                    // Save the detected plate for debugging
                    Imgcodecs.imwrite("step4_detected_plate.jpg", detectedPlate);
                    System.out.println("[DEBUG] Saved: step4_detected_plate.jpg");

                    // Return the first valid plate found (largest by area)
                    break;
                }
            }
        }

        if (detectedPlate == null) {
            System.out.println("[WARN] No license plate detected in the image");
            System.out.println("[HINT] Total 4-vertex candidates checked: " + candidateCount);
        }

        return detectedPlate;
    }

    /**
     * Returns the original image that was loaded during preprocessing.
     * This is needed for cropping the plate region after contour detection.
     */
    private Mat originalImage;

    /**
     * Preprocesses an input image and stores the original for later cropping.
     *
     * <p>Enhanced preprocessing pipeline:</p>
     * <ol>
     *   <li>Grayscale conversion</li>
     *   <li>Bilateral filter for noise reduction while preserving edges</li>
     *   <li>Canny edge detection</li>
     *   <li>Dilation to close gaps in edges</li>
     * </ol>
     *
     * @param imagePath The path to the input image
     * @return The edge-detected image, or null if loading failed
     */
    public Mat preprocessImageWithOriginal(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Image path cannot be null or empty");
        }

        originalImage = Imgcodecs.imread(imagePath);

        if (originalImage.empty()) {
            System.err.println("[ERROR] Could not load image from path: " + imagePath);
            return null;
        }

        System.out.println("[INFO] Image loaded successfully: " + imagePath);
        System.out.println("[INFO] Image dimensions: " + originalImage.cols() + "x" + originalImage.rows());

        // Step 1: Convert to Grayscale
        Mat grayImage = new Mat();
        Imgproc.cvtColor(originalImage, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgcodecs.imwrite("step1_gray.jpg", grayImage);
        System.out.println("[DEBUG] Saved: step1_gray.jpg");

        // Step 2: Apply Bilateral Filter
        // Why: Bilateral filter reduces noise while keeping edges sharp
        // This is better than Gaussian blur for license plate detection
        Mat filteredImage = new Mat();
        Imgproc.bilateralFilter(grayImage, filteredImage, 11, 17, 17);
        Imgcodecs.imwrite("step2_blur.jpg", filteredImage);
        System.out.println("[DEBUG] Saved: step2_blur.jpg");

        // Step 3: Apply Canny Edge Detection
        // Why: Lower thresholds to capture more edges
        Mat cannyImage = new Mat();
        Imgproc.Canny(filteredImage, cannyImage, 30, 200);
        Imgcodecs.imwrite("step3_canny.jpg", cannyImage);
        System.out.println("[DEBUG] Saved: step3_canny.jpg");

        // Step 4: Apply Dilation to close gaps in edges
        // Why: Dilation connects nearby edge pixels, making contours more complete
        // This helps form closed rectangles from broken edges
        Mat dilatedImage = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(cannyImage, dilatedImage, kernel, new Point(-1, -1), 1);
        Imgcodecs.imwrite("step3b_dilated.jpg", dilatedImage);
        System.out.println("[DEBUG] Saved: step3b_dilated.jpg");

        System.out.println("[INFO] Preprocessing complete. Edge-detected image ready.");

        return dilatedImage;
    }

    /**
     * Gets the original image that was loaded during preprocessing.
     *
     * @return The original BGR image, or null if no image has been loaded
     */
    public Mat getOriginalImage() {
        return originalImage;
    }
}
