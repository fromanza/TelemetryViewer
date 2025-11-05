# Telemetry Viewer

A Java application for visualizing real-time telemetry data from serial ports, TCP/UDP connections, or imported CSV files.

## Requirements

- **Java Development Kit (JDK) 8 or later**
  - Check: `javac -version`
  - Download: https://adoptium.net/

## Building

Run the build script to create a standalone JAR:

```batch
create-fat-jar.bat
```

This creates `out\TelemetryViewer.jar` (~50-100 MB) that includes all dependencies and can be distributed as a single file.

## Running

**Option 1:** Double-click `out\TelemetryViewer.jar`  
**Option 2:** `java -jar out\TelemetryViewer.jar`

## Distribution

The built JAR is standalone and requires:
- Java Runtime Environment (JRE) 8 or later
- No additional files needed

Users can double-click the JAR or run `java -jar TelemetryViewer.jar`.

## Dependencies

All dependencies are included in the `lib/` folder:

**Core Libraries:**
- `commons-math3-3.6.1.jar` - Apache Commons Math (mathematical operations)
- `jSerialComm-2.6.2.jar` - Serial port communication
- `miglayout-4.0-swing.jar` - Layout manager for Swing GUI

**OpenGL/Graphics (JOGL):**
- `jogl-all.jar` - Java OpenGL bindings
- `gluegen-rt.jar` - JOGL native library loader
- `jogl-all-natives-*.jar` - Native libraries (Windows, Linux, macOS)
- `gluegen-rt-natives-*.jar` - Native libraries (Windows, Linux, macOS)

**Web/Networking:**
- `jetty-client-9.4.28.v20200408.jar` - HTTP client
- `jetty-http-9.4.28.v20200408.jar` - HTTP protocol
- `jetty-io-9.4.28.v20200408.jar` - I/O utilities
- `jetty-util-9.4.28.v20200408.jar` - Utility classes
- `websocket-api-9.4.28.v20200408.jar` - WebSocket API
- `websocket-client-9.4.28.v20200408.jar` - WebSocket client
- `websocket-common-9.4.28.v20200408.jar` - WebSocket common

**Media:**
- `webcam-capture-0.3.12.jar` - Webcam support
- `turbojpeg.jar` - JPEG image processing
- `bridj-0.7-20140918.jar` - Native library bindings

**Utilities:**
- `slf4j-api-1.7.2.jar` - Logging API
- `slf4j-nop-1.7.2.jar` - No-operation logger implementation

**Documentation/Source (optional):**
- `*-javadoc.jar` files
- `*-sources.jar` files
- `*.zip` files (source archives)

## Troubleshooting

**"javac not recognized"**
- Install JDK and add to PATH

**"ClassNotFoundException"**
- Ensure all JARs in `lib/` are present

**"UnsatisfiedLinkError" (OpenGL)**
- Native libraries should be in `lib/` (platform-specific JOGL natives)

**Out of Memory**
```batch
java -Xmx2g -jar out\TelemetryViewer.jar
```

## Project Structure

```
Telemetry Viewer/
├── src/              # Java source files
├── lib/              # Dependencies (JAR files)
├── resources/        # Resources (STL files, etc.)
├── test/             # Test files
├── out/              # Build output (generated)
├── create-fat-jar.bat  # Build script
└── README.md         # This file
```

