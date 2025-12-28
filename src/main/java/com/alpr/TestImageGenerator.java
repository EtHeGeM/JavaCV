package com.alpr;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * TestImageGenerator - Utility class for generating test images
 *
 * <p>This class creates synthetic test images for development and testing
 * purposes when real vehicle images are not available.</p>
 *
 * <p>Why we need this:</p>
 * <ul>
 *   <li>Allows testing the preprocessing pipeline without external images</li>
 *   <li>Creates a controlled test scenario with known characteristics</li>
 *   <li>Useful for unit testing and debugging</li>
 * </ul>
 *
 * @author ALPR Academic Project
 * @version 1.0
 */
public class TestImageGenerator {

    /**
     * Generates a simple test image with a simulated license plate.
     *
     * <p>The generated image contains:</p>
     * <ul>
     *   <li>A gray background simulating a vehicle body</li>
     *   <li>A white rectangle simulating a license plate</li>
     *   <li>Text on the plate for OCR testing</li>
     * </ul>
     *
     * @param outputPath The file path where the test image will be saved
     * @return true if the image was successfully created, false otherwise
     */
    public static boolean generateTestImage(String outputPath) {
        try {
            // Create a 640x480 image with 3 color channels (BGR)
            // Why: This is a common resolution that works well for testing
            Mat image = new Mat(480, 640, CvType.CV_8UC3);

            // Fill with a gray color (simulating a car body)
            // Why: Gray provides good contrast for the white license plate
            image.setTo(new Scalar(128, 128, 128));

            // Draw a white rectangle simulating a license plate
            // Why: License plates are typically white/light colored rectangles
            // Position: roughly center-bottom of the image (where plates usually are)
            Point plateTopLeft = new Point(200, 300);
            Point plateBottomRight = new Point(440, 380);
            Imgproc.rectangle(image, plateTopLeft, plateBottomRight,
                    new Scalar(255, 255, 255), -1); // -1 = filled rectangle

            // Draw a black border around the plate
            // Why: License plates have a visible border/frame
            Imgproc.rectangle(image, plateTopLeft, plateBottomRight,
                    new Scalar(0, 0, 0), 2);

            // Add simulated plate text
            // Why: Provides content for OCR testing
            Point textOrigin = new Point(220, 355);
            Imgproc.putText(image, "34 ABC 123", textOrigin,
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.2,
                    new Scalar(0, 0, 0), 2);

            // Save the generated image
            boolean success = Imgcodecs.imwrite(outputPath, image);

            if (success) {
                System.out.println("[INFO] Test image generated: " + outputPath);
                System.out.println("[INFO] Image size: " + image.cols() + "x" + image.rows());
            } else {
                System.err.println("[ERROR] Failed to save test image: " + outputPath);
            }

            return success;

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to generate test image: " + e.getMessage());
            return false;
        }
    }
}
