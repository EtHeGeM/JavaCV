package com.alpr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OcrService - Optical Character Recognition for License Plates
 *
 * <p>Uses Tesseract OCR to extract text from cropped plate images.</p>
 *
 * @author ALPR Academic Project
 * @version 1.2 - Simplified preprocessing with debug output
 */
public class OcrService {

    private final Tesseract tesseract;
    private static final String WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private String tessdataPath;

    /**
     * Debug output directory for OCR preprocessed images.
     */
    private static final String DEBUG_OCR_DIR = "debug_output/step5_ocr_preprocessed";

    public OcrService() {
        this(null);
    }

    public OcrService(String tessdataPath) {
        this.tessdataPath = tessdataPath;
        this.tesseract = new Tesseract();
        initializeTesseract();
        ensureDebugDir();
    }

    /**
     * Ensures the OCR debug directory exists.
     */
    private void ensureDebugDir() {
        File dir = new File(DEBUG_OCR_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void initializeTesseract() {
        System.out.println("[OCR] Initializing Tesseract...");

        String path = (tessdataPath != null) ? tessdataPath : findTessdataPath();
        if (path != null) {
            tesseract.setDatapath(path);
            System.out.println("[OCR] Tessdata: " + path);
        }

        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(7);  // Single line
        tesseract.setOcrEngineMode(3); // Default (LSTM)
        tesseract.setVariable("tessedit_char_whitelist", WHITELIST);

        System.out.println("[OCR] Ready. Whitelist: " + WHITELIST);
    }

    private String findTessdataPath() {
        String[] paths = {
            System.getProperty("user.dir") + "/tessdata",
            System.getProperty("user.dir") + "\\tessdata",
            "tessdata",
            System.getenv("TESSDATA_PREFIX"),
            "C:/Program Files/Tesseract-OCR/tessdata"
        };

        for (String p : paths) {
            if (p != null && new File(p, "eng.traineddata").exists()) {
                return new File(p).getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Simple preprocessing for OCR - resize and Otsu threshold.
     * Tesseract handles most binarization internally.
     *
     * @param plate     The plate image to preprocess
     * @param imageName Name for saving debug image
     * @return Preprocessed binary image
     */
    private Mat preprocessForOcr(Mat plate, String imageName) {
        Mat result = plate.clone();

        // Resize small images (Tesseract works better with larger images)
        if (result.width() < 200) {
            double scale = 200.0 / result.width();
            Imgproc.resize(result, result, new Size(result.width() * scale, result.height() * scale),
                    0, 0, Imgproc.INTER_CUBIC);
            System.out.println("[OCR] Resized to: " + result.width() + "x" + result.height());
        }

        // Convert to grayscale if color
        if (result.channels() == 3) {
            Mat gray = new Mat();
            Imgproc.cvtColor(result, gray, Imgproc.COLOR_BGR2GRAY);
            result = gray;
        }

        // Simple threshold to make text clearer
        Mat binary = new Mat();
        Imgproc.threshold(result, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        // Check if we need to invert (text should be dark on light background)
        double mean = Core.mean(binary).val[0];
        if (mean < 127) {
            Core.bitwise_not(binary, binary);
        }

        // Save debug image with proper name
        if (imageName != null && !imageName.isEmpty()) {
            String debugPath = DEBUG_OCR_DIR + "/" + imageName + ".jpg";
            Imgcodecs.imwrite(debugPath, binary);
            System.out.println("[DEBUG] Saved: " + debugPath);
        }

        return binary;
    }

    /**
     * Performs OCR on a cropped license plate image.
     *
     * @param plateMat  Cropped plate image
     * @param imageName Name of the source image (for debug output)
     * @return Recognized text, or empty string if failed
     */
    public String recognizePlate(Mat plateMat, String imageName) {
        if (plateMat == null || plateMat.empty()) {
            System.err.println("[OCR] Error: Empty input");
            return "";
        }

        System.out.println("[OCR] Input: " + plateMat.width() + "x" + plateMat.height());

        Mat processed = preprocessForOcr(plateMat, imageName);
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("plate_", ".png");
            Imgcodecs.imwrite(tempFile.toString(), processed);

            String result = tesseract.doOCR(tempFile.toFile());
            String cleaned = cleanResult(result);

            System.out.println("[OCR] Raw: \"" + result.trim() + "\"");
            System.out.println("[OCR] Clean: \"" + cleaned + "\"");

            return cleaned;

        } catch (TesseractException e) {
            System.err.println("[OCR] Tesseract error: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("[OCR] Error: " + e.getMessage());
            return "";
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Performs OCR on a cropped license plate image (without image name).
     *
     * @param plateMat Cropped plate image
     * @return Recognized text, or empty string if failed
     */
    public String recognizePlate(Mat plateMat) {
        return recognizePlate(plateMat, null);
    }

    /**
     * Cleans OCR result - removes spaces and non-alphanumeric chars.
     */
    private String cleanResult(String raw) {
        if (raw == null) return "";
        return raw.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
    }

    public void setTessdataPath(String path) {
        this.tessdataPath = path;
        if (path != null) tesseract.setDatapath(path);
    }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public void setWhitelist(String whitelist) {
        tesseract.setVariable("tessedit_char_whitelist", whitelist);
    }

    public void setLanguage(String lang) {
        tesseract.setLanguage(lang);
    }
}

