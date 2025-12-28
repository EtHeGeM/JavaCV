package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
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
 *   <li>Extract text using OCR</li>
 *   <li>Validate OCR result against expected plate (from filename)</li>
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
     * Stores validation results for accuracy reporting.
     *
     * <p>Why track validation results:</p>
     * <ul>
     *   <li>Academic projects require accuracy metrics</li>
     *   <li>Helps identify which plate types are problematic</li>
     *   <li>Enables comparison between different algorithm configurations</li>
     * </ul>
     */
    private static final List<ValidationResult> validationResults = new ArrayList<>();

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

        // Clear previous validation results
        validationResults.clear();

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
            boolean created = outputDir.mkdirs();
            if (!created) {
                System.err.println("[WARN] Could not create output directory");
            }
        }

        int detectionSuccessCount = 0;
        int detectionFailCount = 0;

        // Process each image
        for (int i = 0; i < imageFiles.size(); i++) {
            File imageFile = imageFiles.get(i);
            System.out.println("══════════════════════════════════════════════");
            System.out.println("[" + (i + 1) + "/" + imageFiles.size() + "] Processing: " + imageFile.getName());
            System.out.println("══════════════════════════════════════════════");

            // Extract expected plate from filename
            String expectedPlate = extractExpectedPlate(imageFile.getName());
            System.out.println("[EXPECTED] Plate from filename: " + expectedPlate);

            String detectedPlate = processImageWithValidation(
                    imageFile.getAbsolutePath(),
                    outputDir.getPath() + "/" + getBaseName(imageFile.getName()) + "_plate.jpg",
                    expectedPlate
            );

            if (detectedPlate != null) {
                detectionSuccessCount++;
                System.out.println("[RESULT] ✓ Plate detected: " + detectedPlate);
            } else {
                detectionFailCount++;
                System.out.println("[RESULT] ✗ No plate detected");
                // Record failed detection
                validationResults.add(new ValidationResult(
                        imageFile.getName(), expectedPlate, "", false, false
                ));
            }
            System.out.println();
        }

        // Print detailed summary with validation results
        printValidationSummary(imageFiles.size(), detectionSuccessCount, detectionFailCount);
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
     * <p>Processing Pipeline:</p>
     * <ol>
     *   <li>Preprocess image (grayscale, blur, edge detection)</li>
     *   <li>Find license plate contour</li>
     *   <li>Crop the plate region</li>
     *   <li>Perform OCR to extract text</li>
     * </ol>
     *
     * @param imagePath  Path to the input image
     * @param outputPath Path where the detected plate should be saved
     * @return true if a plate was detected and text extracted, false otherwise
     */
    private static boolean processImage(String imagePath, String outputPath) {
        PlateDetector detector = new PlateDetector();
        OcrService ocrService = new OcrService();

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

            // Step 3: Perform OCR on the detected plate
            System.out.println("\n[STEP 3] Performing OCR on detected plate...");
            String plateText = ocrService.recognizePlate(plateRegion);

            if (!plateText.isEmpty()) {
                System.out.println("╔════════════════════════════════════════════╗");
                System.out.println("║  DETECTED PLATE: " + String.format("%-24s", plateText) + " ║");
                System.out.println("╚════════════════════════════════════════════╝");
                return true;
            } else {
                System.out.println("[WARN] OCR could not extract text from plate.");
                return true; // Plate detected but OCR failed
            }
        }

        return false;
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
    private static String processImageWithValidation(String imagePath, String outputPath, String expectedPlate) {
        PlateDetector detector = new PlateDetector();
        OcrService ocrService = new OcrService();

        // Step 1: Preprocess the image
        Mat cannyImage = detector.preprocessImageWithOriginal(imagePath);

        if (cannyImage == null || cannyImage.empty()) {
            System.err.println("[ERROR] Failed to preprocess image!");
            return null;
        }

        // Step 2: Find license plate contour
        Mat plateRegion = detector.findPlateContour(cannyImage, detector.getOriginalImage());

        if (plateRegion != null && !plateRegion.empty()) {
            // Save the detected plate to the specified output path
            org.opencv.imgcodecs.Imgcodecs.imwrite(outputPath, plateRegion);
            System.out.println("[INFO] Plate saved to: " + outputPath);

            // Step 3: Perform OCR on the detected plate
            System.out.println("\n[STEP 3] Performing OCR on detected plate...");
            String detectedPlate = ocrService.recognizePlate(plateRegion);

            if (!detectedPlate.isEmpty()) {
                System.out.println("╔════════════════════════════════════════════╗");
                System.out.println("║  DETECTED PLATE: " + String.format("%-24s", detectedPlate) + " ║");
                System.out.println("╚════════════════════════════════════════════╝");

                // Step 4: Validate against expected plate
                validateAndRecord(imagePath, expectedPlate, detectedPlate);

                return detectedPlate;
            } else {
                System.out.println("[WARN] OCR could not extract text from plate.");
                // Record OCR failure
                validationResults.add(new ValidationResult(
                        new File(imagePath).getName(), expectedPlate, "[OCR FAILED]", true, false
                ));
                return "[OCR FAILED]";
            }
        }

        return null;
    }

    /**
     * Validates OCR result against expected plate and records the result.
     *
     * <p>Why validation is important:</p>
     * <ul>
     *   <li>Measures actual OCR accuracy, not just detection rate</li>
     *   <li>Identifies systematic errors (e.g., certain characters always misread)</li>
     *   <li>Provides metrics for academic project evaluation</li>
     * </ul>
     *
     * <p>Validation considers:</p>
     * <ul>
     *   <li>Exact match: detected plate equals expected plate</li>
     *   <li>Partial match: some characters match (for detailed analysis)</li>
     * </ul>
     *
     * @param imagePath     Path to the processed image
     * @param expectedPlate Expected plate from filename
     * @param detectedPlate OCR result
     */
    private static void validateAndRecord(String imagePath, String expectedPlate, String detectedPlate) {
        String fileName = new File(imagePath).getName();

        // Normalize both strings for comparison
        String normalizedExpected = expectedPlate.toUpperCase().replaceAll("[^A-Z0-9]", "");
        String normalizedDetected = detectedPlate.toUpperCase().replaceAll("[^A-Z0-9]", "");

        boolean exactMatch = normalizedExpected.equals(normalizedDetected);

        // Calculate character-level accuracy
        int matchingChars = countMatchingCharacters(normalizedExpected, normalizedDetected);
        double charAccuracy = normalizedExpected.isEmpty() ? 0 :
                (matchingChars * 100.0 / normalizedExpected.length());

        // Display validation result
        System.out.println("\n[STEP 4] Validation against filename...");
        System.out.println("┌────────────────────────────────────────────┐");
        System.out.println("│  EXPECTED:  " + String.format("%-29s", normalizedExpected) + " │");
        System.out.println("│  DETECTED:  " + String.format("%-29s", normalizedDetected) + " │");
        System.out.println("│  STATUS:    " + String.format("%-29s", (exactMatch ? "✓ EXACT MATCH" : "✗ MISMATCH")) + " │");
        System.out.println("│  CHAR ACC:  " + String.format("%-29s", String.format("%.1f%% (%d/%d chars)",
                charAccuracy, matchingChars, normalizedExpected.length())) + " │");
        System.out.println("└────────────────────────────────────────────┘");

        // Record result
        validationResults.add(new ValidationResult(
                fileName, normalizedExpected, normalizedDetected, true, exactMatch
        ));
    }

    /**
     * Counts matching characters between expected and detected plates.
     *
     * <p>Why character-level comparison:</p>
     * <ul>
     *   <li>Even if plates don't match exactly, partial matches are valuable</li>
     *   <li>Helps identify which character positions are problematic</li>
     *   <li>More granular accuracy metric for analysis</li>
     * </ul>
     *
     * @param expected Expected plate string
     * @param detected Detected plate string
     * @return Number of matching characters
     */
    private static int countMatchingCharacters(String expected, String detected) {
        int matches = 0;
        int minLength = Math.min(expected.length(), detected.length());

        for (int i = 0; i < minLength; i++) {
            if (expected.charAt(i) == detected.charAt(i)) {
                matches++;
            }
        }

        return matches;
    }

    /**
     * Extracts expected plate number from filename.
     *
     * <p>Filename format: "PLATE" or "PLATE_N" where:</p>
     * <ul>
     *   <li>PLATE = license plate number (e.g., "34ABC1234")</li>
     *   <li>_N = optional variant number for same plate different angle</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"34ERD58.png" → "34ERD58"</li>
     *   <li>"34ERD58_1.png" → "34ERD58"</li>
     *   <li>"34ERD58_2.jpg" → "34ERD58"</li>
     *   <li>"06YEV02.jpg" → "06YEV02"</li>
     * </ul>
     *
     * @param filename The image filename
     * @return Expected plate number
     */
    private static String extractExpectedPlate(String filename) {
        // Remove file extension
        String baseName = getBaseName(filename);

        // Remove variant suffix (_1, _2, _3, etc.)
        // Pattern: ends with _N where N is one or more digits
        String plateNumber = baseName.replaceAll("_\\d+$", "");

        return plateNumber.toUpperCase();
    }

    /**
     * Prints detailed validation summary with accuracy metrics.
     *
     * <p>Why detailed summary:</p>
     * <ul>
     *   <li>Provides overall system performance metrics</li>
     *   <li>Lists individual results for analysis</li>
     *   <li>Essential for academic project reports</li>
     * </ul>
     *
     * @param totalImages      Total number of images processed
     * @param detectionSuccess Number of successful plate detections
     * @param detectionFail    Number of failed plate detections
     */
    private static void printValidationSummary(int totalImages, int detectionSuccess, int detectionFail) {
        // Count validation statistics
        long exactMatches = validationResults.stream()
                .filter(ValidationResult::isExactMatch)
                .count();

        long ocrAttempts = validationResults.stream()
                .filter(ValidationResult::wasDetected)
                .count();

        // Calculate total character accuracy
        int totalExpectedChars = 0;
        int totalMatchingChars = 0;
        for (ValidationResult result : validationResults) {
            if (result.wasDetected() && !result.detectedPlate().equals("[OCR FAILED]")) {
                totalExpectedChars += result.expectedPlate().length();
                totalMatchingChars += countMatchingCharacters(result.expectedPlate(), result.detectedPlate());
            }
        }
        double overallCharAccuracy = totalExpectedChars > 0 ?
                (totalMatchingChars * 100.0 / totalExpectedChars) : 0;

        // Print summary
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    VALIDATION SUMMARY                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  DETECTION METRICS:                                          ║");
        System.out.println("║    Total Images:        " + String.format("%-36s", totalImages) + " ║");
        System.out.println("║    Plates Detected:     " + String.format("%-36s", detectionSuccess) + " ║");
        System.out.println("║    Detection Failed:    " + String.format("%-36s", detectionFail) + " ║");
        System.out.println("║    Detection Rate:      " + String.format("%-36s",
                String.format("%.1f%%", (detectionSuccess * 100.0 / totalImages))) + " ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  OCR ACCURACY METRICS:                                       ║");
        System.out.println("║    OCR Attempts:        " + String.format("%-36s", ocrAttempts) + " ║");
        System.out.println("║    Exact Matches:       " + String.format("%-36s", exactMatches) + " ║");
        System.out.println("║    Exact Match Rate:    " + String.format("%-36s",
                String.format("%.1f%%", (ocrAttempts > 0 ? exactMatches * 100.0 / ocrAttempts : 0))) + " ║");
        System.out.println("║    Character Accuracy:  " + String.format("%-36s",
                String.format("%.1f%% (%d/%d chars)", overallCharAccuracy, totalMatchingChars, totalExpectedChars)) + " ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  DETAILED RESULTS:                                           ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        // Print each result
        for (ValidationResult result : validationResults) {
            String status;
            if (!result.wasDetected()) {
                status = "NO DETECT";
            } else if (result.detectedPlate().equals("[OCR FAILED]")) {
                status = "OCR FAIL ";
            } else if (result.isExactMatch()) {
                status = "✓ MATCH  ";
            } else {
                status = "✗ WRONG  ";
            }

            String line = String.format("║  %s │ Expected: %-10s │ Got: %-10s ║",
                    status,
                    result.expectedPlate().length() > 10 ?
                            result.expectedPlate().substring(0, 10) : result.expectedPlate(),
                    result.detectedPlate().length() > 10 ?
                            result.detectedPlate().substring(0, 10) : result.detectedPlate()
            );
            System.out.println(line);
        }

        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("[INFO] Detected plates saved to: detected_plates/");
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

    /**
     * Record class to store validation results.
     *
     * <p>Why use a record:</p>
     * <ul>
     *   <li>Immutable data carrier - results shouldn't change after recording</li>
     *   <li>Clean syntax for data-only classes (Java 16+)</li>
     *   <li>Auto-generated equals, hashCode, toString</li>
     * </ul>
     *
     * @param fileName      Name of the processed image file
     * @param expectedPlate Expected plate from filename
     * @param detectedPlate OCR result (or error message)
     * @param wasDetected   Whether plate region was detected
     * @param isExactMatch  Whether OCR result exactly matches expected
     */
    private record ValidationResult(
            String fileName,
            String expectedPlate,
            String detectedPlate,
            boolean wasDetected,
            boolean isExactMatch
    ) {}
}
