package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
 *   <li>Detect license plate region</li>
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
     * <p>Why OpenCV.loadLocally():</p>
     * <ul>
     *   <li>loadShared() is deprecated in Java 12+ and causes SEVERE warnings</li>
     *   <li>loadLocally() loads the native library from the JAR automatically</li>
     *   <li>This is the recommended approach for modern Java versions</li>
     *   <li>Avoids "Restricted method" warnings in Java 17+</li>
     * </ul>
     */
    static {
        try {
            OpenCV.loadLocally();
            System.out.println("[DEBUG] OpenCV loaded successfully using loadLocally()");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load OpenCV: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Supported image file extensions for batch processing.
     */
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".bmp", ".tiff"
    );

    /**
     * Application entry point.
     *
     * @param args Command-line arguments.
     *             args[0] can be a single image path or a directory path.
     *             If a directory is provided, all images in it will be processed.
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

        // Determine the input path
        String inputPath;
        if (args.length > 0) {
            inputPath = args[0];
        } else {
            // Default to plates directory for batch processing
            inputPath = "src/plates";
            System.out.println("[INFO] No path provided. Using default: " + inputPath);
        }

        File inputFile = new File(inputPath);

        if (inputFile.isDirectory()) {
            // Batch process all images in the directory
            processBatch(inputFile);
        } else if (inputFile.exists()) {
            // Process single image
            processSingleImage(inputPath);
        } else {
            System.err.println("[ERROR] Path not found: " + inputPath);
            System.exit(1);
        }

        System.out.println("\n==============================================");
        System.out.println("  Processing Complete                         ");
        System.out.println("==============================================");
    }

    /**
     * Processes all images in a directory (batch mode).
     *
     * <p>Why batch processing:</p>
     * <ul>
     *   <li>Allows testing the algorithm on multiple images at once</li>
     *   <li>Useful for evaluating detection accuracy across different scenarios</li>
     *   <li>Saves time during development and testing</li>
     * </ul>
     *
     * @param directory The directory containing images to process
     */
    private static void processBatch(File directory) {
        System.out.println("[MODE] Batch Processing Mode");
        System.out.println("[INFO] Scanning directory: " + directory.getAbsolutePath());
        System.out.println();

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            System.err.println("[ERROR] No files found in directory!");
            return;
        }

        // Filter image files
        List<File> imageFiles = Arrays.stream(files)
                .filter(f -> f.isFile() && isImageFile(f.getName()))
                .toList();

        System.out.println("[INFO] Found " + imageFiles.size() + " image(s) to process");
        System.out.println();

        // Create output directory for detected plates
        File outputDir = new File("detected_plates");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int successCount = 0;
        int failCount = 0;

        // Process each image
        for (int i = 0; i < imageFiles.size(); i++) {
            File imageFile = imageFiles.get(i);
            System.out.println("══════════════════════════════════════════════");
            System.out.println("[" + (i + 1) + "/" + imageFiles.size() + "] Processing: " + imageFile.getName());
            System.out.println("══════════════════════════════════════════════");

            boolean success = processImage(imageFile.getAbsolutePath(),
                    outputDir.getPath() + "/" + getBaseName(imageFile.getName()) + "_plate.jpg");

            if (success) {
                successCount++;
                System.out.println("[RESULT] ✓ Plate detected successfully!");
            } else {
                failCount++;
                System.out.println("[RESULT] ✗ No plate detected");
            }
            System.out.println();
        }

        // Print summary
        System.out.println("══════════════════════════════════════════════");
        System.out.println("                    SUMMARY                    ");
        System.out.println("══════════════════════════════════════════════");
        System.out.println("[TOTAL]    Images processed: " + imageFiles.size());
        System.out.println("[SUCCESS]  Plates detected:  " + successCount);
        System.out.println("[FAILED]   No plate found:   " + failCount);
        System.out.println("[ACCURACY] Detection rate:   " +
                String.format("%.1f%%", (successCount * 100.0 / imageFiles.size())));
        System.out.println();
        System.out.println("[INFO] Detected plates saved to: " + outputDir.getAbsolutePath());
    }

    /**
     * Processes a single image file.
     *
     * @param imagePath Path to the image file
     */
    private static void processSingleImage(String imagePath) {
        System.out.println("[MODE] Single Image Mode");
        System.out.println();
        processImage(imagePath, "step4_detected_plate.jpg");
    }

    /**
     * Core image processing logic - detects license plate in an image.
     *
     * @param imagePath  Path to the input image
     * @param outputPath Path where the detected plate should be saved
     * @return true if a plate was detected, false otherwise
     */
    private static boolean processImage(String imagePath, String outputPath) {
        PlateDetector detector = new PlateDetector();

        // Step 1: Preprocess the image
        Mat cannyImage = detector.preprocessImageWithOriginal(imagePath);

        if (cannyImage == null || cannyImage.empty()) {
            System.err.println("[ERROR] Failed to preprocess image!");
            return false;
        }

        // Step 2: Find license plate contour
        Mat plateRegion = detector.findPlateContour(cannyImage, detector.getOriginalImage());

        if (plateRegion != null && !plateRegion.empty()) {
            // Save the detected plate to the specified output path
            org.opencv.imgcodecs.Imgcodecs.imwrite(outputPath, plateRegion);
            System.out.println("[INFO] Plate saved to: " + outputPath);
            return true;
        }

        return false;
    }

    /**
     * Checks if a filename has a supported image extension.
     *
     * @param filename The filename to check
     * @return true if the file is a supported image format
     */
    private static boolean isImageFile(String filename) {
        String lowerName = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }

    /**
     * Extracts the base name from a filename (without extension).
     *
     * @param filename The filename
     * @return The base name without extension
     */
    private static String getBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}
