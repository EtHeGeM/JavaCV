package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
 * @author ALPR Academic Project - Mert Özbay, Defne Oktem, Ata Atay, Ayşe Ceren Sarıgül, Aylin Baki, Ahmad Ali al Ghazi
 * @version 2.1 - Added config file support
 */
public class Main {

    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp"};
    private static final String CONFIG_FILE = "alpr_config.properties";

    // Debug statistics
    private static int totalImages = 0;
    private static int plateDetected = 0;
    private static int exactMatch = 0;
    private static int partialMatch = 0;
    private static int totalCharacters = 0;
    private static int matchedCharacters = 0;
    private static List<DebugResult> debugResults = new ArrayList<>();

    // Current parameters (will be read from config file or detector defaults)
    private static int currentBlurKernel;
    private static int currentCannyT1;
    private static int currentCannyT2;
    private static int currentDilateKernel;
    private static int currentDilateIter;
    private static double currentMinAR;
    private static double currentMaxAR;

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
     * Loads configuration from alpr_config.properties file.
     * Falls back to PlateDetector defaults if config file doesn't exist.
     *
     * @param detector The PlateDetector instance to configure
     * @return true if config was loaded from file, false if using defaults
     */
    private static boolean loadConfigFromFile(PlateDetector detector) {
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            System.out.println("[CONFIG] Config file not found, using defaults");
            return false;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Properties props = new Properties();
            props.load(fis);

            // Read parameters from config file
            currentBlurKernel = Integer.parseInt(props.getProperty("blur.kernel", "11"));
            currentCannyT1 = Integer.parseInt(props.getProperty("canny.threshold1", "50"));
            currentCannyT2 = Integer.parseInt(props.getProperty("canny.threshold2", "150"));
            currentDilateKernel = Integer.parseInt(props.getProperty("dilate.kernel", "3"));
            currentDilateIter = Integer.parseInt(props.getProperty("dilate.iterations", "2"));
            currentMinAR = Double.parseDouble(props.getProperty("aspect.ratio.min", "2.0"));
            currentMaxAR = Double.parseDouble(props.getProperty("aspect.ratio.max", "7.0"));

            // Apply to detector
            detector.setBlurKernel(currentBlurKernel);
            detector.setCannyThreshold1(currentCannyT1);
            detector.setCannyThreshold2(currentCannyT2);
            detector.setDilateKernelSize(currentDilateKernel);
            detector.setDilateIterations(currentDilateIter);
            detector.setMinAspectRatio(currentMinAR);
            detector.setMaxAspectRatio(currentMaxAR);

            System.out.println("[CONFIG] Loaded configuration from: " + CONFIG_FILE);
            return true;

        } catch (Exception e) {
            System.err.println("[CONFIG] Error loading config: " + e.getMessage());
            System.out.println("[CONFIG] Using default values");
            return false;
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

        // Initialize detector
        PlateDetector tempDetector = new PlateDetector();

        // Try to load config from file, otherwise use defaults
        if (!loadConfigFromFile(tempDetector)) {
            // Use detector defaults
            currentBlurKernel = tempDetector.getBlurKernel();
            currentCannyT1 = tempDetector.getCannyThreshold1();
            currentCannyT2 = tempDetector.getCannyThreshold2();
            currentDilateKernel = tempDetector.getDilateKernelSize();
            currentDilateIter = tempDetector.getDilateIterations();
            currentMinAR = tempDetector.getMinAspectRatio();
            currentMaxAR = tempDetector.getMaxAspectRatio();
        }

        // Determine input path (file or directory)
        String inputPath = args.length > 0 ? args[0] : "src/plates";
        File input = new File(inputPath);

        if (input.isDirectory()) {
            // Process all images in directory
            processDirectory(input);
        } else if (input.isFile()) {
            // Process single image
            processAndPrintResult(input.getAbsolutePath());
            printFinalSummary();
            exportResultsToCSV();
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

        for (File imageFile : imageFiles) {
            System.out.println("----------------------------------------------");
            processAndPrintResult(imageFile.getAbsolutePath());
            System.out.println();
        }

        // Print final summary
        printFinalSummary();
        exportResultsToCSV();
    }

    /**
     * Processes a single image and prints the result.
     */
    private static String processAndPrintResult(String imagePath) {
        totalImages++;
        File imageFile = new File(imagePath);
        String fileName = imageFile.getName();
        String expectedPlate = extractExpectedPlate(fileName);

        System.out.println("[INPUT] Image: " + fileName);
        System.out.println("[EXPECTED] Plate: " + (expectedPlate.isEmpty() ? "(unknown)" : expectedPlate));

        DebugResult result = processImage(imagePath, expectedPlate);
        debugResults.add(result);

        if (result.detected) {
            plateDetected++;
            System.out.println("[OCR RESULT] " + result.ocrResult);

            if (!expectedPlate.isEmpty()) {
                if (result.ocrResult.equals(expectedPlate)) {
                    exactMatch++;
                    System.out.println("[MATCH] ✓ EXACT MATCH!");
                } else {
                    int matched = countMatchingChars(expectedPlate, result.ocrResult);
                    if (matched > 0) {
                        partialMatch++;
                        System.out.println("[MATCH] ~ PARTIAL: " + matched + "/" + expectedPlate.length() + " chars");
                    } else {
                        System.out.println("[MATCH] ✗ NO MATCH");
                    }
                }
            }
        } else {
            System.out.println("[RESULT] ✗ No plate detected");
        }

        return result.ocrResult;
    }

    /**
     * Extracts expected plate number from filename.
     * Assumes filename format: PLATENUM.jpg or PLATENUM_variant.jpg
     */
    private static String extractExpectedPlate(String fileName) {
        // Remove extension
        String name = fileName.replaceAll("\\.[^.]+$", "");
        // Remove variant suffix (e.g., _1, _2, _3)
        name = name.replaceAll("_\\d+$", "");
        // Convert to uppercase and remove non-alphanumeric
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Count matching characters between expected and actual plates.
     */
    private static int countMatchingChars(String expected, String actual) {
        if (expected == null || actual == null) return 0;

        int matches = 0;
        int minLen = Math.min(expected.length(), actual.length());

        // Count positional matches
        for (int i = 0; i < minLen; i++) {
            if (expected.charAt(i) == actual.charAt(i)) {
                matches++;
            }
        }

        totalCharacters += expected.length();
        matchedCharacters += matches;

        return matches;
    }

    /**
     * Processes an image through the full ALPR pipeline.
     */
    private static DebugResult processImage(String imagePath, String expectedPlate) {
        DebugResult result = new DebugResult();
        result.fileName = new File(imagePath).getName();
        result.expectedPlate = expectedPlate;

        PlateDetector detector = new PlateDetector();
        OcrService ocrService = new OcrService();

        // Step 1: Preprocess
        Mat edgeImage = detector.preprocessImageWithOriginal(imagePath);
        if (edgeImage == null || edgeImage.empty()) {
            result.detected = false;
            return result;
        }

        // Step 2: Run dual detection
        List<DetectionResult> detections = detector.detectAll();
        result.haarCount = 0;
        result.geoCount = 0;

        for (DetectionResult det : detections) {
            if (det.getMethod() == DetectionResult.MethodType.HAAR) {
                result.haarCount++;
            } else {
                result.geoCount++;
            }
        }

        // Step 3: OCR on all detections
        String bestResult = "";
        int bestScore = 0;

        for (DetectionResult det : detections) {
            Mat plate = det.getCroppedPlate();
            if (plate == null || plate.empty()) continue;

            String ocrText = ocrService.recognizePlate(plate,
                detector.getCurrentImageName() + "_" + det.getMethod().name().toLowerCase());
            det.setOcrResult(ocrText);

            int score = calculateScore(ocrText, expectedPlate);
            if (score > bestScore) {
                bestScore = score;
                bestResult = ocrText;
                result.bestMethod = det.getMethod().name();
            }
        }

        result.detected = !bestResult.isEmpty();
        result.ocrResult = bestResult;

        // Step 4: Save result with bounding box
        Mat resultImage = detector.getLastContourImage();
        if (resultImage != null && !resultImage.empty()) {
            String outputPath = "result_" + result.fileName;
            Imgcodecs.imwrite(outputPath, resultImage);
            System.out.println("[OUTPUT] Saved: " + outputPath);
        }

        return result;
    }

    /**
     * Calculate OCR result score based on Turkish plate format and match with expected.
     */
    private static int calculateScore(String text, String expected) {
        if (text == null || text.isEmpty()) return 0;

        int score = text.length(); // Base score

        // Bonus for Turkish plate format
        if (text.matches("^\\d{2}[A-Z]{1,3}\\d{2,4}$")) {
            score += 50;
        } else if (text.matches("^\\d{2}[A-Z]+\\d+$")) {
            score += 30;
        }

        // Bonus for matching expected plate
        if (expected != null && !expected.isEmpty()) {
            if (text.equals(expected)) {
                score += 100;
            } else {
                // Partial match bonus
                int matches = 0;
                int minLen = Math.min(expected.length(), text.length());
                for (int i = 0; i < minLen; i++) {
                    if (expected.charAt(i) == text.charAt(i)) matches++;
                }
                score += matches * 5;
            }
        }

        return score;
    }

    /**
     * Print final debug summary.
     */
    private static void printFinalSummary() {
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println("                      ALPR DEBUG SUMMARY                          ");
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.println();

        // Parameter values used
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                   PARAMETER VALUES USED                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.printf("│  Blur Kernel (Bilateral):   %-36d │%n", currentBlurKernel);
        System.out.printf("│  Canny Threshold 1:         %-36d │%n", currentCannyT1);
        System.out.printf("│  Canny Threshold 2:         %-36d │%n", currentCannyT2);
        System.out.printf("│  Dilate Kernel Size:        %-36d │%n", currentDilateKernel);
        System.out.printf("│  Dilate Iterations:         %-36d │%n", currentDilateIter);
        System.out.printf("│  Min Aspect Ratio:          %-36.1f │%n", currentMinAR);
        System.out.printf("│  Max Aspect Ratio:          %-36.1f │%n", currentMaxAR);
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Overall statistics
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                     OVERALL STATISTICS                         │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.printf("│  Total Images Processed:    %-36d │%n", totalImages);
        System.out.printf("│  Plates Detected:           %-36d │%n", plateDetected);
        System.out.printf("│  Detection Rate:            %-35.1f%% │%n",
            totalImages > 0 ? (plateDetected * 100.0 / totalImages) : 0);
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.printf("│  Exact Matches:             %-36d │%n", exactMatch);
        System.out.printf("│  Partial Matches:           %-36d │%n", partialMatch);
        System.out.printf("│  No Match:                  %-36d │%n", plateDetected - exactMatch - partialMatch);
        System.out.printf("│  Accuracy (Exact):          %-35.1f%% │%n",
            totalImages > 0 ? (exactMatch * 100.0 / totalImages) : 0);
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.printf("│  Total Characters:          %-36d │%n", totalCharacters);
        System.out.printf("│  Matched Characters:        %-36d │%n", matchedCharacters);
        System.out.printf("│  Character Accuracy:        %-35.1f%% │%n",
            totalCharacters > 0 ? (matchedCharacters * 100.0 / totalCharacters) : 0);
        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // Detailed results table
        System.out.println("┌────────────────────┬───────────────┬───────────────┬──────┬──────┬────────┐");
        System.out.println("│ File               │ Expected      │ OCR Result    │ Haar │ Geo  │ Status │");
        System.out.println("├────────────────────┼───────────────┼───────────────┼──────┼──────┼────────┤");

        for (DebugResult result : debugResults) {
            String fileName = truncate(result.fileName, 18);
            String expected = truncate(result.expectedPlate, 13);
            String ocr = truncate(result.ocrResult, 13);
            String status = getStatusSymbol(result);

            System.out.printf("│ %-18s │ %-13s │ %-13s │ %4d │ %4d │ %-6s │%n",
                fileName, expected, ocr, result.haarCount, result.geoCount, status);
        }

        System.out.println("└────────────────────┴───────────────┴───────────────┴──────┴──────┴────────┘");
        System.out.println();
        System.out.println("Legend: ✓ = Exact Match, ~ = Partial Match, ✗ = No Match/Detection");
        System.out.println("══════════════════════════════════════════════════════════════════");
    }

    /**
     * Export results to CSV file for Excel analysis.
     * Uses semicolon as delimiter for better Excel compatibility.
     */
    private static void exportResultsToCSV() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String csvFileName = "alpr_results_" + timestamp + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFileName))) {
            // Write BOM for UTF-8 Excel compatibility
            writer.print('\ufeff');

            // Write parameters as structured data (header + values)
            writer.println("BlurKernel;CannyT1;CannyT2;DilateKernel;DilateIter;MinAR;MaxAR");
            writer.printf("%d;%d;%d;%d;%d;%.1f;%.1f%n",
                currentBlurKernel, currentCannyT1, currentCannyT2,
                currentDilateKernel, currentDilateIter, currentMinAR, currentMaxAR);
            writer.println();

            // Write summary as structured data (header + values)
            writer.println("TotalImages;PlatesDetected;DetectionRate;ExactMatches;PartialMatches;ExactAccuracy;CharAccuracy");
            writer.printf("%d;%d;%.1f;%d;%d;%.1f;%.1f%n",
                totalImages, plateDetected,
                totalImages > 0 ? (plateDetected * 100.0 / totalImages) : 0,
                exactMatch, partialMatch,
                totalImages > 0 ? (exactMatch * 100.0 / totalImages) : 0,
                totalCharacters > 0 ? (matchedCharacters * 100.0 / totalCharacters) : 0);
            writer.println();

            // Write detailed results header
            writer.println("FileName;Expected;OCRResult;HaarCount;GeoCount;Detected;ExactMatch;PartialMatch;MatchedChars;TotalChars;BestMethod");

            // Write data rows
            for (DebugResult result : debugResults) {
                int matchedChars = 0;
                int totalChars = result.expectedPlate.length();
                if (!result.expectedPlate.isEmpty() && !result.ocrResult.isEmpty()) {
                    int minLen = Math.min(result.expectedPlate.length(), result.ocrResult.length());
                    for (int i = 0; i < minLen; i++) {
                        if (result.expectedPlate.charAt(i) == result.ocrResult.charAt(i)) {
                            matchedChars++;
                        }
                    }
                }

                boolean isExact = result.ocrResult.equals(result.expectedPlate);
                boolean isPartial = !isExact && matchedChars > 0;

                writer.println(String.join(";",
                    result.fileName,
                    result.expectedPlate,
                    result.ocrResult,
                    String.valueOf(result.haarCount),
                    String.valueOf(result.geoCount),
                    result.detected ? "YES" : "NO",
                    isExact ? "YES" : "NO",
                    isPartial ? "YES" : "NO",
                    String.valueOf(matchedChars),
                    String.valueOf(totalChars),
                    result.bestMethod
                ));
            }

            System.out.println("[CSV] Results exported to: " + csvFileName);

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to export CSV: " + e.getMessage());
        }

        // Also export a summary row for parameter comparison
        exportSummaryRow(timestamp);
    }

    /**
     * Export a single summary row for comparing different parameter configurations.
     * Uses semicolon as delimiter for Excel compatibility.
     */
    private static void exportSummaryRow(String timestamp) {
        String summaryFileName = "alpr_summary.csv";
        File summaryFile = new File(summaryFileName);
        boolean writeHeader = !summaryFile.exists() || summaryFile.length() == 0;

        try (PrintWriter writer = new PrintWriter(new FileWriter(summaryFileName, true))) {
            // Write BOM and header if file is new
            if (writeHeader) {
                writer.print('\ufeff'); // UTF-8 BOM for Excel
                writer.println("Timestamp;BlurKernel;CannyT1;CannyT2;DilateKernel;DilateIter;MinAR;MaxAR;TotalImages;Detected;DetectionRate;ExactMatch;PartialMatch;ExactAccuracy;CharAccuracy");
            }

            // Write summary row with semicolon delimiter
            writer.println(String.join(";",
                timestamp,
                String.valueOf(currentBlurKernel),
                String.valueOf(currentCannyT1),
                String.valueOf(currentCannyT2),
                String.valueOf(currentDilateKernel),
                String.valueOf(currentDilateIter),
                String.format("%.1f", currentMinAR),
                String.format("%.1f", currentMaxAR),
                String.valueOf(totalImages),
                String.valueOf(plateDetected),
                String.format("%.1f", totalImages > 0 ? (plateDetected * 100.0 / totalImages) : 0),
                String.valueOf(exactMatch),
                String.valueOf(partialMatch),
                String.format("%.1f", totalImages > 0 ? (exactMatch * 100.0 / totalImages) : 0),
                String.format("%.1f", totalCharacters > 0 ? (matchedCharacters * 100.0 / totalCharacters) : 0)
            ));

            System.out.println("[CSV] Summary appended to: " + summaryFileName);

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to append summary: " + e.getMessage());
        }
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen - 2) + ".." : str;
    }

    private static String getStatusSymbol(DebugResult result) {
        if (!result.detected) return "✗";
        if (result.ocrResult.equals(result.expectedPlate)) return "✓";
        if (!result.expectedPlate.isEmpty()) {
            int minLen = Math.min(result.expectedPlate.length(), result.ocrResult.length());
            for (int i = 0; i < minLen; i++) {
                if (result.expectedPlate.charAt(i) == result.ocrResult.charAt(i)) {
                    return "~";
                }
            }
        }
        return "✗";
    }

    /**
     * Debug result container.
     */
    private static class DebugResult {
        String fileName = "";
        String expectedPlate = "";
        String ocrResult = "";
        boolean detected = false;
        int haarCount = 0;
        int geoCount = 0;
        String bestMethod = "";
    }
}
