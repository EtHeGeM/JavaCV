package com.alpr;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Hashtable;

/**
 * TuningGUI - Parameter Tuning Dashboard for ALPR
 *
 * @author ALPR Academic Project
 * @version 1.0
 */
public class TuningGUI extends JFrame {

    private JLabel imageLabel;
    private JSlider blurSlider;
    private JSlider canny1Slider;
    private JSlider canny2Slider;
    private JSpinner minARSpinner;
    private JSpinner maxARSpinner;
    private JComboBox<String> previewModeCombo;
    private JLabel statusLabel;
    private JLabel plateTextLabel;

    private PlateDetector detector;
    private OcrService ocrService;
    private String currentImagePath;
    private boolean autoProcess = true;

    private static final String[] PREVIEW_MODES = {
        "Original", "Grayscale", "Filtered", "Canny Edges", "Dilated", "Detection Result"
    };

    static {
        try {
            OpenCV.loadLocally();
        } catch (Exception e) {
            System.err.println("Failed to load OpenCV: " + e.getMessage());
        }
    }

    public TuningGUI() {
        super("ALPR Parameter Tuning Dashboard");
        detector = new PlateDetector();
        ocrService = new OcrService();
        initializeUI();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(320);

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

        // Blur Settings
        JPanel blurPanel = new JPanel(new BorderLayout(5, 5));
        blurPanel.setBorder(createTitledBorder("Bilateral Filter Kernel Size"));
        blurSlider = new JSlider(1, 15, 11);
        blurSlider.setMajorTickSpacing(2);
        blurSlider.setPaintTicks(true);
        blurSlider.setPaintLabels(true);
        blurSlider.setSnapToTicks(true);
        Hashtable<Integer, JLabel> blurLabels = new Hashtable<>();
        for (int i = 1; i <= 15; i += 2) {
            blurLabels.put(i, new JLabel(String.valueOf(i)));
        }
        blurSlider.setLabelTable(blurLabels);
        JLabel blurValueLabel = new JLabel("Value: 11", SwingConstants.CENTER);
        blurSlider.addChangeListener(e -> {
            int val = blurSlider.getValue();
            if (val % 2 == 0) val++;
            blurValueLabel.setText("Value: " + val);
        });
        blurSlider.addChangeListener(createProcessListener());
        blurPanel.add(blurSlider, BorderLayout.CENTER);
        blurPanel.add(blurValueLabel, BorderLayout.SOUTH);
        panel.add(blurPanel);
        panel.add(Box.createVerticalStrut(10));

        // Canny Threshold 1
        JPanel canny1Panel = new JPanel(new BorderLayout(5, 5));
        canny1Panel.setBorder(createTitledBorder("Canny Threshold 1 (Lower)"));
        canny1Slider = new JSlider(0, 255, 30);
        canny1Slider.setMajorTickSpacing(50);
        canny1Slider.setMinorTickSpacing(10);
        canny1Slider.setPaintTicks(true);
        canny1Slider.setPaintLabels(true);
        JLabel canny1ValueLabel = new JLabel("Value: 30", SwingConstants.CENTER);
        canny1Slider.addChangeListener(e -> canny1ValueLabel.setText("Value: " + canny1Slider.getValue()));
        canny1Slider.addChangeListener(createProcessListener());
        canny1Panel.add(canny1Slider, BorderLayout.CENTER);
        canny1Panel.add(canny1ValueLabel, BorderLayout.SOUTH);
        panel.add(canny1Panel);
        panel.add(Box.createVerticalStrut(10));

        // Canny Threshold 2
        JPanel canny2Panel = new JPanel(new BorderLayout(5, 5));
        canny2Panel.setBorder(createTitledBorder("Canny Threshold 2 (Upper)"));
        canny2Slider = new JSlider(0, 255, 200);
        canny2Slider.setMajorTickSpacing(50);
        canny2Slider.setMinorTickSpacing(10);
        canny2Slider.setPaintTicks(true);
        canny2Slider.setPaintLabels(true);
        JLabel canny2ValueLabel = new JLabel("Value: 200", SwingConstants.CENTER);
        canny2Slider.addChangeListener(e -> canny2ValueLabel.setText("Value: " + canny2Slider.getValue()));
        canny2Slider.addChangeListener(createProcessListener());
        canny2Panel.add(canny2Slider, BorderLayout.CENTER);
        canny2Panel.add(canny2ValueLabel, BorderLayout.SOUTH);
        panel.add(canny2Panel);
        panel.add(Box.createVerticalStrut(10));

        // Aspect Ratio
        JPanel arPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        arPanel.setBorder(createTitledBorder("Aspect Ratio Filter"));
        arPanel.add(new JLabel("Min AR:"));
        minARSpinner = new JSpinner(new SpinnerNumberModel(2.5, 1.0, 10.0, 0.1));
        minARSpinner.addChangeListener(createProcessListener());
        arPanel.add(minARSpinner);
        arPanel.add(new JLabel("Max AR:"));
        maxARSpinner = new JSpinner(new SpinnerNumberModel(6.0, 1.0, 10.0, 0.1));
        maxARSpinner.addChangeListener(createProcessListener());
        arPanel.add(maxARSpinner);
        panel.add(arPanel);
        panel.add(Box.createVerticalStrut(10));

        // Preview Mode
        JPanel previewModePanel = new JPanel(new BorderLayout(5, 5));
        previewModePanel.setBorder(createTitledBorder("Preview Mode"));
        previewModeCombo = new JComboBox<>(PREVIEW_MODES);
        previewModeCombo.setSelectedIndex(5);
        previewModeCombo.addActionListener(e -> updatePreview());
        previewModePanel.add(previewModeCombo, BorderLayout.CENTER);
        panel.add(previewModePanel);
        panel.add(Box.createVerticalStrut(10));

        // Auto Process Checkbox
        JCheckBox autoProcessCheck = new JCheckBox("Auto-process on change", true);
        autoProcessCheck.addActionListener(e -> autoProcess = autoProcessCheck.isSelected());
        panel.add(autoProcessCheck);
        panel.add(Box.createVerticalStrut(10));

        // Process Button
        JButton processButton = new JButton("Process Image");
        processButton.setFont(processButton.getFont().deriveFont(Font.BOLD, 14f));
        processButton.addActionListener(e -> processImage());
        panel.add(processButton);
        panel.add(Box.createVerticalStrut(10));

        // Run OCR Button
        JButton ocrButton = new JButton("Run OCR on Detected Plate");
        ocrButton.addActionListener(e -> runOCR());
        panel.add(ocrButton);
        panel.add(Box.createVerticalStrut(10));

        // Detected Plate Text
        JPanel platePanel = new JPanel(new BorderLayout(5, 5));
        platePanel.setBorder(createTitledBorder("Detected Plate"));
        plateTextLabel = new JLabel("---", SwingConstants.CENTER);
        plateTextLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        plateTextLabel.setForeground(new Color(0, 128, 0));
        platePanel.add(plateTextLabel, BorderLayout.CENTER);
        panel.add(platePanel);
        panel.add(Box.createVerticalStrut(20));

        // Reset Button
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.addActionListener(e -> resetDefaults());
        panel.add(resetButton);

        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        imageLabel = new JLabel("Load an image to begin", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(800, 600));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel = new JLabel("Ready. Load an image to start tuning.");
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
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        for (String mode : PREVIEW_MODES) {
            JMenuItem item = new JMenuItem(mode);
            item.addActionListener(e -> {
                previewModeCombo.setSelectedItem(mode);
                updatePreview();
            });
            viewMenu.add(item);
        }
        menuBar.add(viewMenu);

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
                    if (slider.getValueIsAdjusting()) {
                        return; // Don't process while dragging
                    }
                }
                SwingUtilities.invokeLater(this::processImage);
            }
        };
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser("src/plates");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "bmp"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            currentImagePath = file.getAbsolutePath();
            statusLabel.setText("Loaded: " + file.getName());
            plateTextLabel.setText("---");
            processImage();
        }
    }

    private void processImage() {
        if (currentImagePath == null) return;

        int blurValue = blurSlider.getValue();
        if (blurValue % 2 == 0) blurValue++;

        detector.setBlurKernel(blurValue);
        detector.setCannyThreshold1(canny1Slider.getValue());
        detector.setCannyThreshold2(canny2Slider.getValue());
        detector.setMinAspectRatio((Double) minARSpinner.getValue());
        detector.setMaxAspectRatio((Double) maxARSpinner.getValue());

        if (!detector.loadImage(currentImagePath)) {
            statusLabel.setText("Error: Could not load image");
            return;
        }

        detector.preprocess();
        Mat plate = detector.findPlate();

        String status = plate != null ? "Plate detected!" : "No plate found";
        statusLabel.setText(status + " | Blur: " + blurValue +
            " | Canny: " + canny1Slider.getValue() + "-" + canny2Slider.getValue() +
            " | AR: " + minARSpinner.getValue() + "-" + maxARSpinner.getValue());

        updatePreview();
    }

    private void updatePreview() {
        if (currentImagePath == null) return;

        Mat displayImage = null;
        String mode = (String) previewModeCombo.getSelectedItem();

        switch (mode) {
            case "Original": displayImage = detector.getOriginalImage(); break;
            case "Grayscale": displayImage = detector.getLastGrayImage(); break;
            case "Filtered": displayImage = detector.getLastFilteredImage(); break;
            case "Canny Edges": displayImage = detector.getLastEdgeImage(); break;
            case "Dilated": displayImage = detector.getLastDilatedImage(); break;
            case "Detection Result": displayImage = detector.getLastContourImage(); break;
        }

        if (displayImage != null && !displayImage.empty()) {
            displayMat(displayImage);
        }
    }

    private void displayMat(Mat mat) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", mat, buffer);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));

            int maxWidth = imageLabel.getParent().getWidth() - 20;
            int maxHeight = imageLabel.getParent().getHeight() - 20;

            if (maxWidth > 0 && maxHeight > 0) {
                double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
                if (scale < 1.0) {
                    int newWidth = (int) (image.getWidth() * scale);
                    int newHeight = (int) (image.getHeight() * scale);
                    imageLabel.setIcon(new ImageIcon(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)));
                } else {
                    imageLabel.setIcon(new ImageIcon(image));
                }
            } else {
                imageLabel.setIcon(new ImageIcon(image));
            }
            imageLabel.setText(null);
        } catch (Exception e) {
            imageLabel.setIcon(null);
            imageLabel.setText("Error: " + e.getMessage());
        }
    }

    private void runOCR() {
        Mat plate = detector.findPlate();
        if (plate != null && !plate.empty()) {
            String text = ocrService.recognizePlate(plate, detector.getCurrentImageName());
            plateTextLabel.setText(text != null && !text.isEmpty() ? text : "(empty)");
            statusLabel.setText("OCR Result: " + (text != null ? text : "empty"));
        } else {
            plateTextLabel.setText("---");
            statusLabel.setText("No plate detected for OCR");
        }
    }

    private void resetDefaults() {
        blurSlider.setValue(11);
        canny1Slider.setValue(30);
        canny2Slider.setValue(200);
        minARSpinner.setValue(2.5);
        maxARSpinner.setValue(6.0);
        if (currentImagePath != null) processImage();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new TuningGUI().setVisible(true));
    }
}

