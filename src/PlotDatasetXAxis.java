import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;

/**
 * Plot class for XY plotting where X-axis uses dataset values instead of time/sample count.
 * Each sample updates the chart with (X-dataset-value, Y-dataset-value) pairs.
 */
public class PlotDatasetXAxis extends Plot {
	
	Dataset xAxisDataset; // Dataset used for X-axis values
	StorageFloats.Cache xAxisCache; // Cache for X-axis dataset
	
	DatasetsController datasetsController;
	
	// Store actual float X-axis values (Plot base class uses long, but we need float)
	float plotMinXFloat; // Actual X-axis minimum value (from dataset)
	float plotMaxXFloat; // Actual X-axis maximum value (from dataset)
	float plotDomainFloat; // Actual X-axis domain (from dataset)
	
	// for non-cached mode
	FloatBuffer   bufferX;  // X-axis values from dataset
	FloatBuffer[] buffersY; // Y-axis values from datasets
	
	// for cached mode
	// TODO: Add DrawCallData inner class when implementing full cached mode support
	int[]     fbHandle;
	int[]     texHandle;
	boolean   cacheIsValid;
	List<Dataset> previousNormalDatasets;
	List<Dataset.Bitfield.State> previousEdgeStates;
	List<Dataset.Bitfield.State> previousLevelStates;
	float         previousPlotMinX;
	float         previousPlotMaxX;
	float         previousPlotMinY;
	float         previousPlotMaxY;
	int           previousPlotWidth;
	int           previousPlotHeight;
	float         previousPlotDomain; // Changed from long to float for dataset values
	float         previousLineWidth;
	long          previousMinSampleNumber;
	long          previousMaxSampleNumber;
	Dataset       previousXAxisDataset;
	
	/**
	 * Step 1: (Required) Calculate the domain and range of the plot.
	 * 
	 * @param endTimestamp      Ignored for XY plots.
	 * @param endSampleNumber   Sample number corresponding with the right edge of the plot. NOTE: this sample might not exist yet!
	 * @param zoomLevel         Current zoom level. 1.0 = no zoom.
	 * @param datasets          Normal/edge/level datasets to acquire from (Y-axis datasets).
	 * @param timestampCache    Ignored for XY plots.
	 * @param duration          The sample count to acquire, before applying the zoom factor.
	 * @param cachedMode        True to enable the cache.
	 * @param showTimestamps    Ignored for XY plots.
	 */
	@Override public void initialize(long endTimestamp, long endSampleNumber, double zoomLevel, DatasetsInterface datasets, StorageTimestamps.Cache timestampsCache, long duration, boolean cachedMode, boolean showTimestamps) {
		
		FileLogger.log("INFO", "PlotDatasetXAxis.initialize() called - endSampleNumber=" + endSampleNumber + ", duration=" + duration + ", zoomLevel=" + zoomLevel);
		
		this.datasets = datasets;
		this.cachedMode = cachedMode;
		xAxisTitle = "X-Axis Dataset"; // Will be updated when X-axis dataset is set
		
		datasetsController = datasets.hasAnyType() ? datasets.connection.datasets : null;
		int sampleCount = datasetsController == null ? 0 : datasetsController.getSampleCount();
		
		// Exit early if no samples or no X-axis dataset
		if(sampleCount == 0 || xAxisDataset == null) {
			FileLogger.log("WARN", "PlotDatasetXAxis.initialize() - No samples (" + sampleCount + ") or no X-axis dataset");
			maxSampleNumber = -1;
			minSampleNumber = -1;
			plotSampleCount = 0;
			samplesMinY = -1;
			samplesMaxY = 1;
			plotMinX = 0;
			plotMaxX = 1;
			plotDomain = 1;
			plotMinXFloat = 0;
			plotMaxXFloat = 1;
			plotDomainFloat = 1;
			return;
		}
		
		// Calculate sample range (for selecting which samples to display)
		long sampleDomain = Math.round(duration * zoomLevel);
		if(sampleDomain < 2)
			sampleDomain = 2;
		long trueLastSampleNumber = sampleCount - 1;
		maxSampleNumber = Long.min(endSampleNumber, trueLastSampleNumber);
		minSampleNumber = maxSampleNumber - sampleDomain + 1;
		if(minSampleNumber < 0)
			minSampleNumber = 0;
		
		plotSampleCount = maxSampleNumber - minSampleNumber + 1;
		
		if(plotSampleCount < 2) {
			FileLogger.log("WARN", "PlotDatasetXAxis.initialize() - No valid sample range: count=" + plotSampleCount);
			maxSampleNumber = -1;
			minSampleNumber = -1;
			plotSampleCount = 0;
			samplesMinY = -1;
			samplesMaxY = 1;
			plotMinX = 0;
			plotMaxX = 1;
			plotDomain = 1;
			plotMinXFloat = 0;
			plotMaxXFloat = 1;
			plotDomainFloat = 1;
			return;
		}
		
		// Get X-axis dataset range (the actual float values)
		try {
			StorageFloats.MinMax xRange = xAxisDataset.getRange((int) minSampleNumber, (int) maxSampleNumber, xAxisCache);
			plotMinXFloat = xRange.min;
			plotMaxXFloat = xRange.max;
			plotDomainFloat = plotMaxXFloat - plotMinXFloat;
			if(plotDomainFloat <= 0) {
				// Handle case where all X values are the same
				plotDomainFloat = 1;
				plotMinXFloat = xRange.min - 0.5f;
				plotMaxXFloat = xRange.max + 0.5f;
			}
			// Store in base class fields as well (cast to long for compatibility)
			plotMinX = (long) plotMinXFloat;
			plotMaxX = (long) plotMaxXFloat;
			plotDomain = (long) plotDomainFloat;
			FileLogger.log("INFO", "PlotDatasetXAxis.initialize() - X-axis range: [" + plotMinXFloat + " to " + plotMaxXFloat + "], domain=" + plotDomainFloat);
		} catch(Exception e) {
			FileLogger.logException("Error getting X-axis dataset range", e);
			plotMinXFloat = -1;
			plotMaxXFloat = 1;
			plotDomainFloat = 2;
			plotMinX = -1;
			plotMaxX = 1;
			plotDomain = 2;
		}
		
		// Get Y-axis datasets range
		float[] range = datasets.getRange((int) minSampleNumber, (int) maxSampleNumber);
		samplesMinY = range[0];
		samplesMaxY = range[1];
		
		FileLogger.log("INFO", "PlotDatasetXAxis.initialize() - Y-axis range: [" + samplesMinY + " to " + samplesMaxY + "]");
		FileLogger.log("INFO", "PlotDatasetXAxis.initialize() - Sample range: " + minSampleNumber + " to " + maxSampleNumber + " (count=" + plotSampleCount + ")");
		
	}
	
	/**
	 * Sets the dataset to use for X-axis values.
	 * 
	 * @param dataset    The dataset to use for X-axis, or null to disable.
	 */
	public void setXAxisDataset(Dataset dataset) {
		
		FileLogger.log("INFO", "PlotDatasetXAxis.setXAxisDataset() - Setting X-axis dataset: " + (dataset == null ? "null" : dataset.name));
		
		xAxisDataset = dataset;
		if(dataset != null) {
			xAxisCache = dataset.createCache();
			xAxisTitle = dataset.name + " (" + dataset.unit + ")";
		} else {
			xAxisCache = null;
			xAxisTitle = "X-Axis Dataset";
		}
		
	}
	
	// steps 2 and 3 are handled by the Plot class
	
	/**
	 * Step 4: Get the x-axis divisions.
	 * 
	 * @param gl           The OpenGL context.
	 * @param plotWidth    The width of the plot region, in pixels.
	 * @return             A Map where each value is a string to draw on screen, and each key is the pixelX location for it (0 = left edge of the plot)
	 */
	@Override public Map<Float, String> getXdivisions(GL2ES3 gl, float plotWidth) {
		
		Map<Float, String> divisions = new HashMap<Float, String>();
		
		// sanity check
		if(plotWidth < 1 || plotDomainFloat <= 0 || xAxisDataset == null)
			return divisions;
		
		FileLogger.log("DEBUG", "PlotDatasetXAxis.getXdivisions() - plotWidth=" + plotWidth + ", plotMinXFloat=" + plotMinXFloat + ", plotMaxXFloat=" + plotMaxXFloat);
		
		// Use ChartUtils.getFloatXdivisions125 for float values (handles large numbers better)
		try {
			Map<Float, String> floatDivisions = ChartUtils.getFloatXdivisions125(gl, plotWidth, plotMinXFloat, plotMaxXFloat);
			
			// Convert from data-space X values to pixel-space positions
			for(Map.Entry<Float, String> entry : floatDivisions.entrySet()) {
				float xValue = entry.getKey();
				float pixelX = (xValue - plotMinXFloat) / plotDomainFloat * plotWidth;
				divisions.put(pixelX, entry.getValue());
			}
		} catch(Exception e) {
			FileLogger.logException("Error calculating X-axis divisions", e);
			// Fallback: try to create at least a few divisions
			if(plotDomainFloat > 0 && plotWidth > 0) {
				// Create simple divisions at 0, 25%, 50%, 75%, 100%
				for(int i = 0; i <= 4; i++) {
					float pixelX = (plotWidth * i) / 4.0f;
					float xValue = plotMinXFloat + (plotDomainFloat * i) / 4.0f;
					divisions.put(pixelX, ChartUtils.formattedNumber(xValue, 5));
				}
			}
		}
		
		FileLogger.log("DEBUG", "PlotDatasetXAxis.getXdivisions() - returning " + divisions.size() + " divisions");
		
		return divisions;
		
	}
	
	/**
	 * Step 5: Acquire the samples (non-cached mode).
	 * 
	 * @param plotMinY      Y-axis value at the bottom of the plot.
	 * @param plotMaxY      Y-axis value at the top of the plot.
	 * @param plotWidth     Width of the plot region, in pixels.
	 * @param plotHeight    Height of the plot region, in pixels.
	 */
	@Override void acquireSamplesNonCachedMode(float plotMinY, float plotMaxY, int plotWidth, int plotHeight) {
		
		FileLogger.log("DEBUG", "PlotDatasetXAxis.acquireSamplesNonCachedMode() - Acquiring samples " + minSampleNumber + " to " + maxSampleNumber);
		
		if(xAxisDataset == null) {
			FileLogger.log("WARN", "PlotDatasetXAxis.acquireSamplesNonCachedMode() - No X-axis dataset set!");
			return;
		}
		
		events = new BitfieldEvents(true, false, datasets, (int) minSampleNumber, (int) maxSampleNumber);
		
		// Load X-axis dataset values
		try {
			bufferX = xAxisDataset.getSamplesBuffer((int) minSampleNumber, (int) maxSampleNumber, xAxisCache);
			FileLogger.log("DEBUG", "PlotDatasetXAxis.acquireSamplesNonCachedMode() - Loaded X-axis buffer: " + bufferX.capacity() + " values");
		} catch(Exception e) {
			FileLogger.logException("Error loading X-axis dataset buffer", e);
			return;
		}
		
		// Load Y-axis dataset values
		buffersY = new FloatBuffer[datasets.normalsCount()];
		for(int datasetN = 0; datasetN < datasets.normalsCount(); datasetN++) {
			Dataset dataset = datasets.getNormal(datasetN);
			if(!dataset.isBitfield) {
				try {
					buffersY[datasetN] = datasets.getSamplesBuffer(dataset, (int) minSampleNumber, (int) maxSampleNumber);
					FileLogger.log("DEBUG", "PlotDatasetXAxis.acquireSamplesNonCachedMode() - Loaded Y-axis buffer[" + datasetN + "] (" + dataset.name + "): " + buffersY[datasetN].capacity() + " values");
				} catch(Exception e) {
					FileLogger.logException("Error loading Y-axis dataset buffer for " + dataset.name, e);
				}
			}
		}
		
	}
	
	/**
	 * Step 5: Acquire the samples (cached mode).
	 * 
	 * @param plotMinY      Y-axis value at the bottom of the plot.
	 * @param plotMaxY      Y-axis value at the top of the plot.
	 * @param plotWidth     Width of the plot region, in pixels.
	 * @param plotHeight    Height of the plot region, in pixels.
	 */
	@Override void acquireSamplesCachedMode(float plotMinY, float plotMaxY, int plotWidth, int plotHeight) {
		
		FileLogger.log("DEBUG", "PlotDatasetXAxis.acquireSamplesCachedMode() - Cached mode not yet implemented, using non-cached");
		// For now, use non-cached mode
		// TODO: Implement cached mode similar to PlotMilliseconds
		acquireSamplesNonCachedMode(plotMinY, plotMaxY, plotWidth, plotHeight);
		
	}
	
	/**
	 * Step 6: Render the plot on screen (non-cached mode).
	 * 
	 * @param gl             The OpenGL context.
	 * @param chartMatrix    The current 4x4 matrix.
	 * @param xPlotLeft      Bottom-left corner location, in pixels.
	 * @param yPlotBottom    Bottom-left corner location, in pixels.
	 * @param plotWidth      Width of the plot region, in pixels.
	 * @param plotHeight     Height of the plot region, in pixels.
	 * @param plotMinY       Y-axis value at the bottom of the plot.
	 * @param plotMaxY       Y-axis value at the top of the plot.
	 */
	@Override public void drawNonCachedMode(GL2ES3 gl, float[] chartMatrix, int xPlotLeft, int yPlotBottom, int plotWidth, int plotHeight, float plotMinY, float plotMaxY) {
		
		if(plotSampleCount < 2 || bufferX == null) {
			FileLogger.log("DEBUG", "PlotDatasetXAxis.drawNonCachedMode() - Skipping draw: sampleCount=" + plotSampleCount + ", bufferX=" + (bufferX == null ? "null" : "ok"));
			return;
		}
		
		float plotRange = plotMaxY - plotMinY;
		if(plotRange <= 0)
			plotRange = 1;
		
		// clip to the plot region
		int[] originalScissorArgs = new int[4];
		gl.glGetIntegerv(GL3.GL_SCISSOR_BOX, originalScissorArgs, 0);
		gl.glScissor(originalScissorArgs[0] + (int) xPlotLeft, originalScissorArgs[1] + (int) yPlotBottom, plotWidth, plotHeight);
		
		float[] plotMatrix = Arrays.copyOf(chartMatrix, 16);
		// adjust so: x = (x - plotMinXFloat) / plotDomainFloat * plotWidth + xPlotLeft;
		// adjust so: y = (y - plotMinY) / plotRange * plotHeight + yPlotBottom;
		OpenGL.translateMatrix(plotMatrix, xPlotLeft, yPlotBottom, 0);
		OpenGL.scaleMatrix(plotMatrix, (float) plotWidth / plotDomainFloat, (float) plotHeight / plotRange, 1);
		OpenGL.translateMatrix(plotMatrix, -plotMinXFloat, -plotMinY, 0);
		OpenGL.useMatrix(gl, plotMatrix);
		
		// draw each dataset
		if(plotSampleCount >= 2) {
			for(int i = 0; i < datasets.normalsCount(); i++) {
				
				Dataset dataset = datasets.getNormal(i);
				if(dataset.isBitfield || buffersY[i] == null)
					continue;
				
				try {
					OpenGL.drawLinesX_Y(gl, GL3.GL_LINE_STRIP, dataset.glColor, bufferX, buffersY[i], (int) plotSampleCount);
					
					// also draw points if there are relatively few samples on screen
					boolean fewSamplesOnScreen = (plotWidth / plotSampleCount) > (2 * Theme.pointWidth);
					if(fewSamplesOnScreen)
						OpenGL.drawPointsX_Y(gl, dataset.glColor, bufferX, buffersY[i], (int) plotSampleCount);
					
					FileLogger.log("DEBUG", "PlotDatasetXAxis.drawNonCachedMode() - Drew dataset " + dataset.name);
				} catch(Exception e) {
					FileLogger.logException("Error drawing dataset " + dataset.name, e);
				}
			}
		}
		
		OpenGL.useMatrix(gl, chartMatrix);
		
		// Note: Bitfield markers not implemented for XY plots yet
		// TODO: Implement bitfield markers if needed
		
		// stop clipping to the plot region
		gl.glScissor(originalScissorArgs[0], originalScissorArgs[1], originalScissorArgs[2], originalScissorArgs[3]);
		
	}
	
	/**
	 * Step 6: Render the plot on screen (cached mode).
	 * 
	 * @param gl             The OpenGL context.
	 * @param chartMatrix    The current 4x4 matrix.
	 * @param xPlotLeft      Bottom-left corner location, in pixels.
	 * @param yPlotBottom    Bottom-left corner location, in pixels.
	 * @param plotWidth      Width of the plot region, in pixels.
	 * @param plotHeight     Height of the plot region, in pixels.
	 * @param plotMinY       Y-axis value at the bottom of the plot.
	 * @param plotMaxY       Y-axis value at the top of the plot.
	 */
	@Override public void drawCachedMode(GL2ES3 gl, float[] chartMatrix, int xPlotLeft, int yPlotBottom, int plotWidth, int plotHeight, float plotMinY, float plotMaxY) {
		
		FileLogger.log("DEBUG", "PlotDatasetXAxis.drawCachedMode() - Cached mode not yet implemented, using non-cached");
		// For now, use non-cached mode
		drawNonCachedMode(gl, chartMatrix, xPlotLeft, yPlotBottom, plotWidth, plotHeight, plotMinY, plotMaxY);
		
	}
	
	/**
	 * Step 7: Check if a tooltip should be drawn for the mouse's current location.
	 * 
	 * @param mouseX       The mouse's location along the x-axis, in pixels (0 = left edge of the plot)
	 * @param plotWidth    Width of the plot region, in pixels.
	 * @return             An object indicating if the tooltip should be drawn, for what sample number, with what label, and at what location on screen.
	 */
	@Override public TooltipInfo getTooltip(int mouseX, float plotWidth) {
		
		if(plotSampleCount < 2 || plotDomainFloat <= 0 || bufferX == null)
			return new TooltipInfo(false, -1, "", 0);
		
		// Convert mouse X pixel to dataset X value
		float xValue = plotMinXFloat + (mouseX / plotWidth * plotDomainFloat);
		
		// Find closest sample
		float minDistance = Float.MAX_VALUE;
		int closestSample = -1;
		
		try {
			bufferX.position(0);
			for(int i = 0; i < plotSampleCount && i < bufferX.capacity(); i++) {
				float sampleX = bufferX.get(i);
				float distance = Math.abs(sampleX - xValue);
				if(distance < minDistance) {
					minDistance = distance;
					closestSample = (int) minSampleNumber + i;
				}
			}
		} catch(Exception e) {
			FileLogger.logException("Error in getTooltip", e);
			return new TooltipInfo(false, -1, "", 0);
		}
		
		if(closestSample >= 0) {
			bufferX.position(closestSample - (int) minSampleNumber);
			float sampleX = bufferX.get();
			float pixelX = (sampleX - plotMinXFloat) / plotDomainFloat * plotWidth;
			String label = "Sample " + closestSample + " (X=" + ChartUtils.formattedNumber(sampleX, 5) + ")";
			return new TooltipInfo(true, closestSample, label, pixelX);
		}
		
		return new TooltipInfo(false, -1, "", 0);
		
	}
	
	/**
	 * Gets the horizontal location, relative to the plot, for a sample number.
	 * 
	 * @param sampleNumber    The sample number.
	 * @param plotWidth       Width of the plot region, in pixels.
	 * @return                Corresponding horizontal location on the plot, in pixels, with 0 = left edge of the plot.
	 */
	@Override public float getPixelXforSampleNumber(long sampleNumber, float plotWidth) {
		
		if(xAxisDataset == null || plotDomainFloat <= 0 || sampleNumber < minSampleNumber || sampleNumber > maxSampleNumber)
			return 0;
		
		try {
			float xValue = xAxisDataset.getSample((int) sampleNumber, xAxisCache);
			return (xValue - plotMinXFloat) / plotDomainFloat * plotWidth;
		} catch(Exception e) {
			FileLogger.logException("Error in getPixelXforSampleNumber", e);
			return 0;
		}
		
	}
	
	@Override public void freeResources(GL2ES3 gl) {
		
		if(fbHandle != null) {
			gl.glDeleteFramebuffers(1, fbHandle, 0);
			fbHandle = null;
		}
		if(texHandle != null) {
			gl.glDeleteTextures(1, texHandle, 0);
			texHandle = null;
		}
		
	}

}
