package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.Arrays;

/**
 * Main - Entry Point for the ALPR (Automatic License Plate Recognition) System
 *
 * <p>This application demonstrates a complete pipeline for detecting
 * and reading license plates from vehicle images.</p>
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Load input image</li>
 *   <li>Detect license plate region using PlateDetector</li>
 *   <li>Extract text using OcrService</li>
 *   <li>Draw bounding box and save result</li>
 * </ol>
 *
 * @author ALPR Academic Project
 * @version 2.0
 */
public class Main {

    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp"};

    /**
     * Static initializer to load OpenCV native libraries.
     */
    static {
        try {
            OpenCV.loadLocally();
            System.out.println("[INFO] OpenCV loaded successfully");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load OpenCV: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Application entry point.
     *
     * @param args Command-line arguments. args[0] = image path (optional)
     */
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  ALPR - Automatic License Plate Recognition  ");
        System.out.println("==============================================");
        System.out.println("[INFO] OpenCV Version: " + Core.VERSION);
        System.out.println();

        // Determine input path (file or directory)
        String inputPath = args.length > 0 ? args[0] : "src/plates/34ERD58.jpg";
        File input = new File(inputPath);

        if (input.isDirectory()) {
            // Process all images in directory
            processDirectory(input);
        } else if (input.isFile()) {
            // Process single image
            processAndPrintResult(input.getAbsolutePath());
        } else {
            System.err.println("[ERROR] Invalid path: " + inputPath);
        }
    }

    /**
     * Processes all images in a directory.
     */
    private static void processDirectory(File directory) {
        File[] imageFiles = directory.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return Arrays.stream(IMAGE_EXTENSIONS).anyMatch(lowerName::endsWith);
        });

        if (imageFiles == null || imageFiles.length == 0) {
            System.err.println("[ERROR] No image files found in: " + directory.getPath());
            return;
        }

        System.out.println("[INFO] Found " + imageFiles.length + " images in: " + directory.getPath());
        System.out.println();

        int success = 0;
        int failed = 0;

        for (File imageFile : imageFiles) {
            System.out.println("----------------------------------------------");
            String result = processAndPrintResult(imageFile.getAbsolutePath());
            if (result != null && !result.isEmpty()) {
                success++;
            } else {
                failed++;
            }
            System.out.println();
        }

        // Print summary
        System.out.println("==============================================");
        System.out.println("                   SUMMARY                    ");
        System.out.println("==============================================");
        System.out.println("  Total images:  " + imageFiles.length);
        System.out.println("  Successful:    " + success);
        System.out.println("  Failed:        " + failed);
        System.out.println("==============================================");
    }

    /**
     * Processes a single image and prints the result.
     */
    private static String processAndPrintResult(String imagePath) {
        System.out.println("[INPUT] Image: " + imagePath);

        String detectedPlate = processImage(imagePath);

        if (detectedPlate != null && !detectedPlate.isEmpty()) {
            System.out.println("  >> Detected Plate: " + detectedPlate);
        } else {
            System.out.println("  >> No plate detected");
        }

        return detectedPlate;
    }

    /**
     * Processes an image through the full ALPR pipeline.
     */
    private static String processImage(String imagePath) {
        PlateDetector detector = new PlateDetector();
        OcrService ocrService = new OcrService();

        // Step 1: Preprocess
        Mat edgeImage = detector.preprocessImageWithOriginal(imagePath);
        if (edgeImage == null || edgeImage.empty()) {
            return null;
        }

        // Step 2: Find plate
        Mat plateRegion = detector.findPlateContour(edgeImage, detector.getOriginalImage());
        if (plateRegion == null || plateRegion.empty()) {
            return null;
        }

        // Step 3: OCR
        String plateText = ocrService.recognizePlate(plateRegion, detector.getCurrentImageName());

        // Step 4: Save result with bounding box
        Mat resultImage = detector.drawBoundingBox(new Scalar(0, 255, 0), 3);
        if (resultImage != null) {
            String fileName = new File(imagePath).getName();
            String outputPath = "result_" + fileName;
            Imgcodecs.imwrite(outputPath, resultImage);
            System.out.println("[OUTPUT] Saved: " + outputPath);
        }

        return plateText;
    }
}
