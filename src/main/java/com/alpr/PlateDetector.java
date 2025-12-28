package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

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
 * @author ALPR Academic Project
 * @version 1.0
 */
public class PlateDetector {

    /**
     * Static initializer block to load OpenCV native libraries.
     *
     * <p>Why we use OpenCV.loadShared():</p>
     * <ul>
     *   <li>The org.openpnp OpenCV wrapper bundles native libraries for all platforms</li>
     *   <li>loadShared() extracts and loads the appropriate native library for the current OS</li>
     *   <li>This must be called BEFORE any OpenCV operations are performed</li>
     *   <li>Static block ensures it runs exactly once when the class is first loaded</li>
     * </ul>
     */
    static {
        OpenCV.loadShared();
    }

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
}

