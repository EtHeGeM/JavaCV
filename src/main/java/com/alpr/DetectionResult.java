package com.alpr;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * DetectionResult - Container for plate detection results
 *
 * @author ALPR Academic Project
 * @version 1.1 - Added cropped plate image for OCR
 */
public class DetectionResult {

    /**
     * Detection method types
     */
    public enum MethodType {
        HAAR("Haar Cascade"),
        GEOMETRIC("Geometric/Contour");

        private final String displayName;

        MethodType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Rect bounds;
    private final MethodType method;
    private final double confidence;
    private Mat croppedPlate;  // Cropped plate image for OCR
    private String ocrResult;  // OCR result for this detection

    public DetectionResult(Rect bounds, MethodType method) {
        this(bounds, method, 1.0);
    }

    public DetectionResult(Rect bounds, MethodType method, double confidence) {
        this.bounds = bounds;
        this.method = method;
        this.confidence = confidence;
        this.ocrResult = null;
        this.croppedPlate = null;
    }

    public Rect getBounds() {
        return bounds;
    }

    public MethodType getMethod() {
        return method;
    }

    public double getConfidence() {
        return confidence;
    }

    public Mat getCroppedPlate() {
        return croppedPlate;
    }

    public void setCroppedPlate(Mat croppedPlate) {
        this.croppedPlate = croppedPlate;
    }

    public String getOcrResult() {
        return ocrResult;
    }

    public void setOcrResult(String ocrResult) {
        this.ocrResult = ocrResult;
    }

    /**
     * Calculates the Intersection over Union (IoU) with another detection result.
     *
     * @param other Another detection result
     * @return IoU value between 0.0 and 1.0
     */
    public double calculateIoU(DetectionResult other) {
        return calculateIoU(this.bounds, other.bounds);
    }

    /**
     * Calculates the Intersection over Union (IoU) between two rectangles.
     */
    public static double calculateIoU(Rect r1, Rect r2) {
        int x1 = Math.max(r1.x, r2.x);
        int y1 = Math.max(r1.y, r2.y);
        int x2 = Math.min(r1.x + r1.width, r2.x + r2.width);
        int y2 = Math.min(r1.y + r1.height, r2.y + r2.height);

        if (x2 <= x1 || y2 <= y1) {
            return 0.0; // No intersection
        }

        double intersectionArea = (x2 - x1) * (y2 - y1);
        double area1 = r1.width * r1.height;
        double area2 = r2.width * r2.height;
        double unionArea = area1 + area2 - intersectionArea;

        return intersectionArea / unionArea;
    }

    /**
     * Checks if this detection overlaps with another by more than the given threshold.
     *
     * @param other     Another detection result
     * @param threshold IoU threshold (e.g., 0.5 for 50%)
     * @return true if overlap exceeds threshold
     */
    public boolean overlaps(DetectionResult other, double threshold) {
        return calculateIoU(other) > threshold;
    }

    @Override
    public String toString() {
        return String.format("DetectionResult{method=%s, bounds=[%d,%d,%dx%d], ocr='%s'}",
                method.name(), bounds.x, bounds.y, bounds.width, bounds.height,
                ocrResult != null ? ocrResult : "N/A");
    }
}
