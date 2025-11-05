# Telemetry Viewer - Updates & Feature Requests

This document tracks planned improvements, bug fixes, and feature requests for the Telemetry Viewer project.

---

---

## 🐛 Bug Fixes & Issues

### ✅ Fixed

#### StringIndexOutOfBoundsException in formattedNumber()
**Status:** Fixed  
**Description:** Fix for `StringIndexOutOfBoundsException` in `ChartUtils.formattedNumber()`  
**Fix:** Added length check before calling `substring()` to prevent index out of bounds  
**Location:** `src/ChartUtils.java:450`

#### Binary Processors for uint32 and int16
**Status:** Already Implemented  
**Description:** Added processors for uint32 (LSB/MSB First) and int16 (LSB/MSB First)  
**Location:** `src/DatasetsController.java:56-91`  
**Note:** Processors were already present, verified and corrected int16 signed value handling

### High Priority

#### #70 - Radiomaster ELRS Nomad Module not working
**Status:** Open  
**Description:** Radiomaster ELRS Nomad Module not working with Telemetry Viewer  
**Analysis Needed:** Investigate ELRS protocol compatibility, packet format, sync word detection

#### #62 - Never achieving sync with initial ascii response
**Status:** Open  
**Description:** Sync issues with initial ASCII response  
**Analysis Needed:** Review sync word detection logic in `SharedByteStream.readPackets()`, may need to handle initial data differently

#### #48 - Problem with "inf" or "nan" in serial stream
**Status:** Investigated - Awaiting Testing  
**Description:** Handling of infinite/NaN values in CSV stream  
**Impact:** 
- `Float.parseFloat("inf")` returns `Float.POSITIVE_INFINITY` (doesn't throw)
- `Float.parseFloat("nan")` returns `Float.NaN` (doesn't throw)
- These values propagate to datasets and break min/max calculations
- NaN comparisons (`f < min`) are always false, corrupting range tracking
- Infinity values can corrupt min/max block tracking

**Affected Locations:**
1. CSV file import: `ConnectionTelemetry.java:1484` - `Float.parseFloat(tokens[columnN])`
2. Live CSV streaming: `ConnectionTelemetry.java:1725` - `Float.parseFloat(tokens[i])`
3. Binary processing: `ConnectionTelemetry.java:2034-2043` - Min/max calculations with NaN/Infinity

**Investigation Findings:**
- Skipping samples would create gaps (0.0 values) that break charts/timestamps
- Better approach: Replace NaN/Infinity with last valid sample value
- Maintains sequential sample numbering and timestamp alignment
- Prevents chart rendering artifacts and min/max corruption

**Proposed Solution:** 
- Validate after `Float.parseFloat()` using `Float.isNaN()` and `Float.isInfinite()`
- Replace invalid values with last valid sample (per dataset) or skip sample count increment
- Show notification when invalid values detected
- Test with real serial device to validate approach

#### #40 - Version 0.7 hangs at startup on macOS High Sierra (10.13.6)
**Status:** Open  
**Description:** Startup hang on older macOS  
**Analysis Needed:** Likely JOGL/OpenGL initialization issue, may need fallback for older systems

### Medium Priority

#### #47 - Is there a key that will clear all the data?
**Status:** Open  
**Description:** Keyboard shortcut to clear all data  
**Current:** Data clearing requires manual disconnect  
**Enhancement:** Add keyboard shortcut (e.g., Ctrl+Shift+C) to clear all connections

---

## ✨ Feature Requests

### Data Export & Import

#### #54 - Periodic data export
**Status:** Open  
**Description:** Automatically export data at regular intervals  
**Use Case:** Continuous logging without manual intervention  
**Implementation:** 
- Add settings for export interval (e.g., every 5 minutes)
- Auto-increment filenames
- Background export thread

#### #42 - Custom X-Axis from data stream
**Status:** Open  
**Description:** Use a dataset value as X-axis instead of time/sample count  
**Use Case:** Plotting one dataset vs another over time (e.g., voltage vs current, creating XY trajectory plots)  
**Implementation:** 
- New chart type: "XY Plot" or add option to "Time Domain"
- New `PlotDatasetXAxis` class (similar to `PlotMilliseconds` but uses dataset values instead of timestamps)
- Widget to select X-axis dataset
- Uses `OpenGL.drawLinesX_Y()` for rendering

**Note:** Different from Acceleration Meter:
- **Acceleration Meter**: Shows current X,Y position as a point on a 2D meter (live indicator)
- **Custom X-Axis**: XY line/scatter plot showing relationship between datasets over time (trajectory plot)

#### #45 - Time axis from timestamp in the data?
**Status:** Open  
**Description:** Use actual timestamps from data for X-axis instead of system time  
**Use Case:** When importing data with embedded timestamps  
**Current:** Uses system time during live capture  
**Enhancement:** Allow using dataset timestamps for X-axis

---

### Data Functions & Analysis

#### #50 - Ability to add annotations
**Status:** Open  
**Description:** Add text annotations/markers to charts  
**Use Case:** Mark events, add notes, highlight regions  
**Implementation:**
- Add annotation widget/button
- Store annotations with timestamps/sample numbers
- Render as vertical lines or text boxes

#### #2 - Add ability to see underlying values
**Status:** Open  
**Description:** Show raw data values (tooltip or side panel)  
**Current:** Charts show visual representation only  
**Enhancement:** 
- Enhanced tooltips showing exact values
- Data table view
- Value inspection panel

---

### Binary Protocol Enhancements

#### #26 - Support for 32 bit int, 64 bit float (binary)
**Status:** Open  
**Description:** Add support for int32 and double (float64) in binary mode  
**Current:** Only supports int16, uint16, uint32, float32  
**Implementation:** Add new `BinaryFieldProcessor` types:
- `int32 LSB First` / `int32 MSB First`
- `uint64 LSB First` / `uint64 MSB First`
- `int64 LSB First` / `int64 MSB First`
- `float64 (double) LSB First` / `float64 MSB First`

#### #24 - Calculating binary checksum?
**Status:** Open  
**Description:** Need to calculate checksum (not just verify)  
**Current:** Only validates incoming checksums  
**Enhancement:** Add checksum calculation for export/transmit

---

### Chart Enhancements

#### #39 - Allowing user to select a STL model for use in the quaternion chart
**Status:** Open  
**Description:** Custom 3D model for quaternion visualization  
**Current:** Uses `monkey.stl` hardcoded  
**Implementation:**
- File picker for STL selection
- Store in settings
- Support multiple models

#### #29 - Linear scaling of Frequency Domain (Live view) with manual x-axis limit selection
**Status:** Open  
**Description:** Manual frequency range selection for FFT charts  
**Current:** Auto-scales based on data  
**Enhancement:** Add min/max frequency inputs (similar to Y-axis controls)

---

### Connection & Protocol

#### #41 - Different sampling rates
**Status:** Open  
**Description:** Support different sampling rates per dataset/connection  
**Current:** Single sample rate per connection  
**Use Case:** Multiple sensors with different rates  
**Enhancement:** Per-dataset timestamp offsets or independent rate tracking

#### #22 - Additional WebSocket server connection option
**Status:** Open  
**Description:** WebSocket server support (currently only TCP/UDP)  
**Use Case:** Web-based clients, browser integration  
**Implementation:** Add WebSocket server mode using existing Jetty libraries

---

### Build & Development

#### Build System - Maven/Gradle
**Status:** Open  
**Description:** Add Maven or Gradle build file  
**Current:** Manual batch script  
**Enhancement:** Add `pom.xml` or `build.gradle` for easier dependency management

---

## 🎯 Priority Classification

### P0 - Critical Bugs (Fix First)
- #62 - Sync issues with ASCII
- #48 - NaN/Inf handling
- #40 - macOS startup hang

### P1 - High-Value Features
- #54 - Periodic export
- #50 - Annotations
- #42 - Custom X-axis
- #26 - int32/float64 support

### P2 - Nice-to-Have
- #70 - ELRS compatibility
- #45 - Timestamp-based time axis
- #39 - Custom STL models
- #29 - Manual frequency range

### P3 - Future Enhancements
- #47 - Clear data shortcut
- #41 - Different sampling rates
- #22 - WebSocket server
- #24 - Checksum calculation
- #2 - Value inspection

---

## 📝 Implementation Notes

### Quick Wins (Low Effort, High Impact)
1. **#48 - NaN/Inf handling:** Add try-catch and validation around `Float.parseFloat()`
2. **#47 - Clear data shortcut:** Add keyboard listener in `Main.java`
3. **#24 - Checksum calculation:** Add method to `BinaryChecksumProcessor` interface

### Medium Complexity
1. **#54 - Periodic export:** Extend existing export system with timer
2. **#50 - Annotations:** Add new widget type and rendering code
3. **#42 - Custom X-axis:** Modify chart rendering to support dataset as X-axis

### High Complexity
1. **#26 - int32/float64:** Add new binary processors (requires testing)
2. **#41 - Different sampling rates:** Architecture change for timestamp handling
3. **#22 - WebSocket server:** New connection type implementation

---

## 🔗 Related to Performance Analysis

Several of these issues relate to the performance analysis:

- **#62 (Sync issues)** → Related to `SharedByteStream` synchronization bottlenecks
- **#48 (NaN/Inf)** → Error handling improvements (reliability)
- **#54 (Periodic export)** → Can leverage optimized export path from performance fixes
- **#41 (Different rates)** → May require storage system improvements

---

## 📋 Template for New Items

When adding new items, use this format:

```markdown
#### #XXX - Title
**Status:** Open / In Progress / Completed  
**Priority:** P0/P1/P2/P3  
**Description:** Brief description  
**Analysis:** What we know about the issue  
**Implementation Notes:** How to fix/implement  
**Related Issues:** Links to other items
```

---

## 🚀 Roadmap Suggestions

### Phase 1: Stability & Reliability
- Fix sync issues (#62)
- Handle NaN/Inf (#48)
- Add error recovery improvements

### Phase 2: Core Features
- Periodic export (#54)
- Annotations (#50)
- Custom X-axis (#42)

### Phase 3: Protocol Enhancements
- int32/float64 support (#26)
- Checksum calculation (#24)
- WebSocket server (#22)

### Phase 4: Polish & UX
- Keyboard shortcuts (#47)
- Value inspection (#2)
- Custom STL models (#39)

