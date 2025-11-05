import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple file logger that captures System.out, System.err, and exceptions.
 * Logs to telemetry-viewer.log in the current directory.
 */
public class FileLogger {
	
	private static final SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static PrintWriter logWriter;
	private static PrintStream originalOut;
	private static PrintStream originalErr;
	private static boolean initialized = false;
	
	/**
	 * Initializes file logging. Redirects System.out and System.err to both console and log file.
	 * Should be called early in main() method.
	 */
	public static void initialize() {
		
		if(initialized)
			return;
		
		try {
			// Open log file in append mode
			File logFile = new File("telemetry-viewer.log");
			FileOutputStream fos = new FileOutputStream(logFile, true); // append mode
			logWriter = new PrintWriter(fos, true); // autoflush
			
			// Write startup marker
			logWriter.println();
			logWriter.println("========================================");
			logWriter.println("Telemetry Viewer Started: " + timestamp.format(new Date()));
			logWriter.println("========================================");
			logWriter.flush();
			
			// Save original streams
			originalOut = System.out;
			originalErr = System.err;
			
			// Create tee streams that write to both console and file
			PrintStream teeOut = new PrintStream(new TeeOutputStream(originalOut, logWriter, "OUT"));
			PrintStream teeErr = new PrintStream(new TeeOutputStream(originalErr, logWriter, "ERR"));
			
			// Redirect System.out and System.err
			System.setOut(teeOut);
			System.setErr(teeErr);
			
			// Set up global exception handler
			Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
				logException("Uncaught exception in thread: " + thread.getName(), exception);
			});
			
			initialized = true;
			
		} catch(Exception e) {
			// If logging setup fails, just print to console
			System.err.println("Failed to initialize file logging: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Logs an exception with a message to the log file.
	 */
	public static void logException(String message, Throwable exception) {
		
		if(!initialized) {
			// Fallback to console if not initialized
			System.err.println("[" + timestamp.format(new Date()) + "] " + message);
			exception.printStackTrace();
			return;
		}
		
		try {
			logWriter.println("[" + timestamp.format(new Date()) + "] EXCEPTION: " + message);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			logWriter.println(sw.toString());
			logWriter.flush();
		} catch(Exception e) {
			// If logging fails, fall back to console
			System.err.println("Failed to log exception: " + e.getMessage());
			exception.printStackTrace();
		}
		
	}
	
	/**
	 * Explicitly logs a message to the log file.
	 */
	public static void log(String level, String message) {
		
		if(!initialized) {
			System.out.println("[" + timestamp.format(new Date()) + "] [" + level + "] " + message);
			return;
		}
		
		try {
			logWriter.println("[" + timestamp.format(new Date()) + "] [" + level + "] " + message);
			logWriter.flush();
		} catch(Exception e) {
			System.err.println("Failed to log message: " + e.getMessage());
		}
		
	}
	
	/**
	 * Closes the log file. Should be called on application shutdown.
	 */
	public static void close() {
		
		if(!initialized || logWriter == null)
			return;
		
		try {
			logWriter.println("========================================");
			logWriter.println("Telemetry Viewer Stopped: " + timestamp.format(new Date()));
			logWriter.println("========================================");
			logWriter.flush();
			logWriter.close();
			
			// Restore original streams
			if(originalOut != null)
				System.setOut(originalOut);
			if(originalErr != null)
				System.setErr(originalErr);
			
			initialized = false;
			
		} catch(Exception e) {
			System.err.println("Failed to close log file: " + e.getMessage());
		}
		
	}
	
	/**
	 * OutputStream that writes to both a PrintStream (console) and PrintWriter (file).
	 */
	private static class TeeOutputStream extends java.io.OutputStream {
		
		private final PrintStream console;
		private final PrintWriter file;
		private final String prefix;
		
		public TeeOutputStream(PrintStream console, PrintWriter file, String prefix) {
			this.console = console;
			this.file = file;
			this.prefix = prefix;
		}
		
		@Override
		public void write(int b) {
			console.write(b);
			file.write(b);
		}
		
		@Override
		public void write(byte[] b, int off, int len) {
			console.write(b, off, len);
			// Convert bytes to string for PrintWriter
			String str = new String(b, off, len);
			file.write(str, 0, str.length());
		}
		
		@Override
		public void flush() {
			console.flush();
			file.flush();
		}
		
	}
	
}

