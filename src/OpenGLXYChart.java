import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL3;

/**
 * Renders an XY plot chart where X-axis is a dataset value and Y-axis is one or more datasets.
 * 
 * User settings:
 *     X-axis dataset selection.
 *     Y-axis datasets to visualize.
 *     X-axis minimum value can be fixed or autoscaled.
 *     X-axis maximum value can be fixed or autoscaled.
 *     Y-axis minimum value can be fixed or autoscaled.
 *     Y-axis maximum value can be fixed or autoscaled.
 *     Sample count (for how many samples to display).
 *     X-axis title can be displayed.
 *     X-axis scale can be displayed.
 *     Y-axis title can be displayed.
 *     Y-axis scale can be displayed.
 *     Legend can be displayed.
 */
public class OpenGLXYChart extends PositionedChart {
	
	
	// plot region
	float xPlotLeft;
	float xPlotRight;
	float plotWidth;
	float yPlotTop;
	float yPlotBottom;
	float plotHeight;
	float plotMaxY;
	float plotMinY;
	float plotMaxX;
	float plotMinX;
	float plotRangeY;
	float plotRangeX;
	
	// x-axis title
	boolean showXaxisTitle;
	float yXaxisTitleTextBaseline;
	float yXaxisTitleTextTop;
	String xAxisTitle;
	float xXaxisTitleTextLeft;
	
	// y-axis title
	boolean showYaxisTitle;
	float xYaxisTitleTextTop;
	float xYaxisTitleTextBaseline;
	String yAxisTitle;
	float yYaxisTitleTextLeft;
	
	// x-axis scale
	boolean showXaxisScale;
	float yXaxisTickTextBaseline;
	float yXaxisTickTextTop;
	float yXaxisTickBottom;
	float yXaxisTickTop;
	Map<Float, String> xDivisions;
	
	// y-axis scale
	boolean showYaxisScale;
	Map<Float, String> yDivisions;
	float xYaxisTickTextRight;
	float xYaxisTickLeft;
	float xYaxisTickRight;
	
	// legend
	boolean showLegend;
	float xLegendBorderLeft;
	float yLegendBorderBottom;
	float yLegendTextBaseline;
	float yLegendTextTop;
	float yLegendBorderTop;
	float[][] legendMouseoverCoordinates;
	float[][] legendBoxCoordinates;
	float[] xLegendNameLeft;
	float xLegendBorderRight;
	
	AutoScale autoscaleX;
	AutoScale autoscaleY;
	boolean autoscaleXmin;
	boolean autoscaleXmax;
	boolean autoscaleYmin;
	boolean autoscaleYmax;
	float manualXmin;
	float manualXmax;
	float manualYmin;
	float manualYmax;
	
	Plot plot;
	boolean cachedMode;
	List<Dataset> allDatasets;
	Dataset xAxisDataset; // The dataset used for X-axis values
	StorageFloats.Cache xAxisCache;
	long duration; // Number of samples to display (default value)
	boolean squareScale; // If true, maintain 1:1 aspect ratio (same units per pixel for X and Y)
	
	static final float axisMinimumDefault = -1.0f;
	static final float axisMaximumDefault =  1.0f;
	static final float axisLowerLimit     = -Float.MAX_VALUE;
	static final float axisUpperLimit     =  Float.MAX_VALUE;
	
	// control widgets
	WidgetDatasets yDatasetsWidget; // Widget for selecting Y-axis datasets
	WidgetXAxisDataset xAxisDatasetWidget; // Widget for selecting X-axis dataset (to be created)
	WidgetTextfieldsOptionalMinMax xMinMaxWidget;
	WidgetTextfieldsOptionalMinMax yMinMaxWidget;
	WidgetCheckbox showXaxisTitleWidget;
	WidgetCheckbox showXaxisScaleWidget;
	WidgetCheckbox showYaxisTitleWidget;
	WidgetCheckbox showYaxisScaleWidget;
	WidgetCheckbox showLegendWidget;
	WidgetCheckbox cachedWidget;
	
	@Override public String toString() {
		
		return "XY Plot";
		
	}
	
	public OpenGLXYChart(int x1, int y1, int x2, int y2) {
		
		super(x1, y1, x2, y2);
		
		FileLogger.log("XY Plot", "Creating new XY Plot chart at (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
		
		autoscaleX = new AutoScale(AutoScale.MODE_EXPONENTIAL, 30, 0.10f);
		autoscaleY = new AutoScale(AutoScale.MODE_EXPONENTIAL, 30, 0.10f);
		
		// Default duration: 1000 samples
		duration = 1000;
		squareScale = false; // Default to auto scale
		
		// Create the Plot instance
		plot = new PlotDatasetXAxis();
		
		// X-axis dataset widget
		xAxisDatasetWidget = new WidgetXAxisDataset(newDataset -> {
			xAxisDataset = newDataset;
			((PlotDatasetXAxis) plot).setXAxisDataset(newDataset);
			updateAllDatasetsList();
			FileLogger.log("XY Plot", "X-axis dataset changed to: " + (newDataset != null ? newDataset.name : "null"));
		});
		
		// Y-axis datasets widget with duration control
		yDatasetsWidget = new WidgetDatasets(newDatasets -> {
			FileLogger.log("XY Plot", "Y-axis datasets changed. Count: " + (newDatasets != null ? newDatasets.size() : 0));
			datasets.setNormals(newDatasets);
			updateAllDatasetsList();
		},
		null, // no bitfield edges
		null, // no bitfield levels
		(newAxisType, newDuration) -> {
			// Duration handler - only sample count mode supported for XY plots
			duration = (long) newDuration;
			FileLogger.log("XY Plot", "Sample count changed to: " + duration);
			return duration;
		},
		false, // only sample count, no time-based duration
		null);
		
		// X-axis min/max widget
		xMinMaxWidget = new WidgetTextfieldsOptionalMinMax("X-Axis",
		                                                   true,
		                                                   axisMinimumDefault,
		                                                   axisMaximumDefault,
		                                                   axisLowerLimit,
		                                                   axisUpperLimit,
		                                                   (newAutoscaleXmin, newManualXmin) -> {
		                                                       autoscaleXmin = newAutoscaleXmin;
		                                                       manualXmin = newManualXmin;
		                                                       FileLogger.log("XY Plot", "X-axis min changed: autoscale=" + newAutoscaleXmin + ", value=" + newManualXmin);
		                                                   },
		                                                   (newAutoscaleXmax, newManualXmax) -> {
		                                                       autoscaleXmax = newAutoscaleXmax;
		                                                       manualXmax = newManualXmax;
		                                                       FileLogger.log("XY Plot", "X-axis max changed: autoscale=" + newAutoscaleXmax + ", value=" + newManualXmax);
		                                                   });
		
		// Y-axis min/max widget
		yMinMaxWidget = new WidgetTextfieldsOptionalMinMax("Y-Axis",
		                                                   true,
		                                                   axisMinimumDefault,
		                                                   axisMaximumDefault,
		                                                   axisLowerLimit,
		                                                   axisUpperLimit,
		                                                   (newAutoscaleYmin, newManualYmin) -> {
		                                                       autoscaleYmin = newAutoscaleYmin;
		                                                       manualYmin = newManualYmin;
		                                                       FileLogger.log("XY Plot", "Y-axis min changed: autoscale=" + newAutoscaleYmin + ", value=" + newManualYmin);
		                                                   },
		                                                   (newAutoscaleYmax, newManualYmax) -> {
		                                                       autoscaleYmax = newAutoscaleYmax;
		                                                       manualYmax = newManualYmax;
		                                                       FileLogger.log("XY Plot", "Y-axis max changed: autoscale=" + newAutoscaleYmax + ", value=" + newManualYmax);
		                                                   });
		
		showXaxisTitleWidget = new WidgetCheckbox("Show X-Axis Title",
		                                          true,
		                                          newShowXaxisTitle -> showXaxisTitle = newShowXaxisTitle);
		
		showXaxisScaleWidget = new WidgetCheckbox("Show X-Axis Scale",
		                                         true,
		                                         newShowXaxisScale -> showXaxisScale = newShowXaxisScale);
		
		showYaxisTitleWidget = new WidgetCheckbox("Show Y-Axis Title",
		                                         true,
		                                         newShowYaxisTitle -> showYaxisTitle = newShowYaxisTitle);
		
		showYaxisScaleWidget = new WidgetCheckbox("Show Y-Axis Scale",
		                                         true,
		                                         newShowYaxisScale -> showYaxisScale = newShowYaxisScale);
		
		showLegendWidget = new WidgetCheckbox("Show Legend",
		                                     true,
		                                     newShowLegend -> showLegend = newShowLegend);
		
		cachedWidget = new WidgetCheckbox("Cached Mode",
		                                 false,
		                                 newCachedMode -> {
		                                     cachedMode = newCachedMode;
		                                     autoscaleX = cachedMode ? new AutoScale(AutoScale.MODE_STICKY, 1, 0.10f) :
		                                                              new AutoScale(AutoScale.MODE_EXPONENTIAL, 30, 0.10f);
		                                     autoscaleY = cachedMode ? new AutoScale(AutoScale.MODE_STICKY, 1, 0.10f) :
		                                                              new AutoScale(AutoScale.MODE_EXPONENTIAL, 30, 0.10f);
		                                     FileLogger.log("XY Plot", "Cached mode changed: " + newCachedMode);
		                                 });
		
		WidgetCheckbox squareScaleWidget = new WidgetCheckbox("Square Scale (1:1)",
		                                                      false,
		                                                      newSquareScale -> {
		                                                          squareScale = newSquareScale;
		                                                          FileLogger.log("XY Plot", "Square scale changed: " + newSquareScale);
		                                                      });
		
		widgets = new Widget[16];
		
		widgets[0]  = xAxisDatasetWidget;
		widgets[1]  = null;
		widgets[2]  = yDatasetsWidget;
		widgets[3]  = null;
		widgets[4]  = xMinMaxWidget;
		widgets[5]  = null;
		widgets[6]  = yMinMaxWidget;
		widgets[7]  = null;
		widgets[8]  = squareScaleWidget;
		widgets[9]  = null;
		widgets[10] = showXaxisTitleWidget;
		widgets[11] = showXaxisScaleWidget;
		widgets[12] = null;
		widgets[13] = showYaxisTitleWidget;
		widgets[14] = showYaxisScaleWidget;
		widgets[15] = showLegendWidget;
		// TODO: Add cachedWidget later
		
		FileLogger.log("XY Plot", "XY Plot chart constructor completed");
		
	}
	
	/**
	 * Updates the List of all datasets, and creates new corresponding caches.
	 */
	private void updateAllDatasetsList() {
		
		allDatasets = new ArrayList<Dataset>(datasets.normalDatasets);
		
		if(xAxisDataset != null && !allDatasets.contains(xAxisDataset))
			allDatasets.add(xAxisDataset);
		
		if(allDatasets.isEmpty()) {
			xAxisCache = null;
		} else {
			if(xAxisDataset != null)
				xAxisCache = xAxisDataset.createCache();
		}
		
		FileLogger.log("XY Plot", "Updated datasets list. Total: " + allDatasets.size() + ", X-axis dataset: " + (xAxisDataset != null ? xAxisDataset.name : "null"));
		
	}
	
	@Override public EventHandler drawChart(GL2ES3 gl, float[] chartMatrix, int width, int height, long endTimestamp, int endSampleNumber, double zoomLevel, int mouseX, int mouseY) {
		
		FileLogger.log("XY Plot", "drawChart called - width=" + width + ", height=" + height + ", endSampleNumber=" + endSampleNumber);
		
		EventHandler handler = null;
		
		// Basic validation - check if we have X-axis dataset and Y-axis datasets
		if(xAxisDataset == null) {
			FileLogger.log("XY Plot", "WARNING: No X-axis dataset selected");
			return null;
		}
		
		if(!datasets.hasNormals()) {
			FileLogger.log("XY Plot", "WARNING: No Y-axis datasets selected");
			return null;
		}
		
		FileLogger.log("XY Plot", "Drawing XY plot with X-axis: " + xAxisDataset.name + ", Y-axis datasets: " + datasets.normalsCount());
		
		boolean haveDatasets = allDatasets != null && !allDatasets.isEmpty();
		int datasetsCount = haveDatasets ? allDatasets.size() : 0;
		
		// Initialize the plot
		plot.initialize(endTimestamp, endSampleNumber, zoomLevel, datasets, null, duration, cachedMode, false);
		
		// Calculate Y-axis range
		StorageFloats.MinMax requiredRangeY = plot.getRange();
		autoscaleY.update(requiredRangeY.min, requiredRangeY.max);
		plotMaxY = autoscaleYmax ? autoscaleY.getMax() : manualYmax;
		plotMinY = autoscaleYmin ? autoscaleY.getMin() : manualYmin;
		plotRangeY = plotMaxY - plotMinY;
		
		// Calculate X-axis range from the plot's X-axis domain
		// For XY plots, the X-axis range comes from the dataset values
		PlotDatasetXAxis xyPlot = (PlotDatasetXAxis) plot;
		float plotMinXFloat = xyPlot.plotMinXFloat;
		float plotMaxXFloat = xyPlot.plotMaxXFloat;
		float plotDomainXFloat = xyPlot.plotDomainFloat;
		
		// Apply autoscaling for X-axis if enabled
		if(autoscaleXmin || autoscaleXmax) {
			autoscaleX.update(plotMinXFloat, plotMaxXFloat);
			if(autoscaleXmin)
				plotMinXFloat = autoscaleX.getMin();
			if(autoscaleXmax)
				plotMaxXFloat = autoscaleX.getMax();
			plotDomainXFloat = plotMaxXFloat - plotMinXFloat;
		} else {
			// Use manual values
			plotMinXFloat = manualXmin;
			plotMaxXFloat = manualXmax;
			plotDomainXFloat = plotMaxXFloat - plotMinXFloat;
		}
		
		// Store initial ranges before square scale adjustment (used later if square scale is enabled)
		float initialPlotMinXFloat = plotMinXFloat;
		float initialPlotMaxXFloat = plotMaxXFloat;
		float initialPlotMinY = plotMinY;
		float initialPlotMaxY = plotMaxY;
		
		plotMinX = plotMinXFloat;
		plotMaxX = plotMaxXFloat;
		plotRangeX = plotDomainXFloat;
		
		// Calculate x and y positions of everything
		xPlotLeft = Theme.tilePadding;
		xPlotRight = width - Theme.tilePadding;
		plotWidth = xPlotRight - xPlotLeft;
		yPlotTop = height - Theme.tilePadding;
		yPlotBottom = Theme.tilePadding;
		plotHeight = yPlotTop - yPlotBottom;
		
		// X-axis title
		if(showXaxisTitle) {
			yXaxisTitleTextBaseline = Theme.tilePadding;
			yXaxisTitleTextTop = yXaxisTitleTextBaseline + OpenGL.largeTextHeight;
			xAxisTitle = plot.getTitle();
			xXaxisTitleTextLeft = xPlotLeft + (plotWidth / 2.0f) - (OpenGL.largeTextWidth(gl, xAxisTitle) / 2.0f);
			
			float temp = yXaxisTitleTextTop + Theme.tickTextPadding;
			if(yPlotBottom < temp) {
				yPlotBottom = temp;
				plotHeight = yPlotTop - yPlotBottom;
			}
		}
		
		// Legend
		if(showLegend && haveDatasets) {
			xLegendBorderLeft = Theme.tilePadding;
			yLegendBorderBottom = Theme.tilePadding;
			yLegendTextBaseline = yLegendBorderBottom + Theme.legendTextPadding;
			yLegendTextTop = yLegendTextBaseline + OpenGL.mediumTextHeight;
			yLegendBorderTop = yLegendTextTop + Theme.legendTextPadding;
			
			legendMouseoverCoordinates = new float[datasetsCount][4];
			legendBoxCoordinates = new float[datasetsCount][4];
			xLegendNameLeft = new float[datasetsCount];
			
			float xOffset = xLegendBorderLeft + (Theme.lineWidth / 2) + Theme.legendTextPadding;
			
			for(int i = 0; i < datasetsCount; i++) {
				legendMouseoverCoordinates[i][0] = xOffset - Theme.legendTextPadding;
				legendMouseoverCoordinates[i][1] = yLegendBorderBottom;
				
				legendBoxCoordinates[i][0] = xOffset;
				legendBoxCoordinates[i][1] = yLegendTextBaseline;
				legendBoxCoordinates[i][2] = xOffset + OpenGL.mediumTextHeight;
				legendBoxCoordinates[i][3] = yLegendTextTop;
				
				xOffset += OpenGL.mediumTextHeight + Theme.legendTextPadding;
				xLegendNameLeft[i] = xOffset;
				xOffset += OpenGL.mediumTextWidth(gl, allDatasets.get(i).name) + Theme.legendNamesPadding;
				
				legendMouseoverCoordinates[i][2] = xOffset - Theme.legendNamesPadding + Theme.legendTextPadding;
				legendMouseoverCoordinates[i][3] = yLegendBorderTop;
			}
			
			xLegendBorderRight = xOffset - Theme.legendNamesPadding + Theme.legendTextPadding + (Theme.lineWidth / 2);
			if(showXaxisTitle)
				xXaxisTitleTextLeft = xLegendBorderRight + ((xPlotRight - xLegendBorderRight) / 2) - (OpenGL.largeTextWidth(gl, xAxisTitle) / 2.0f);
			
			float temp = yLegendBorderTop + Theme.legendTextPadding;
			if(yPlotBottom < temp) {
				yPlotBottom = temp;
				plotHeight = yPlotTop - yPlotBottom;
			}
		}
		
		// X-axis scale
		if(showXaxisScale) {
			yXaxisTickTextBaseline = yPlotBottom;
			yXaxisTickTextTop = yXaxisTickTextBaseline + OpenGL.smallTextHeight;
			yXaxisTickBottom = yXaxisTickTextTop + Theme.tickTextPadding;
			yXaxisTickTop = yXaxisTickBottom + Theme.tickLength;
			
			yPlotBottom = yXaxisTickTop;
			plotHeight = yPlotTop - yPlotBottom;
		}
		
		// Y-axis title
		if(showYaxisTitle) {
			xYaxisTitleTextTop = xPlotLeft;
			xYaxisTitleTextBaseline = xYaxisTitleTextTop + OpenGL.largeTextHeight;
			yAxisTitle = haveDatasets ? allDatasets.get(0).unit : "";
			yYaxisTitleTextLeft = yPlotBottom + (plotHeight / 2.0f) - (OpenGL.largeTextWidth(gl, yAxisTitle) / 2.0f);
			
			xPlotLeft = xYaxisTitleTextBaseline + Theme.tickTextPadding;
			plotWidth = xPlotRight - xPlotLeft;
			
			if(showXaxisTitle && !showLegend)
				xXaxisTitleTextLeft = xPlotLeft + (plotWidth / 2.0f) - (OpenGL.largeTextWidth(gl, xAxisTitle) / 2.0f);
		}
		
		// Y-axis scale
		if(showYaxisScale) {
			yDivisions = ChartUtils.getYdivisions125(plotHeight, plotMinY, plotMaxY);
			float maxTextWidth = 0;
			for(String text : yDivisions.values()) {
				float textWidth = OpenGL.smallTextWidth(gl, text);
				if(textWidth > maxTextWidth)
					maxTextWidth = textWidth;
			}
			
			xYaxisTickTextRight = xPlotLeft + maxTextWidth;
			xYaxisTickLeft = xYaxisTickTextRight + Theme.tickTextPadding;
			xYaxisTickRight = xYaxisTickLeft + Theme.tickLength;
			
			xPlotLeft = xYaxisTickRight;
			plotWidth = xPlotRight - xPlotLeft;
			
			if(showXaxisTitle && !showLegend)
				xXaxisTitleTextLeft = xPlotLeft + (plotWidth / 2.0f) - (OpenGL.largeTextWidth(gl, xAxisTitle) / 2.0f);
		}
		
		// Stop if the plot is too small
		if(plotWidth < 1 || plotHeight < 1) {
			FileLogger.log("XY Plot", "WARNING: Plot too small - width=" + plotWidth + ", height=" + plotHeight);
			return handler;
		}
		
		// Force the plot to be an integer number of pixels
		xPlotLeft = (int) xPlotLeft;
		xPlotRight = (int) xPlotRight;
		yPlotBottom = (int) yPlotBottom;
		yPlotTop = (int) yPlotTop;
		plotWidth = xPlotRight - xPlotLeft;
		plotHeight = yPlotTop - yPlotBottom;
		
		// Apply square scale if enabled (maintain 1:1 aspect ratio)
		if(squareScale && plotDomainXFloat > 0 && plotRangeY > 0 && plotWidth > 0 && plotHeight > 0) {
			// Calculate pixels per unit for each axis
			float pixelsPerUnitX = plotWidth / plotDomainXFloat;
			float pixelsPerUnitY = plotHeight / plotRangeY;
			
			FileLogger.log("XY Plot", "Square scale calculation - pixelsPerUnitX=" + pixelsPerUnitX + ", pixelsPerUnitY=" + pixelsPerUnitY + ", plotWidth=" + plotWidth + ", plotHeight=" + plotHeight);
			FileLogger.log("XY Plot", "Before square scale - X range=[" + plotMinXFloat + " to " + plotMaxXFloat + "], Y range=[" + plotMinY + " to " + plotMaxY + "]");
			
			// Use the smaller scale (more zoomed out) to ensure all data fits
			float pixelsPerUnit = Math.min(pixelsPerUnitX, pixelsPerUnitY);
			
			// Adjust ranges to maintain 1:1 aspect ratio (same pixels per unit on both axes)
			if(pixelsPerUnitX < pixelsPerUnitY) {
				// X-axis is the limiting factor, expand Y-axis range to match X-axis scale
				float newPlotRangeY = plotHeight / pixelsPerUnit;
				float centerY = (initialPlotMinY + initialPlotMaxY) / 2.0f;
				plotMinY = centerY - newPlotRangeY / 2.0f;
				plotMaxY = centerY + newPlotRangeY / 2.0f;
				plotRangeY = newPlotRangeY;
			} else {
				// Y-axis is the limiting factor, expand X-axis range to match Y-axis scale
				float newPlotDomainXFloat = plotWidth / pixelsPerUnit;
				float centerX = (initialPlotMinXFloat + initialPlotMaxXFloat) / 2.0f;
				plotMinXFloat = centerX - newPlotDomainXFloat / 2.0f;
				plotMaxXFloat = centerX + newPlotDomainXFloat / 2.0f;
				plotDomainXFloat = newPlotDomainXFloat;
				plotMinX = plotMinXFloat;
				plotMaxX = plotMaxXFloat;
				plotRangeX = plotDomainXFloat;
			}
			
			// Verify the result
			float finalPixelsPerUnitX = plotWidth / plotDomainXFloat;
			float finalPixelsPerUnitY = plotHeight / plotRangeY;
			FileLogger.log("XY Plot", "Square scale applied - pixelsPerUnit=" + pixelsPerUnit + ", final pixelsPerUnitX=" + finalPixelsPerUnitX + ", final pixelsPerUnitY=" + finalPixelsPerUnitY);
			FileLogger.log("XY Plot", "After square scale - X range=[" + plotMinXFloat + " to " + plotMaxXFloat + "], Y range=[" + plotMinY + " to " + plotMaxY + "]");
		}
		
		// Update plot's X-axis range BEFORE drawing scales (needed for proper X-axis divisions)
		xyPlot.plotMinXFloat = plotMinXFloat;
		xyPlot.plotMaxXFloat = plotMaxXFloat;
		xyPlot.plotDomainFloat = plotDomainXFloat;
		
		// Draw plot background
		OpenGL.drawQuad2D(gl, Theme.plotBackgroundColor, xPlotLeft, yPlotBottom, xPlotRight, yPlotTop);
		
		// Draw the X-axis scale
		if(showXaxisScale) {
			Map<Float, String> divisions = plot.getXdivisions(gl, (int) plotWidth);
			FileLogger.log("XY Plot", "X-axis divisions: " + divisions.size() + " divisions");
			if(divisions.isEmpty()) {
				FileLogger.log("XY Plot", "WARNING: X-axis divisions map is empty! plotMinXFloat=" + plotMinXFloat + ", plotMaxXFloat=" + plotMaxXFloat + ", plotDomainFloat=" + plotDomainXFloat);
			}
			
			OpenGL.buffer.rewind();
			for(Float divisionLocation : divisions.keySet()) {
				float x = divisionLocation + xPlotLeft;
				OpenGL.buffer.put(x); OpenGL.buffer.put(yPlotTop);    OpenGL.buffer.put(Theme.divisionLinesColor);
				OpenGL.buffer.put(x); OpenGL.buffer.put(yPlotBottom); OpenGL.buffer.put(Theme.divisionLinesColor);
				
				OpenGL.buffer.put(x); OpenGL.buffer.put(yXaxisTickTop);    OpenGL.buffer.put(Theme.tickLinesColor);
				OpenGL.buffer.put(x); OpenGL.buffer.put(yXaxisTickBottom); OpenGL.buffer.put(Theme.tickLinesColor);
			}
			OpenGL.buffer.rewind();
			int vertexCount = divisions.keySet().size() * 4;
			OpenGL.drawLinesXyrgba(gl, GL3.GL_LINES, OpenGL.buffer, vertexCount);
			
			for(Map.Entry<Float,String> entry : divisions.entrySet()) {
				float x = entry.getKey() + xPlotLeft - (OpenGL.smallTextWidth(gl, entry.getValue()) / 2.0f);
				float y = yXaxisTickTextBaseline;
				OpenGL.drawSmallText(gl, entry.getValue(), (int) x, (int) y, 0);
			}
		}
		
		// Draw the Y-axis scale
		if(showYaxisScale) {
			OpenGL.buffer.rewind();
			for(Float entry : yDivisions.keySet()) {
				float y = (entry - plotMinY) / plotRangeY * plotHeight + yPlotBottom;
				OpenGL.buffer.put(xPlotLeft);  OpenGL.buffer.put(y); OpenGL.buffer.put(Theme.divisionLinesColor);
				OpenGL.buffer.put(xPlotRight); OpenGL.buffer.put(y); OpenGL.buffer.put(Theme.divisionLinesColor);
				
				OpenGL.buffer.put(xYaxisTickLeft);  OpenGL.buffer.put(y); OpenGL.buffer.put(Theme.tickLinesColor);
				OpenGL.buffer.put(xYaxisTickRight); OpenGL.buffer.put(y); OpenGL.buffer.put(Theme.tickLinesColor);
			}
			OpenGL.buffer.rewind();
			int vertexCount = yDivisions.keySet().size() * 4;
			OpenGL.drawLinesXyrgba(gl, GL3.GL_LINES, OpenGL.buffer, vertexCount);
			
			for(Map.Entry<Float,String> entry : yDivisions.entrySet()) {
				float x = xYaxisTickTextRight - OpenGL.smallTextWidth(gl, entry.getValue());
				float y = (entry.getKey() - plotMinY) / plotRangeY * plotHeight + yPlotBottom - (OpenGL.smallTextHeight / 2.0f);
				OpenGL.drawSmallText(gl, entry.getValue(), (int) x, (int) y, 0);
			}
		}
		
		// Draw the legend
		if(showLegend && haveDatasets && xLegendBorderRight < width - Theme.tilePadding) {
			OpenGL.drawQuad2D(gl, Theme.legendBackgroundColor, xLegendBorderLeft, yLegendBorderBottom, xLegendBorderRight, yLegendBorderTop);
			
			for(int i = 0; i < datasetsCount; i++) {
				if(mouseX >= legendMouseoverCoordinates[i][0] && mouseX <= legendMouseoverCoordinates[i][2] && mouseY >= legendMouseoverCoordinates[i][1] && mouseY <= legendMouseoverCoordinates[i][3]) {
					OpenGL.drawQuadOutline2D(gl, Theme.tickLinesColor, legendMouseoverCoordinates[i][0], legendMouseoverCoordinates[i][1], legendMouseoverCoordinates[i][2], legendMouseoverCoordinates[i][3]);
					Dataset d = allDatasets.get(i);
					handler = EventHandler.onPress(event -> ConfigureView.instance.forDataset(d));
				}
				OpenGL.drawQuad2D(gl, allDatasets.get(i).glColor, legendBoxCoordinates[i][0], legendBoxCoordinates[i][1], legendBoxCoordinates[i][2], legendBoxCoordinates[i][3]);
				OpenGL.drawMediumText(gl, allDatasets.get(i).name, (int) xLegendNameLeft[i], (int) yLegendTextBaseline, 0);
			}
		}
		
		// Draw the X-axis title
		if(showXaxisTitle)
			if((!showLegend && xXaxisTitleTextLeft > xPlotLeft) || (showLegend && xXaxisTitleTextLeft > xLegendBorderRight + Theme.legendTextPadding))
				OpenGL.drawLargeText(gl, xAxisTitle, (int) xXaxisTitleTextLeft, (int) yXaxisTitleTextBaseline, 0);
		
		// Draw the Y-axis title
		if(showYaxisTitle && yYaxisTitleTextLeft > yPlotBottom)
			OpenGL.drawLargeText(gl, yAxisTitle, (int) xYaxisTitleTextBaseline, (int) yYaxisTitleTextLeft, 90);
		
		// Acquire the samples
		plot.acquireSamples(plotMinY, plotMaxY, (int) plotWidth, (int) plotHeight);
		
		// Draw the plot
		plot.draw(gl, chartMatrix, (int) xPlotLeft, (int) yPlotBottom, (int) plotWidth, (int) plotHeight, plotMinY, plotMaxY);
		
		return handler;
		
	}
	
	@Override public void disposeNonGpu() {
		
		FileLogger.log("XY Plot", "Disposing XY Plot chart (non-GPU resources)");
		
	}
	
	@Override public void disposeGpu(GL2ES3 gl) {
		
		FileLogger.log("XY Plot", "Disposing XY Plot chart (GPU resources)");
		
		if(plot != null)
			plot.freeResources(gl);
		
	}
	
}

