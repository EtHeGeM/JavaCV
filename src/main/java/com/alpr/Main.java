package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;

/**
 * Main - Entry Point for the ALPR (Automatic License Plate Recognition) System
 *
 * <p>This is a monolithic console application that demonstrates the complete
 * pipeline for detecting and reading license plates from vehicle images.</p>
 *
 * <p>Application Flow:</p>
 * <ol>
 *   <li>Load an input image containing a vehicle</li>
 *   <li>Preprocess the image (grayscale, blur, edge detection)</li>
 *   <li>Detect license plate region (future implementation)</li>
 *   <li>Extract text using OCR (future implementation)</li>
 * </ol>
 *
 * @author ALPR Academic Project
 * @version 1.0
 */
public class Main {

    /**
     * Static initializer to load OpenCV native libraries.
     *
     * <p>Why we load OpenCV here in Main:</p>
     * <ul>
     *   <li>Ensures OpenCV is loaded before ANY OpenCV class is used</li>
     *   <li>Main is the entry point, so this runs first</li>
     *   <li>Prevents UnsatisfiedLinkError when using OpenCV classes</li>
     * </ul>
     */
    static {
        OpenCV.loadShared();
    }

    /**
     * Application entry point.
     *
     * <p>Why we structure Main this way:</p>
     * <ul>
     *   <li>Simple console application for academic demonstration</li>
     *   <li>Command-line argument support for flexible image input</li>
     *   <li>Clear separation between Main (orchestration) and PlateDetector (processing)</li>
     * </ul>
     *
     * @param args Command-line arguments.
     *             args[0] should contain the path to the input image.
     *             If no argument is provided, a default test image path is used.
     */
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  ALPR - Automatic License Plate Recognition  ");
        System.out.println("  Academic Project - v1.0                     ");
        System.out.println("==============================================");
        System.out.println();

        // Verify OpenCV is loaded correctly
        System.out.println("[INFO] OpenCV Version: " + Core.VERSION);
        System.out.println("[INFO] OpenCV loaded successfully!");
        System.out.println();

        // Determine the input image path
        // Why: Allow flexibility for testing with different images
        // Default path is provided for convenience during development
        String imagePath;
        if (args.length > 0) {
            imagePath = args[0];
        } else {
            // Default test image path - change this to your test image
            imagePath = "test_car.jpg";
            System.out.println("[INFO] No image path provided. Using default: " + imagePath);
        }

        // Check if the image file exists, if not generate a test image
        // Why: This allows the application to run without requiring an external image
        // Useful for initial testing and development
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            System.out.println("[WARN] Image file not found: " + imagePath);
            System.out.println("[INFO] Generating a synthetic test image...");

            if (!TestImageGenerator.generateTestImage(imagePath)) {
                System.err.println("[FAILURE] Could not generate test image!");
                System.exit(1);
            }
            System.out.println();
        }

        // Create the PlateDetector instance
        // Why: PlateDetector encapsulates all image processing logic
        // This separation makes the code modular and testable
        PlateDetector detector = new PlateDetector();

        // Execute the preprocessing pipeline
        // Why: Preprocessing prepares the image for license plate detection
        // The returned Mat contains the edge-detected image
        System.out.println("[STEP] Starting image preprocessing...\n");
        Mat processedImage = detector.preprocessImage(imagePath);

        // Verify preprocessing was successful
        if (processedImage != null && !processedImage.empty()) {
            System.out.println("\n[SUCCESS] Image preprocessing completed!");
            System.out.println("[INFO] Processed image size: " +
                    processedImage.cols() + "x" + processedImage.rows());
            System.out.println("[INFO] Check the project root for debug images:");
            System.out.println("       - step1_gray.jpg  (Grayscale conversion)");
            System.out.println("       - step2_blur.jpg  (Gaussian blur applied)");
            System.out.println("       - step3_canny.jpg (Canny edge detection)");
        } else {
            System.err.println("\n[FAILURE] Image preprocessing failed!");
            System.err.println("[HINT] Make sure the image file exists at: " + imagePath);
            System.exit(1);
        }

        System.out.println("\n==============================================");
        System.out.println("  Processing Complete                         ");
        System.out.println("==============================================");
    }
}
