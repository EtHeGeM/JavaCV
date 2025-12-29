package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * TuningGUI - Dual-Detection Audit Tool for ALPR
 *
 * @author ALPR Academic Project
 * @version 2.3 - Added persistent configuration with auto-save
 */
public class TuningGUI extends JFrame {

    private static final String CONFIG_FILE = "alpr_config.properties";
    private static final int SAVE_DELAY_MS = 1000; // 1 saniye bekle (debounce)

    private ImagePanel imagePanel;

    private JSlider blurSlider;
    private JSlider canny1Slider;
    private JSlider canny2Slider;
    private JSlider dilateKernelSlider;
    private JSlider dilateIterSlider;
    private JSpinner minARSpinner;
    private JSpinner maxARSpinner;
    private JComboBox<String> previewModeCombo;

    private JCheckBox showHaarCheck;
    private JCheckBox showGeoCheck;
    private JCheckBox showOverlapCheck;

    private JLabel statusLabel;
    private JLabel statsLabel;

    // OCR Results Panel
    private JTextArea haarOcrResults;
    private JTextArea geoOcrResults;
    private JLabel bestResultLabel;

    private PlateDetector detector;
    private OcrService ocrService;
    private String currentImagePath;
    private boolean autoProcess = true;

    // Configuration
    private Properties config;

    // Debounce timer for auto-save
    private Timer saveTimer;

    private static final String[] PREVIEW_MODES = {
        "Original + Overlays", "Grayscale", "Filtered", "Canny Edges", "Dilated", "Detection Result"
    };

    private static final Color HAAR_COLOR = new Color(0, 255, 0, 180);
    private static final Color GEO_COLOR = new Color(0, 0, 255, 180);
    private static final Color OVERLAP_COLOR = new Color(255, 0, 0, 200);

    static {
        try {
            OpenCV.loadLocally();
        } catch (Exception e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    public TuningGUI() {
        super("ALPR Dual-Detection Audit Tool");
        detector = new PlateDetector();
        ocrService = new OcrService();
        config = loadConfig();
        initializeUI();
        applyConfig();
        checkResources();
    }

    // ==================== CONFIGURATION ====================

    private Properties loadConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                System.out.println("[CONFIG] Loaded configuration from: " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("[CONFIG] Failed to load config: " + e.getMessage());
            }
        } else {
            // Set defaults
            props.setProperty("blur.kernel", "11");
            props.setProperty("canny.threshold1", "50");
            props.setProperty("canny.threshold2", "150");
            props.setProperty("dilate.kernel", "3");
            props.setProperty("dilate.iterations", "2");
            props.setProperty("aspect.min", "2.0");
            props.setProperty("aspect.max", "7.0");
            props.setProperty("show.haar", "true");
            props.setProperty("show.geo", "true");
            props.setProperty("show.overlap", "true");
            props.setProperty("last.image.path", "");
            System.out.println("[CONFIG] Using default configuration");
        }

        return props;
    }

    private void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            // Update config from current UI values
            int blurValue = blurSlider.getValue();
            if (blurValue % 2 == 0) blurValue++;
            int dilateKernel = dilateKernelSlider.getValue();
            if (dilateKernel % 2 == 0) dilateKernel++;

            config.setProperty("blur.kernel", String.valueOf(blurValue));
            config.setProperty("canny.threshold1", String.valueOf(canny1Slider.getValue()));
            config.setProperty("canny.threshold2", String.valueOf(canny2Slider.getValue()));
            config.setProperty("dilate.kernel", String.valueOf(dilateKernel));
            config.setProperty("dilate.iterations", String.valueOf(dilateIterSlider.getValue()));
            config.setProperty("aspect.min", String.valueOf(minARSpinner.getValue()));
            config.setProperty("aspect.max", String.valueOf(maxARSpinner.getValue()));
            config.setProperty("show.haar", String.valueOf(showHaarCheck.isSelected()));
            config.setProperty("show.geo", String.valueOf(showGeoCheck.isSelected()));
            config.setProperty("show.overlap", String.valueOf(showOverlapCheck.isSelected()));
            if (currentImagePath != null) {
                config.setProperty("last.image.path", currentImagePath);
            }

            config.store(fos, "ALPR Configuration - Auto-saved");
            System.out.println("[CONFIG] Configuration saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("[CONFIG] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Schedule auto-save with debounce mechanism.
     * Waits for user to stop changing values before saving.
     */
    private void scheduleAutoSave() {
        if (saveTimer != null) {
            saveTimer.stop();
        }
        saveTimer = new Timer(SAVE_DELAY_MS, e -> {
            saveConfig();
            saveTimer.stop();
        });
        saveTimer.setRepeats(false);
        saveTimer.start();
    }

    private void applyConfig() {
        try {
            blurSlider.setValue(Integer.parseInt(config.getProperty("blur.kernel", "11")));
            canny1Slider.setValue(Integer.parseInt(config.getProperty("canny.threshold1", "50")));
            canny2Slider.setValue(Integer.parseInt(config.getProperty("canny.threshold2", "150")));
            dilateKernelSlider.setValue(Integer.parseInt(config.getProperty("dilate.kernel", "3")));
            dilateIterSlider.setValue(Integer.parseInt(config.getProperty("dilate.iterations", "2")));
            minARSpinner.setValue(Double.parseDouble(config.getProperty("aspect.min", "2.0")));
            maxARSpinner.setValue(Double.parseDouble(config.getProperty("aspect.max", "7.0")));
            showHaarCheck.setSelected(Boolean.parseBoolean(config.getProperty("show.haar", "true")));
            showGeoCheck.setSelected(Boolean.parseBoolean(config.getProperty("show.geo", "true")));
            showOverlapCheck.setSelected(Boolean.parseBoolean(config.getProperty("show.overlap", "true")));

            // Load last image if exists
            String lastImagePath = config.getProperty("last.image.path", "");
            if (!lastImagePath.isEmpty() && new File(lastImagePath).exists()) {
                currentImagePath = lastImagePath;
                statusLabel.setText("Loaded last image: " + new File(lastImagePath).getName());
                SwingUtilities.invokeLater(this::processImage);
            }
        } catch (NumberFormatException e) {
            System.err.println("[CONFIG] Invalid config value: " + e.getMessage());
        }
    }

    // ==================== RESOURCES ====================

    private void checkResources() {
        if (!detector.isHaarAvailable()) {
            JOptionPane.showMessageDialog(this,
                "Haar Cascade file not found!\n" +
                "Haar detection will be disabled.\n" +
                "Only Geometric detection will be available.",
                "Resource Warning",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveConfig();
                System.exit(0);
            }
        });
        setSize(1500, 950);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(380);

        JPanel controlPanel = createControlPanel();
        splitPane.setLeftComponent(new JScrollPane(controlPanel));

        JPanel previewPanel = createPreviewPanel();
        splitPane.setRightComponent(previewPanel);

        add(splitPane, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Image Selection
        JPanel imagePanel = new JPanel(new BorderLayout(5, 5));
        imagePanel.setBorder(createTitledBorder("Image Selection"));
        JButton loadButton = new JButton("Load Image...");
        loadButton.addActionListener(e -> loadImage());
        imagePanel.add(loadButton, BorderLayout.CENTER);
        panel.add(imagePanel);
        panel.add(Box.createVerticalStrut(10));

        // Detection Method Toggles
        JPanel togglePanel = new JPanel(new GridLayout(3, 1, 5, 5));
        togglePanel.setBorder(createTitledBorder("Detection Layers"));

        showHaarCheck = new JCheckBox("Show Haar Cascade (Green)", true);
        showHaarCheck.setForeground(new Color(0, 150, 0));
        showHaarCheck.addActionListener(e -> { updatePreview(); scheduleAutoSave(); });
        togglePanel.add(showHaarCheck);

        showGeoCheck = new JCheckBox("Show Geometric (Blue)", true);
        showGeoCheck.setForeground(new Color(0, 0, 200));
        showGeoCheck.addActionListener(e -> { updatePreview(); scheduleAutoSave(); });
        togglePanel.add(showGeoCheck);

        showOverlapCheck = new JCheckBox("Show High Confidence (Red)", true);
        showOverlapCheck.setForeground(new Color(200, 0, 0));
        showOverlapCheck.addActionListener(e -> { updatePreview(); scheduleAutoSave(); });
        togglePanel.add(showOverlapCheck);

        panel.add(togglePanel);
        panel.add(Box.createVerticalStrut(10));

        // Detection Stats
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(createTitledBorder("Detection Statistics"));
        statsLabel = new JLabel("Haar: 0 | Geo: 0 | Overlap: 0", SwingConstants.CENTER);
        statsLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        statsPanel.add(statsLabel, BorderLayout.CENTER);
        panel.add(statsPanel);
        panel.add(Box.createVerticalStrut(10));

        // Geometric Parameters
        JPanel geoParamsPanel = new JPanel();
        geoParamsPanel.setLayout(new BoxLayout(geoParamsPanel, BoxLayout.Y_AXIS));
        geoParamsPanel.setBorder(createTitledBorder("Geometric Parameters"));

        // Blur
        blurSlider = createSlider(1, 15, 11, 2, "Blur Kernel");
        JLabel blurValueLabel = new JLabel("Value: 11", SwingConstants.CENTER);
        blurSlider.addChangeListener(e -> {
            int val = blurSlider.getValue();
            if (val % 2 == 0) val++;
            blurValueLabel.setText("Value: " + val);
        });
        blurSlider.addChangeListener(createProcessListener());
        geoParamsPanel.add(createSliderPanel("Bilateral Filter:", blurSlider, blurValueLabel));

        // Canny 1
        canny1Slider = createSlider(0, 255, 50, 50, "Canny T1");
        JLabel canny1ValueLabel = new JLabel("Value: 50", SwingConstants.CENTER);
        canny1Slider.addChangeListener(e -> canny1ValueLabel.setText("Value: " + canny1Slider.getValue()));
        canny1Slider.addChangeListener(createProcessListener());
        geoParamsPanel.add(createSliderPanel("Canny Threshold 1:", canny1Slider, canny1ValueLabel));

        // Canny 2
        canny2Slider = createSlider(0, 255, 150, 50, "Canny T2");
        JLabel canny2ValueLabel = new JLabel("Value: 150", SwingConstants.CENTER);
        canny2Slider.addChangeListener(e -> canny2ValueLabel.setText("Value: " + canny2Slider.getValue()));
        canny2Slider.addChangeListener(createProcessListener());
        geoParamsPanel.add(createSliderPanel("Canny Threshold 2:", canny2Slider, canny2ValueLabel));

        // Dilate Kernel
        dilateKernelSlider = createSlider(1, 15, 3, 2, "Dilate Kernel");
        JLabel dilateKernelValueLabel = new JLabel("Value: 3", SwingConstants.CENTER);
        dilateKernelSlider.addChangeListener(e -> {
            int val = dilateKernelSlider.getValue();
            if (val % 2 == 0) val++;
            dilateKernelValueLabel.setText("Value: " + val);
        });
        dilateKernelSlider.addChangeListener(createProcessListener());
        geoParamsPanel.add(createSliderPanel("Dilate Kernel:", dilateKernelSlider, dilateKernelValueLabel));

        // Dilate Iterations
        dilateIterSlider = createSlider(0, 10, 2, 1, "Dilate Iterations");
        JLabel dilateIterValueLabel = new JLabel("Value: 2", SwingConstants.CENTER);
        dilateIterSlider.addChangeListener(e -> dilateIterValueLabel.setText("Value: " + dilateIterSlider.getValue()));
        dilateIterSlider.addChangeListener(createProcessListener());
        geoParamsPanel.add(createSliderPanel("Dilate Iterations:", dilateIterSlider, dilateIterValueLabel));

        // Aspect Ratio
        JPanel arPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        arPanel.add(new JLabel("Min AR:"));
        minARSpinner = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 10.0, 0.1));
        minARSpinner.addChangeListener(createProcessListener());
        arPanel.add(minARSpinner);
        arPanel.add(new JLabel("Max AR:"));
        maxARSpinner = new JSpinner(new SpinnerNumberModel(7.0, 1.0, 10.0, 0.1));
        maxARSpinner.addChangeListener(createProcessListener());
        arPanel.add(maxARSpinner);
        geoParamsPanel.add(arPanel);

        panel.add(geoParamsPanel);
        panel.add(Box.createVerticalStrut(10));

        // Preview Mode
        JPanel previewModePanel = new JPanel(new BorderLayout(5, 5));
        previewModePanel.setBorder(createTitledBorder("Preview Mode"));
        previewModeCombo = new JComboBox<>(PREVIEW_MODES);
        previewModeCombo.setSelectedIndex(0);
        previewModeCombo.addActionListener(e -> updatePreview());
        previewModePanel.add(previewModeCombo, BorderLayout.CENTER);
        panel.add(previewModePanel);
        panel.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 5, 5));

        JButton processButton = new JButton("Detect");
        processButton.addActionListener(e -> processImage());
        buttonsPanel.add(processButton);

        JButton ocrButton = new JButton("Run OCR (All)");
        ocrButton.addActionListener(e -> runOcrOnAllDetections());
        buttonsPanel.add(ocrButton);

        JCheckBox autoProcessCheck = new JCheckBox("Auto", true);
        autoProcessCheck.addActionListener(e -> autoProcess = autoProcessCheck.isSelected());
        buttonsPanel.add(autoProcessCheck);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetDefaults());
        buttonsPanel.add(resetButton);

        panel.add(buttonsPanel);
        panel.add(Box.createVerticalStrut(10));

        // OCR Results - Haar
        JPanel haarOcrPanel = new JPanel(new BorderLayout());
        haarOcrPanel.setBorder(createTitledBorder("Haar OCR Results (Green)"));
        haarOcrResults = new JTextArea(3, 20);
        haarOcrResults.setEditable(false);
        haarOcrResults.setFont(new Font("Monospaced", Font.BOLD, 12));
        haarOcrResults.setForeground(new Color(0, 128, 0));
        haarOcrPanel.add(new JScrollPane(haarOcrResults), BorderLayout.CENTER);
        panel.add(haarOcrPanel);
        panel.add(Box.createVerticalStrut(5));

        // OCR Results - Geometric
        JPanel geoOcrPanel = new JPanel(new BorderLayout());
        geoOcrPanel.setBorder(createTitledBorder("Geometric OCR Results (Blue)"));
        geoOcrResults = new JTextArea(3, 20);
        geoOcrResults.setEditable(false);
        geoOcrResults.setFont(new Font("Monospaced", Font.BOLD, 12));
        geoOcrResults.setForeground(new Color(0, 0, 180));
        geoOcrPanel.add(new JScrollPane(geoOcrResults), BorderLayout.CENTER);
        panel.add(geoOcrPanel);
        panel.add(Box.createVerticalStrut(10));

        // Best Result
        JPanel bestPanel = new JPanel(new BorderLayout());
        bestPanel.setBorder(createTitledBorder("Best Result"));
        bestResultLabel = new JLabel("---", SwingConstants.CENTER);
        bestResultLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        bestResultLabel.setForeground(new Color(200, 0, 0));
        bestPanel.add(bestResultLabel, BorderLayout.CENTER);
        panel.add(bestPanel);

        // Legend
        panel.add(Box.createVerticalStrut(10));
        JPanel legendPanel = new JPanel(new GridLayout(3, 1));
        legendPanel.setBorder(createTitledBorder("Legend"));
        legendPanel.add(createLegendItem("■ Green = Haar Cascade", new Color(0, 180, 0)));
        legendPanel.add(createLegendItem("■ Blue = Geometric", new Color(0, 0, 180)));
        legendPanel.add(createLegendItem("■ Red = Both Agree", new Color(180, 0, 0)));
        panel.add(legendPanel);

        return panel;
    }

    private JSlider createSlider(int min, int max, int value, int majorTick, String name) {
        JSlider slider = new JSlider(min, max, value);
        slider.setMajorTickSpacing(majorTick);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JPanel createSliderPanel(String label, JSlider slider, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel createLegendItem(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        return label;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        imagePanel = new ImagePanel();
        imagePanel.setPreferredSize(new Dimension(1000, 800));

        JScrollPane scrollPane = new JScrollPane(imagePanel);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel = new JLabel("Ready. Load an image to start.");
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.addActionListener(e -> loadImage());
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            saveConfig();
            System.exit(0);
        });
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    private TitledBorder createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title);
    }

    private ChangeListener createProcessListener() {
        return e -> {
            if (autoProcess && currentImagePath != null) {
                if (e.getSource() instanceof JSlider) {
                    JSlider slider = (JSlider) e.getSource();
                    if (slider.getValueIsAdjusting()) return;
                }
                SwingUtilities.invokeLater(this::processImage);
            }
            // Schedule auto-save after parameter change
            scheduleAutoSave();
        };
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser("src/plates");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "bmp"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            currentImagePath = file.getAbsolutePath();
            statusLabel.setText("Loaded: " + file.getName());
            clearOcrResults();
            processImage();
            scheduleAutoSave(); // Save when new image is loaded
        }
    }

    private void clearOcrResults() {
        haarOcrResults.setText("");
        geoOcrResults.setText("");
        bestResultLabel.setText("---");
    }

    private void processImage() {
        if (currentImagePath == null) return;

        int blurValue = blurSlider.getValue();
        if (blurValue % 2 == 0) blurValue++;

        int dilateKernel = dilateKernelSlider.getValue();
        if (dilateKernel % 2 == 0) dilateKernel++;

        detector.setBlurKernel(blurValue);
        detector.setCannyThreshold1(canny1Slider.getValue());
        detector.setCannyThreshold2(canny2Slider.getValue());
        detector.setDilateKernelSize(dilateKernel);
        detector.setDilateIterations(dilateIterSlider.getValue());
        detector.setMinAspectRatio((Double) minARSpinner.getValue());
        detector.setMaxAspectRatio((Double) maxARSpinner.getValue());

        if (!detector.loadImage(currentImagePath)) {
            statusLabel.setText("Error: Could not load image");
            return;
        }

        detector.preprocess();
        List<DetectionResult> results = detector.detectAll();

        int[] stats = detector.getDetectionStats();
        statsLabel.setText(String.format("Haar: %d | Geo: %d | Overlap: %d", stats[0], stats[1], stats[2]));

        String status = results.isEmpty() ? "No plates found" : results.size() + " detection(s)";
        statusLabel.setText(status + " | Blur=" + blurValue +
            ", Canny=" + canny1Slider.getValue() + "-" + canny2Slider.getValue() +
            ", Dilate=" + dilateKernel + "x" + dilateIterSlider.getValue());

        updatePreview();
    }

    private void runOcrOnAllDetections() {
        List<DetectionResult> results = detector.getLastResults();
        if (results.isEmpty()) {
            statusLabel.setText("No detections to run OCR on");
            return;
        }

        StringBuilder haarSb = new StringBuilder();
        StringBuilder geoSb = new StringBuilder();
        String bestResult = "";
        int bestConfidence = 0;

        int haarIdx = 0, geoIdx = 0;

        for (DetectionResult result : results) {
            Mat plate = result.getCroppedPlate();
            if (plate == null || plate.empty()) continue;

            String ocrText = ocrService.recognizePlate(plate,
                    detector.getCurrentImageName() + "_" + result.getMethod().name().toLowerCase());
            result.setOcrResult(ocrText);

            if (result.getMethod() == DetectionResult.MethodType.HAAR) {
                haarSb.append("H").append(haarIdx++).append(": ").append(ocrText.isEmpty() ? "(empty)" : ocrText).append("\n");
            } else {
                geoSb.append("G").append(geoIdx++).append(": ").append(ocrText.isEmpty() ? "(empty)" : ocrText).append("\n");
            }

            // Determine best result based on length and format
            int confidence = calculateConfidence(ocrText);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestResult = ocrText;
            }
        }

        haarOcrResults.setText(haarSb.length() > 0 ? haarSb.toString().trim() : "(no Haar detections)");
        geoOcrResults.setText(geoSb.length() > 0 ? geoSb.toString().trim() : "(no Geo detections)");
        bestResultLabel.setText(bestResult.isEmpty() ? "---" : bestResult);

        // Update visualization with OCR results
        detector.getLastResults(); // Trigger re-visualization
        updatePreview();

        statusLabel.setText("OCR complete. Haar: " + haarIdx + ", Geo: " + geoIdx);
    }

    /**
     * Calculate confidence score for OCR result based on Turkish plate format.
     * Turkish plates: 2 digits + 1-3 letters + 2-4 digits (e.g., 34ABC1234)
     */
    private int calculateConfidence(String text) {
        if (text == null || text.isEmpty()) return 0;

        int score = text.length(); // Base score = length

        // Bonus for matching Turkish plate pattern
        if (text.matches("^\\d{2}[A-Z]{1,3}\\d{2,4}$")) {
            score += 50; // Perfect match
        } else if (text.matches("^\\d{2}[A-Z]+\\d+$")) {
            score += 30; // Partial match
        } else if (text.matches("^[A-Z0-9]{6,9}$")) {
            score += 10; // Reasonable length
        }

        return score;
    }

    private void updatePreview() {
        if (currentImagePath == null) return;

        String mode = (String) previewModeCombo.getSelectedItem();

        if ("Original + Overlays".equals(mode)) {
            Mat original = detector.getOriginalImage();
            if (original != null && !original.empty()) {
                BufferedImage image = matToBufferedImage(original);
                imagePanel.setImage(image);
                imagePanel.setDetectionResults(detector.getLastResults(),
                        showHaarCheck.isSelected(),
                        showGeoCheck.isSelected(),
                        showOverlapCheck.isSelected());
            }
        } else {
            Mat displayImage = null;
            switch (mode) {
                case "Grayscale": displayImage = detector.getLastGrayImage(); break;
                case "Filtered": displayImage = detector.getLastFilteredImage(); break;
                case "Canny Edges": displayImage = detector.getLastEdgeImage(); break;
                case "Dilated": displayImage = detector.getLastDilatedImage(); break;
                case "Detection Result": displayImage = detector.getLastContourImage(); break;
            }

            if (displayImage != null && !displayImage.empty()) {
                BufferedImage image = matToBufferedImage(displayImage);
                imagePanel.setImage(image);
                imagePanel.clearOverlays();
            }
        }

        imagePanel.repaint();
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", mat, buffer);
            return ImageIO.read(new ByteArrayInputStream(buffer.toArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private void resetDefaults() {
        blurSlider.setValue(11);
        canny1Slider.setValue(50);
        canny2Slider.setValue(150);
        dilateKernelSlider.setValue(3);
        dilateIterSlider.setValue(2);
        minARSpinner.setValue(2.0);
        maxARSpinner.setValue(7.0);
        showHaarCheck.setSelected(true);
        showGeoCheck.setSelected(true);
        showOverlapCheck.setSelected(true);
        clearOcrResults();
        if (currentImagePath != null) processImage();
        scheduleAutoSave(); // Save after reset
    }

    // ==================== IMAGE PANEL ====================

    private static class ImagePanel extends JPanel {
        private BufferedImage image;
        private List<DetectionResult> results = new ArrayList<>();
        private boolean showHaar = true;
        private boolean showGeo = true;
        private boolean showOverlap = true;

        public void setImage(BufferedImage image) {
            this.image = image;
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            }
            revalidate();
        }

        public void setDetectionResults(List<DetectionResult> results,
                                        boolean showHaar, boolean showGeo, boolean showOverlap) {
            this.results = results != null ? results : new ArrayList<>();
            this.showHaar = showHaar;
            this.showGeo = showGeo;
            this.showOverlap = showOverlap;
        }

        public void clearOverlays() {
            this.results = new ArrayList<>();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (image == null) {
                g2d.setColor(Color.GRAY);
                g2d.drawString("Load an image to begin", getWidth() / 2 - 80, getHeight() / 2);
                return;
            }

            double scaleX = (double) getWidth() / image.getWidth();
            double scaleY = (double) getHeight() / image.getHeight();
            double scale = Math.min(Math.min(scaleX, scaleY), 1.0);

            int scaledWidth = (int) (image.getWidth() * scale);
            int scaledHeight = (int) (image.getHeight() * scale);
            int offsetX = (getWidth() - scaledWidth) / 2;
            int offsetY = (getHeight() - scaledHeight) / 2;

            g2d.drawImage(image, offsetX, offsetY, scaledWidth, scaledHeight, null);

            // Find overlaps
            List<Rect> haarRects = new ArrayList<>();
            List<Rect> geoRects = new ArrayList<>();

            for (DetectionResult result : results) {
                if (result.getMethod() == DetectionResult.MethodType.HAAR) {
                    haarRects.add(result.getBounds());
                } else {
                    geoRects.add(result.getBounds());
                }
            }

            List<Rect> overlapRects = new ArrayList<>();
            for (Rect haar : haarRects) {
                for (Rect geo : geoRects) {
                    if (DetectionResult.calculateIoU(haar, geo) > 0.3) {
                        overlapRects.add(haar);
                    }
                }
            }

            // Draw detections
            for (DetectionResult result : results) {
                Rect rect = result.getBounds();
                boolean isOverlap = overlapRects.stream()
                        .anyMatch(o -> DetectionResult.calculateIoU(rect, o) > 0.3);

                if (isOverlap && showOverlap) continue; // Draw overlaps separately

                Color color = null;
                if (result.getMethod() == DetectionResult.MethodType.HAAR && showHaar) {
                    color = HAAR_COLOR;
                } else if (result.getMethod() == DetectionResult.MethodType.GEOMETRIC && showGeo) {
                    color = GEO_COLOR;
                }

                if (color != null) {
                    String label = result.getMethod() == DetectionResult.MethodType.HAAR ? "H" : "G";
                    if (result.getOcrResult() != null && !result.getOcrResult().isEmpty()) {
                        label += ": " + result.getOcrResult();
                    }
                    drawDetectionRect(g2d, rect, color, scale, offsetX, offsetY, label);
                }
            }

            // Draw overlaps last
            if (showOverlap) {
                for (Rect rect : overlapRects) {
                    // Find OCR result for this overlap
                    String ocrText = "";
                    for (DetectionResult r : results) {
                        if (DetectionResult.calculateIoU(rect, r.getBounds()) > 0.3 && r.getOcrResult() != null) {
                            ocrText = r.getOcrResult();
                            break;
                        }
                    }
                    String label = "H+G" + (ocrText.isEmpty() ? "" : ": " + ocrText);
                    drawDetectionRect(g2d, rect, OVERLAP_COLOR, scale, offsetX, offsetY, label);
                }
            }
        }

        private void drawDetectionRect(Graphics2D g2d, Rect rect, Color color,
                                       double scale, int offsetX, int offsetY, String label) {
            int x = (int) (rect.x * scale) + offsetX;
            int y = (int) (rect.y * scale) + offsetY;
            int w = (int) (rect.width * scale);
            int h = (int) (rect.height * scale);

            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
            g2d.fillRect(x, y, w, h);

            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(x, y, w, h);

            if (label != null && !label.isEmpty()) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(label) + 6;
                int labelHeight = fm.getHeight();

                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
                g2d.fillRect(x, y - labelHeight - 2, labelWidth, labelHeight + 2);

                g2d.setColor(Color.WHITE);
                g2d.drawString(label, x + 3, y - 5);
            }
        }
    }

    // ==================== MAIN ====================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new TuningGUI().setVisible(true));
    }
}

