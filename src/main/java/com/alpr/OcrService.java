package com.alpr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OcrService - Optical Character Recognition Service for License Plates
 *
 * <p>This class wraps the Tesseract OCR engine (via Tess4J) to extract
 * text from cropped license plate images. It is specifically configured
 * for reading license plate characters.</p>
 *
 * <p>Why Tesseract for ALPR:</p>
 * <ul>
 *   <li>Open-source and well-maintained OCR engine</li>
 *   <li>Supports character whitelisting for improved accuracy</li>
 *   <li>Can be trained for specific fonts (license plate fonts)</li>
 *   <li>Works well with preprocessed binary images</li>
 * </ul>
 *
 * <p>Configuration Details:</p>
 * <ul>
 *   <li>Whitelist: Only uppercase A-Z and digits 0-9 are recognized</li>
 *   <li>Page Segmentation Mode: Single line of text (PSM 7)</li>
 *   <li>Engine Mode: LSTM neural network for better accuracy</li>
 * </ul>
 *
 * @author ALPR Academic Project
 * @version 1.0
 */
public class OcrService {

    /**
     * Tesseract OCR instance.
     *
     * <p>Why single instance:</p>
     * <ul>
     *   <li>Tesseract initialization is expensive (loads trained data)</li>
     *   <li>Reusing the instance improves performance for batch processing</li>
     *   <li>Configuration is set once during initialization</li>
     * </ul>
     */
    private final Tesseract tesseract;

    /**
     * Character whitelist for license plate OCR.
     *
     * <p>Why whitelist only A-Z and 0-9:</p>
     * <ul>
     *   <li>License plates contain ONLY uppercase letters and numbers</li>
     *   <li>Prevents misreading characters (e.g., 'l' vs '1', 'o' vs '0')</li>
     *   <li>Significantly improves recognition accuracy</li>
     *   <li>Turkish plates use format: 34 ABC 1234 (city code + letters + numbers)</li>
     * </ul>
     */
    private static final String WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Path to tessdata directory containing trained language data.
     *
     * <p>Default path assumes tessdata is in project root or system path.</p>
     */
    private String tessdataPath;

    /**
     * Temporary file prefix for saving Mat images before OCR processing.
     */
    private static final String TEMP_FILE_PREFIX = "alpr_ocr_temp_";

    /**
     * Creates a new OcrService with default tessdata path.
     *
     * <p>Default initialization uses system tessdata path or current directory.</p>
     */
    public OcrService() {
        this(null);
    }

    /**
     * Creates a new OcrService with specified tessdata path.
     *
     * <p>Why configurable tessdata path:</p>
     * <ul>
     *   <li>Allows deployment flexibility (different environments)</li>
     *   <li>Supports custom trained data for license plates</li>
     *   <li>Enables using different language models</li>
     * </ul>
     *
     * @param tessdataPath Path to tessdata directory, or null for default
     */
    public OcrService(String tessdataPath) {
        this.tessdataPath = tessdataPath;
        this.tesseract = new Tesseract();
        initializeTesseract();
    }

    /**
     * Initializes and configures the Tesseract OCR engine.
     *
     * <p>Configuration choices explained:</p>
     * <ul>
     *   <li><b>Language "eng":</b> English model works well for alphanumeric plates</li>
     *   <li><b>PSM 7:</b> Treats image as a single text line (ideal for plates)</li>
     *   <li><b>OEM 3:</b> Uses LSTM neural network engine for best accuracy</li>
     *   <li><b>Whitelist:</b> Restricts output to valid plate characters only</li>
     * </ul>
     */
    private void initializeTesseract() {
        System.out.println("[OCR] Initializing Tesseract OCR engine...");

        // Set tessdata path if provided
        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            tesseract.setDatapath(tessdataPath);
            System.out.println("[OCR] Tessdata path: " + tessdataPath);
        } else {
            // Try common default paths
            String defaultPath = findTessdataPath();
            if (defaultPath != null) {
                tesseract.setDatapath(defaultPath);
                System.out.println("[OCR] Using detected tessdata path: " + defaultPath);
            }
        }

        // Set language to English (works for alphanumeric license plates)
        tesseract.setLanguage("eng");

        /*
         * Page Segmentation Modes (PSM) explanation:
         * 0  = Orientation and script detection (OSD) only
         * 1  = Automatic page segmentation with OSD
         * 3  = Fully automatic page segmentation (default)
         * 6  = Assume a single uniform block of text
         * 7  = Treat the image as a single text line <-- BEST FOR LICENSE PLATES
         * 8  = Treat the image as a single word
         * 13 = Raw line (no binarization)
         *
         * Why PSM 7: License plates are typically a single line of text,
         * this mode optimizes recognition for that specific layout.
         */
        tesseract.setPageSegMode(7);

        /*
         * OCR Engine Modes (OEM) explanation:
         * 0 = Legacy engine only
         * 1 = Neural nets LSTM engine only
         * 2 = Legacy + LSTM engines
         * 3 = Default, based on what is available <-- RECOMMENDED
         *
         * Why OEM 3: Automatically uses the best available engine,
         * typically LSTM which provides superior accuracy.
         */
        tesseract.setOcrEngineMode(3);

        /*
         * Character Whitelist Configuration:
         * By setting tessedit_char_whitelist, we tell Tesseract to ONLY
         * recognize characters in this list. Any other character detected
         * will be forced to match one of these.
         *
         * This dramatically improves accuracy for license plates because:
         * - No lowercase letters on plates
         * - No special characters on plates
         * - Prevents common misreadings like 'O' vs '0', 'I' vs '1'
         */
        tesseract.setVariable("tessedit_char_whitelist", WHITELIST);

        System.out.println("[OCR] Whitelist configured: " + WHITELIST);
        System.out.println("[OCR] Tesseract initialization complete.");
    }

    /**
     * Attempts to find tessdata path in common locations.
     *
     * <p>Search order:</p>
     * <ol>
     *   <li>Project's tessdata directory</li>
     *   <li>TESSDATA_PREFIX environment variable</li>
     *   <li>Common Windows installation paths</li>
     *   <li>Common Linux/Mac installation paths</li>
     * </ol>
     *
     * @return Found tessdata path, or null if not found
     */
    private String findTessdataPath() {
        // Check project directory first - use absolute path for reliability
        String projectDir = System.getProperty("user.dir");

        String[] possiblePaths = {
            projectDir + "/tessdata",                    // Project root tessdata
            projectDir + "\\tessdata",                   // Windows style
            "tessdata",                                  // Relative path
            "src/main/resources/tessdata",
            System.getenv("TESSDATA_PREFIX"),
            "C:/Program Files/Tesseract-OCR/tessdata",
            "C:/Program Files (x86)/Tesseract-OCR/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tesseract-ocr/tessdata",
            "/usr/local/share/tessdata"
        };

        for (String path : possiblePaths) {
            if (path != null) {
                File tessdataDir = new File(path);
                File engFile = new File(path, "eng.traineddata");
                if (tessdataDir.exists() && engFile.exists()) {
                    System.out.println("[OCR] Found tessdata at: " + tessdataDir.getAbsolutePath());
                    return tessdataDir.getAbsolutePath();
                }
            }
        }

        System.out.println("[OCR WARNING] Tessdata path not found. Using Tess4J bundled data.");
        return null;
    }

    /**
     * Performs OCR on a cropped license plate image (OpenCV Mat).
     *
     * <p>Why this approach:</p>
     * <ul>
     *   <li>Tess4J requires a File or BufferedImage, not OpenCV Mat</li>
     *   <li>We save the Mat temporarily, perform OCR, then clean up</li>
     *   <li>Temporary file ensures compatibility across all platforms</li>
     * </ul>
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Validate input Mat is not null or empty</li>
     *   <li>Save Mat to temporary file</li>
     *   <li>Run Tesseract OCR on the file</li>
     *   <li>Clean up temporary file</li>
     *   <li>Post-process and return result</li>
     * </ol>
     *
     * @param plateMat The cropped license plate image as OpenCV Mat
     * @return Recognized text from the plate, or empty string if failed
     */
    public String recognizePlate(Mat plateMat) {
        // Validate input
        if (plateMat == null || plateMat.empty()) {
            System.err.println("[OCR ERROR] Input Mat is null or empty!");
            return "";
        }

        System.out.println("[OCR] Starting plate recognition...");
        System.out.println("[OCR] Input image size: " + plateMat.width() + "x" + plateMat.height());

        Path tempFile = null;
        try {
            // Create temporary file for the plate image
            tempFile = Files.createTempFile(TEMP_FILE_PREFIX, ".png");
            String tempPath = tempFile.toAbsolutePath().toString();

            // Save Mat to temporary file
            boolean saved = Imgcodecs.imwrite(tempPath, plateMat);
            if (!saved) {
                System.err.println("[OCR ERROR] Failed to save temporary image!");
                return "";
            }
            System.out.println("[OCR] Temporary file created: " + tempPath);

            // Perform OCR
            String rawResult = tesseract.doOCR(new File(tempPath));

            // Post-process the result
            String cleanedResult = postProcessOcrResult(rawResult);

            System.out.println("[OCR] Raw result: \"" + rawResult.trim() + "\"");
            System.out.println("[OCR] Cleaned result: \"" + cleanedResult + "\"");

            return cleanedResult;

        } catch (TesseractException e) {
            System.err.println("[OCR ERROR] Tesseract failed: " + e.getMessage());
            e.printStackTrace();
            return "";
        } catch (Exception e) {
            System.err.println("[OCR ERROR] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return "";
        } finally {
            // Clean up temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    System.out.println("[OCR] Temporary file cleaned up.");
                } catch (Exception e) {
                    System.err.println("[OCR WARNING] Could not delete temp file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Performs OCR directly on an image file.
     *
     * <p>Why provide file-based method:</p>
     * <ul>
     *   <li>Avoids unnecessary Mat conversion if image is already saved</li>
     *   <li>Useful for batch processing pre-saved plate images</li>
     *   <li>Can be used for testing with sample images</li>
     * </ul>
     *
     * @param imagePath Path to the license plate image file
     * @return Recognized text from the plate, or empty string if failed
     */
    public String recognizePlateFromFile(String imagePath) {
        File imageFile = new File(imagePath);

        if (!imageFile.exists()) {
            System.err.println("[OCR ERROR] Image file not found: " + imagePath);
            return "";
        }

        System.out.println("[OCR] Processing file: " + imagePath);

        try {
            String rawResult = tesseract.doOCR(imageFile);
            String cleanedResult = postProcessOcrResult(rawResult);

            System.out.println("[OCR] Raw result: \"" + rawResult.trim() + "\"");
            System.out.println("[OCR] Cleaned result: \"" + cleanedResult + "\"");

            return cleanedResult;

        } catch (TesseractException e) {
            System.err.println("[OCR ERROR] Tesseract failed: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Post-processes OCR result to clean up common issues.
     *
     * <p>Why post-processing is necessary:</p>
     * <ul>
     *   <li>OCR may include whitespace, newlines, or artifacts</li>
     *   <li>Some characters may still be misread despite whitelist</li>
     *   <li>Turkish plates have specific format rules we can validate</li>
     *   <li>Removes any non-alphanumeric characters that slip through</li>
     * </ul>
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Trim whitespace from both ends</li>
     *   <li>Remove all spaces and newlines</li>
     *   <li>Convert to uppercase (safety measure)</li>
     *   <li>Remove any remaining non-alphanumeric characters</li>
     * </ol>
     *
     * @param rawResult The raw OCR output string
     * @return Cleaned license plate text
     */
    private String postProcessOcrResult(String rawResult) {
        if (rawResult == null || rawResult.isEmpty()) {
            return "";
        }

        return rawResult
                .trim()                              // Remove leading/trailing whitespace
                .replaceAll("\\s+", "")              // Remove all whitespace
                .toUpperCase()                       // Ensure uppercase
                .replaceAll("[^A-Z0-9]", "");        // Keep only alphanumeric
    }

    /**
     * Sets the tessdata path for Tesseract.
     *
     * <p>Why allow runtime path change:</p>
     * <ul>
     *   <li>Enables switching between different trained models</li>
     *   <li>Useful for testing with different tessdata versions</li>
     *   <li>Allows configuration after object creation</li>
     * </ul>
     *
     * @param tessdataPath New path to tessdata directory
     */
    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            tesseract.setDatapath(tessdataPath);
            System.out.println("[OCR] Tessdata path updated: " + tessdataPath);
        }
    }

    /**
     * Gets the current tessdata path.
     *
     * @return Current tessdata path
     */
    public String getTessdataPath() {
        return tessdataPath;
    }

    /**
     * Sets a custom character whitelist for OCR.
     *
     * <p>Why allow custom whitelist:</p>
     * <ul>
     *   <li>Different countries may have different plate characters</li>
     *   <li>Some plates may include special characters (e.g., hyphens)</li>
     *   <li>Testing with different configurations</li>
     * </ul>
     *
     * @param whitelist New character whitelist string
     */
    public void setWhitelist(String whitelist) {
        tesseract.setVariable("tessedit_char_whitelist", whitelist);
        System.out.println("[OCR] Whitelist updated: " + whitelist);
    }

    /**
     * Sets the OCR language model.
     *
     * <p>Why configurable language:</p>
     * <ul>
     *   <li>Different languages have different character sets</li>
     *   <li>May need to use custom trained models for plates</li>
     *   <li>Some regions may benefit from specific language models</li>
     * </ul>
     *
     * @param language Language code (e.g., "eng", "tur", "deu")
     */
    public void setLanguage(String language) {
        tesseract.setLanguage(language);
        System.out.println("[OCR] Language set to: " + language);
    }
}

