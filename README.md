# ALPR Tuning Tool - License Plate Recognition Optimization Tool

![Java](https://img.shields.io/badge/Java-17-orange)
![OpenCV](https://img.shields.io/badge/OpenCV-4.9.0-blue)
![Tesseract](https://img.shields.io/badge/Tesseract-5.11.0-green)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

## Table of Contents

- [About the Project](#about-the-project)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Java Classes and Methods](#java-classes-and-methods)
- [Parameters and Optimization](#parameters-and-optimization)
- [Debug Outputs](#debug-outputs)
- [Improvement Suggestions](#improvement-suggestions)
- [Contributors](#contributors)

---

## About the Project

**ALPR Tuning Tool** is an academic project developed to facilitate the optimization of image processing parameters used in license plate recognition systems.

### Important Note

> This project is **not a complete license plate recognition system**. Its main purpose is to provide a tool for **real-time testing and optimization** of:
> - Image processing parameters (blur, canny, dilate etc.)
> - Detection algorithms (Haar Cascade, Geometric Detection)
> - OCR settings
> 
> used in license plate recognition systems.

### Why This Tool Is Needed?

The most challenging part when developing license plate recognition systems is finding optimal parameters for different lighting conditions, camera angles, and plate types. This tool:

1. **Visual Feedback**: Allows you to instantly see the effect of each parameter change
2. **Dual Detection Comparison**: Compares Haar Cascade and Geometric detection methods side by side
3. **Automatic Saving**: Automatically saves your settings so you can return to the best parameters
4. **Debug Outputs**: Saves images of each processing step

---

## Features

### Dual Detection System

| Method | Description | Color Code |
|--------|-------------|------------|
| **Haar Cascade** | Fast detection with pre-trained cascade classifier | Green |
| **Geometric Detection** | Contour analysis and aspect ratio filtering | Blue |
| **High Confidence** | Regions detected by both methods | Red |

### Adjustable Parameters

- **Bilateral Filter Kernel**: Image smoothing
- **Canny Threshold 1 & 2**: Edge detection sensitivity
- **Dilate Kernel & Iterations**: Morphological dilation
- **Aspect Ratio (Min/Max)**: Plate width-height ratio filter

### Additional Features

- Real-time preview (6 different modes)
- Display OCR results
- Turkish plate format validation
- Automatic configuration saving
- Result reporting in CSV format
- Batch processing support

---

## Requirements

### System Requirements

- **Operating System**: Windows 10/11, Linux, macOS
- **Java**: JDK 17 or higher
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk**: 500MB free space

### Software Dependencies

| Dependency | Version | Description |
|------------|---------|-------------|
| OpenCV | 4.9.0 | Image processing library |
| Tess4J | 5.11.0 | Tesseract OCR Java wrapper |
| SLF4J | 2.0.9 | Logging facade |
| Logback | 1.4.14 | Logging implementation |

---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/JavaCV.git
cd JavaCV
```

### 2. Tesseract OCR Installation

#### Windows
```bash
# With Chocolatey:
choco install tesseract

# or manually:
# Download from https://github.com/UB-Mannheim/tesseract/wiki
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-eng
```

#### macOS
```bash
brew install tesseract
```

### 3. Tessdata File

Make sure the `eng.traineddata` file is in the `tessdata/` folder:

```
JavaCV/
├── tessdata/
│   └── eng.traineddata   ← This file is required!
```

To download: [tessdata repository](https://github.com/tesseract-ocr/tessdata)

### 4. Maven Build

```bash
# Download dependencies and compile
mvn clean install

# To skip tests:
mvn clean install -DskipTests
```

### 5. Running

#### GUI Mode (Tuning Tool)
```bash
mvn exec:java -Dexec.mainClass="com.alpr.TuningGUI"
```

#### CLI Mode (Batch Processing)
```bash
# Single image processing
mvn exec:java -Dexec.mainClass="com.alpr.Main" -Dexec.args="path/to/image.jpg"

# Process all images in folder
mvn exec:java -Dexec.mainClass="com.alpr.Main" -Dexec.args="src/plates"
```

### 6. Creating Distributable Package

#### Creating Fat JAR (All dependencies included)
```bash
mvn clean package
```

This command creates the following files in the `target/` folder:

| File | Description |
|------|-------------|
| `license-plate-recognition-1.0-SNAPSHOT.jar` | Executable JAR containing all dependencies |
| `alpr-cli.exe` | Windows CLI application |
| `alpr-gui.exe` | Windows GUI application |

#### Running JAR File
```bash
# CLI mode
java -jar target/license-plate-recognition-1.0-SNAPSHOT.jar src/plates

# GUI mode
java -cp target/license-plate-recognition-1.0-SNAPSHOT.jar com.alpr.TuningGUI
```

#### Running Windows EXE Files
```bash
# CLI version
target\alpr-cli.exe src/plates

# GUI version (can also be opened by double-clicking)
target\alpr-gui.exe
```

### 7. Preparing Distribution Package

To distribute the application, copy these files to a folder:

```
ALPR-Distribution/
├── alpr-cli.exe              # or .jar file
├── alpr-gui.exe              # or .jar file
├── tessdata/
│   └── eng.traineddata       # Required for OCR
├── haarcascade_russian_plate_number.xml  # Haar cascade model
└── alpr_config.properties    # (optional) pre-configured parameters
```

> **Note:** Java 17+ must be installed on the user's computer. EXE files use the `JAVA_HOME` environment variable.

---

## Usage

### GUI Interface

1. **Load Image**: Load the image to be tested
2. **Adjust Parameters**: Use sliders to change parameters
3. **Detect**: Run plate detection (runs automatically if Auto is selected)
4. **Run OCR**: Run OCR on detected plates
5. **Preview Mode**: View different processing steps

### Preview Modes

| Mode | Description |
|------|-------------|
| Original + Overlays | Original image + detection boxes |
| Grayscale | Grayscale image |
| Filtered | Bilateral filter applied |
| Canny Edges | Edge detection result |
| Dilated | After morphological dilation |
| Detection Result | All detection results |

### Keyboard Shortcuts

- **Ctrl+O**: Load image
- **Ctrl+Q**: Exit

---

## Project Structure

```
JavaCV/
├── pom.xml                          # Maven configuration
├── alpr_config.properties           # Automatically saved settings
├── alpr_summary.csv                 # Summary result report
├── haarcascade_russian_plate_number.xml  # Haar Cascade model
│
├── src/
│   ├── main/
│   │   ├── java/com/alpr/
│   │   │   ├── TuningGUI.java       # Main GUI application
│   │   │   ├── PlateDetector.java   # Plate detection engine
│   │   │   ├── OcrService.java      # Tesseract OCR service
│   │   │   ├── DetectionResult.java # Detection result model
│   │   │   ├── Main.java            # CLI entry point
│   │   │   └── TestImageGenerator.java # Test image generator
│   │   └── resources/
│   └── plates/                      # Test images folder
│
├── tessdata/
│   └── eng.traineddata              # Tesseract language file
│
├── debug_output/                    # Debug images
│   ├── step1_grayscale/
│   ├── step2_filtered/
│   ├── step3_canny/
│   ├── step3b_dilated/
│   ├── step4_detected_plate/
│   ├── step5_ocr_preprocessed/
│   ├── haar_plates/                 # Plates detected with Haar
│   └── geo_plates/                  # Plates detected with Geometric
│
└── target/                          # Maven build outputs
```

---

## Java Classes and Methods

### 1. TuningGUI.java

Main GUI application - Swing-based parameter adjustment and preview tool.

#### Configuration Methods

| Method | Description |
|--------|-------------|
| `loadConfig()` | Loads settings from `alpr_config.properties` file |
| `saveConfig()` | Saves current settings to file |
| `scheduleAutoSave()` | Schedules automatic save with debounce mechanism (1s delay) |
| `applyConfig()` | Applies loaded settings to UI components |

#### UI Creation Methods

| Method | Description |
|--------|-------------|
| `initializeUI()` | Creates main window and components |
| `createControlPanel()` | Creates left-side control panel |
| `createPreviewPanel()` | Creates right-side image preview panel |
| `createStatusBar()` | Creates bottom status bar |
| `createMenuBar()` | Creates menu bar |
| `createSlider(min, max, value, majorTick, name)` | Creates parameter slider |
| `createSliderPanel(label, slider, valueLabel)` | Creates labeled panel for slider |
| `createTitledBorder(title)` | Creates titled border |
| `createLegendItem(text, color)` | Creates color legend label |

#### Processing Methods

| Method | Description |
|--------|-------------|
| `loadImage()` | Loads image with file chooser |
| `processImage()` | Runs plate detection and updates results |
| `updatePreview()` | Updates preview according to selected mode |
| `runOcrOnAllDetections()` | Runs OCR on all detected plates |
| `calculateConfidence(text)` | Calculates confidence score of OCR result |
| `resetDefaults()` | Returns all parameters to default values |
| `clearOcrResults()` | Clears OCR result areas |
| `matToBufferedImage(mat)` | Converts OpenCV Mat to BufferedImage |
| `checkResources()` | Checks existence of Haar Cascade file |

#### Inner Class: ImagePanel

| Method | Description |
|--------|-------------|
| `setImage(image)` | Sets image to display |
| `setDetectionResults(results, showHaar, showGeo, showOverlap)` | Sets detection results and visibility settings |
| `clearOverlays()` | Clears overlays |
| `paintComponent(g)` | Draws image and detection boxes |
| `drawDetectionRect(g2d, rect, color, scale, offsetX, offsetY, label)` | Draws single detection rectangle |

---

### 2. PlateDetector.java

Main plate detection engine containing dual detection system.

#### Configuration Methods

| Method | Description |
|--------|-------------|
| `createDebugDirectories()` | Creates debug output folders |
| `initializeHaarClassifier()` | Loads Haar Cascade classifier |
| `saveDebugImage(stepDir, image, imageName)` | Saves debug image |

#### Parameter Setters

| Method | Description |
|--------|-------------|
| `setBlurKernel(size)` | Sets bilateral filter kernel size (odd number) |
| `setCannyThreshold1(threshold)` | Sets Canny lower threshold value |
| `setCannyThreshold2(threshold)` | Sets Canny upper threshold value |
| `setMinAspectRatio(ratio)` | Sets minimum aspect ratio |
| `setMaxAspectRatio(ratio)` | Sets maximum aspect ratio |
| `setDilateKernelSize(size)` | Sets dilate kernel size |
| `setDilateIterations(iterations)` | Sets dilate iteration count |
| `setHaarScaleFactor(factor)` | Sets Haar scale factor |
| `setHaarMinNeighbors(neighbors)` | Sets Haar minimum neighbor count |

#### Parameter Getters

| Method | Description |
|--------|-------------|
| `getBlurKernel()` | Returns blur kernel value |
| `getCannyThreshold1()` | Returns Canny threshold 1 value |
| `getCannyThreshold2()` | Returns Canny threshold 2 value |
| `getMinAspectRatio()` | Returns minimum aspect ratio value |
| `getMaxAspectRatio()` | Returns maximum aspect ratio value |
| `getDilateKernelSize()` | Returns dilate kernel size |
| `getDilateIterations()` | Returns dilate iteration count |
| `isHaarAvailable()` | Returns whether Haar cascade is available |

#### Intermediate Image Getters

| Method | Description |
|--------|-------------|
| `getOriginalImage()` | Returns original loaded image |
| `getLastGrayImage()` | Returns grayscale image |
| `getLastFilteredImage()` | Returns filtered image |
| `getLastEdgeImage()` | Returns Canny edge image |
| `getLastDilatedImage()` | Returns dilated image |
| `getLastContourImage()` | Returns contour visualization image |
| `getLastResults()` | Returns last detection results |
| `getCurrentImageName()` | Returns current image name |

#### Image Processing Methods

| Method | Description |
|--------|-------------|
| `loadImage(imagePath)` | Loads image from file |
| `preprocess()` | Runs image preprocessing pipeline |
| `preprocessImageWithOriginal(imagePath)` | Loads image, processes and creates debug records |

#### Detection Methods

| Method | Description |
|--------|-------------|
| `detectAll()` | Runs both detection methods and merges results |
| `detectWithHaar()` | Performs plate detection with Haar Cascade |
| `detectWithGeometric()` | Performs plate detection with Geometric/Contour analysis |
| `cropPlateWithPadding(rect, padding)` | Crops plate region with padding |
| `findHighConfidenceDetections()` | Finds regions detected by both methods |

#### Perspective Transform Methods

| Method | Description |
|--------|-------------|
| `fourPointTransform(image, pts)` | Applies 4-point perspective transform |
| `orderPoints(pts)` | Orders corner points (top-left, top-right, bottom-right, bottom-left) |
| `indexOfMin(arr)` | Finds index of minimum value in array |
| `indexOfMax(arr)` | Finds index of maximum value in array |
| `distance(p1, p2)` | Calculates distance between two points |

#### Visualization Methods

| Method | Description |
|--------|-------------|
| `createContourVisualization()` | Visualizes detection results with colored boxes |
| `getDetectionStats()` | Returns detection statistics as [haar, geo, overlap] |

---

### 3. OcrService.java

Tesseract OCR integration and plate text recognition service.

#### Configuration Methods

| Method | Description |
|--------|-------------|
| `initializeTesseract()` | Initializes Tesseract engine and applies settings |
| `findTessdataPath()` | Automatically finds tessdata folder |
| `ensureDebugDir()` | Ensures existence of debug output folder |

#### OCR Methods

| Method | Description |
|--------|-------------|
| `recognizePlate(plateMat, imageName)` | Runs OCR on plate image and returns result |
| `recognizePlate(plateMat)` | Runs OCR without image name |
| `preprocessForOcr(plate, imageName)` | Image preprocessing for OCR (resize, threshold) |
| `cleanResult(raw)` | Cleans OCR result (uppercase, alphanumeric) |

#### Helper Methods

| Method | Description |
|--------|-------------|
| `setTessdataPath(path)` | Sets tessdata path |
| `getTessdataPath()` | Returns current tessdata path |
| `setWhitelist(whitelist)` | Sets character list to be recognized |
| `setLanguage(lang)` | Sets OCR language |
| `isWindows()` | Checks if operating system is Windows |
| `isMac()` | Checks if operating system is macOS |

---

### 4. DetectionResult.java

Data model representing a single plate detection.

#### Enum: MethodType

| Value | Description |
|-------|-------------|
| `HAAR` | Detected with Haar Cascade |
| `GEOMETRIC` | Detected with Geometric analysis |

#### Constructors

| Constructor | Description |
|-------------|-------------|
| `DetectionResult(bounds, method)` | Basic constructor (confidence = 1.0) |
| `DetectionResult(bounds, method, confidence)` | Constructor with confidence value |

#### Getter/Setter Methods

| Method | Description |
|--------|-------------|
| `getBounds()` | Returns detection rectangle (Rect) |
| `getMethod()` | Returns detection method |
| `getConfidence()` | Returns confidence value |
| `getCroppedPlate()` | Returns cropped plate image |
| `setCroppedPlate(mat)` | Sets cropped plate image |
| `getOcrResult()` | Returns OCR result |
| `setOcrResult(text)` | Sets OCR result |

#### Calculation Methods

| Method | Description |
|--------|-------------|
| `calculateIoU(other)` | Calculates IoU with another detection |
| `calculateIoU(r1, r2)` | (static) Calculates IoU between two rectangles |
| `overlaps(other, threshold)` | Checks if overlap exceeds specified threshold |

---

### 5. Main.java

Command line interface and entry point for batch processing.

#### Main Methods

| Method | Description |
|--------|-------------|
| `main(args)` | Application entry point |
| `processDirectory(directory)` | Processes all images in folder |
| `processAndPrintResult(imagePath)` | Processes single image and prints result |
| `processImage(imagePath, expectedPlate)` | Runs complete ALPR pipeline |

#### Helper Methods

| Method | Description |
|--------|-------------|
| `extractExpectedPlate(fileName)` | Extracts expected plate from filename |
| `countMatchingChars(expected, actual)` | Calculates matching character count |
| `calculateScore(text, expected)` | Calculates score of OCR result |

#### Reporting Methods

| Method | Description |
|--------|-------------|
| `printFinalSummary()` | Prints detailed summary report to console |
| `exportResultsToCSV()` | Saves results to timestamped CSV file |
| `exportSummaryRow(timestamp)` | Adds summary row to alpr_summary.csv |
| `truncate(str, maxLen)` | Truncates text to specified length |
| `getStatusSymbol(result)` | Returns symbol for result status (✓, ~, ✗) |

---

### 6. TestImageGenerator.java

Test image generation utility class.

| Method | Description |
|--------|-------------|
| `generateTestImage(outputPath)` | Creates synthetic test image (640x480, simulated plate) |

---

## Parameters and Optimization

### Image Processing Pipeline

```
Original Image
       ↓
1. Grayscale Conversion
       ↓
2. CLAHE (Contrast Limited Adaptive Histogram Equalization)
       ↓
3. Bilateral Filter (blur kernel)
       ↓
4. Canny Edge Detection (threshold1, threshold2)
       ↓
5. Morphological Closing (21x5 kernel)
       ↓
6. Dilation (dilate kernel, iterations)
       ↓
7. Contour Analysis + Aspect Ratio Filtering
       ↓
Detected Plates
```

### Parameter Recommendations

| Parameter | Default | Low Value Effect | High Value Effect |
|-----------|---------|------------------|-------------------|
| **Blur Kernel** | 11 | More noise | Detail loss |
| **Canny T1** | 50 | More edges | Fewer edges |
| **Canny T2** | 150 | More edges | Fewer edges |
| **Dilate Kernel** | 3 | Thin edges | Thick edges |
| **Dilate Iter** | 2 | Gaps | Merged areas |
| **Min AR** | 2.0 | Square regions included | Only rectangles |
| **Max AR** | 7.0 | Narrow plates excluded | Wide regions included |

### Turkish Plate Format

Turkish plates have the following format: `[2 digits][1-3 letters][2-4 digits]`

Examples:
- `34ABC123` (Istanbul)
- `06XY1234` (Ankara)
- `35A12` (Izmir)

---

## Debug Outputs

### Folder Structure

| Folder | Content |
|--------|---------|
| `step1_grayscale/` | Grayscale images |
| `step2_filtered/` | After bilateral filter |
| `step3_canny/` | After Canny edge detection |
| `step3b_dilated/` | After dilation |
| `step4_detected_plate/` | Detected plate regions |
| `step5_ocr_preprocessed/` | Images prepared for OCR |
| `haar_plates/` | Plates detected with Haar |
| `geo_plates/` | Plates detected with Geometric |

### CSV Outputs

1. **alpr_results_YYYYMMDD_HHMMSS.csv**: Detailed result report
2. **alpr_summary.csv**: Summary rows for parameter comparison

---

## Improvement Suggestions

### Short Term

- [ ] GPU acceleration (CUDA) support
- [ ] Adding more Haar Cascade models
- [ ] Special OCR training for Turkish plates
- [ ] Real-time video stream support

### Medium Term

- [ ] Deep Learning based detection (YOLO, SSD)
- [ ] Simultaneous multi-plate detection
- [ ] Character segmentation
- [ ] Plate recognition confidence scoring

### Long Term

- [ ] REST API service
- [ ] Database integration
- [ ] Web interface
- [ ] Mobile application

---

## Contributors

**ALPR Academic Project Team:**

- Mert Ozbay
- Defne Oktem
- Ata Atay
- Ayse Ceren Sarigul
- Aylin Baki
- Ahmad Ali al Ghazi

---

## License

This project is licensed under **GNU General Public License v3.0 (GPL-3.0)**.

This license provides:
- Commercial use
- Modification
- Distribution
- Patent use
- Private use

Conditions:
- Source code must be open
- License and copyright notice must be included
- Same license must be used (copyleft)
- Changes must be stated

For details, see the [LICENSE](LICENSE) file or [GNU GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.html) page.

---

## Troubleshooting

### Common Errors

#### "Haar Cascade file not found"
```bash
# Make sure the Haar cascade file is in the project root directory
ls haarcascade_russian_plate_number.xml
```

#### "Tesseract not found"
```bash
# Add to PATH for Windows or check tessdata folder
dir tessdata\eng.traineddata
```

#### "Could not load OpenCV"
```bash
# Re-download Maven dependencies
mvn clean install -U
```

### Support

Use GitHub Issues for problems or contact the project team.

---

<p align="center">
  <i>Developed for optimization in license plate recognition systems.</i>
</p>
