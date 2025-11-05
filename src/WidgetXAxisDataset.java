import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import java.awt.Component;

/**
 * Widget for selecting a single dataset to use as the X-axis.
 */
public class WidgetXAxisDataset extends Widget {
	
	// "model"
	Dataset selectedDataset = null;
	
	// "view"
	JComboBox<Dataset> datasetCombobox;
	
	// "controller"
	Consumer<Dataset> datasetEventHandler;
	
	/**
	 * A widget that lets the user select one dataset for the X-axis.
	 * 
	 * @param datasetHandler    Will be notified when the dataset selection changes.
	 */
	public WidgetXAxisDataset(Consumer<Dataset> datasetHandler) {
		
		super();
		
		datasetEventHandler = datasetHandler;
		
		// Create combobox with all available datasets from all connections
		List<Dataset> allDatasets = new ArrayList<Dataset>();
		for(ConnectionTelemetry connection : ConnectionsController.telemetryConnections) {
			for(int i = 0; i < connection.datasets.getCount(); i++) {
				Dataset d = connection.datasets.getByIndex(i);
				if(!d.isBitfield) // Only allow non-bitfield datasets for X-axis
					allDatasets.add(d);
			}
		}
		
		datasetCombobox = new JComboBox<Dataset>(allDatasets.toArray(new Dataset[0]));
		datasetCombobox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value instanceof Dataset) {
					Dataset d = (Dataset) value;
					int connectionIndex = ConnectionsController.allConnections.indexOf(d.connection);
					label.setText("connection " + connectionIndex + " location " + d.location + " (" + d.name + ")");
				}
				return label;
			}
		});
		
		datasetCombobox.addActionListener(event -> {
			Dataset selected = (Dataset) datasetCombobox.getSelectedItem();
			selectedDataset = selected;
			FileLogger.log("XY Plot", "X-axis dataset selected: " + (selected != null ? selected.name : "null"));
			datasetEventHandler.accept(selected);
		});
		
		// Add placeholder option for "None"
		datasetCombobox.insertItemAt(null, 0);
		datasetCombobox.setSelectedIndex(0); // Start with "None" selected
		
		widgets.put(new JLabel("X-Axis Dataset: "), "");
		widgets.put(datasetCombobox, "span 3, growx");
		
		// Update available datasets when connections change
		// TODO: This might need to be called when connections are added/removed
		
	}
	
	/**
	 * Updates the list of available datasets (call this when connections change).
	 */
	public void updateAvailableDatasets() {
		
		FileLogger.log("XY Plot", "Updating available X-axis datasets");
		
		Dataset currentSelection = selectedDataset;
		
		List<Dataset> allDatasets = new ArrayList<Dataset>();
		for(ConnectionTelemetry connection : ConnectionsController.telemetryConnections) {
			for(int i = 0; i < connection.datasets.getCount(); i++) {
				Dataset d = connection.datasets.getByIndex(i);
				if(!d.isBitfield)
					allDatasets.add(d);
			}
		}
		
		datasetCombobox.removeAllItems();
		datasetCombobox.addItem(null); // "None" option
		for(Dataset d : allDatasets)
			datasetCombobox.addItem(d);
		
		// Try to restore previous selection
		if(currentSelection != null && allDatasets.contains(currentSelection)) {
			datasetCombobox.setSelectedItem(currentSelection);
		} else {
			datasetCombobox.setSelectedIndex(0); // Select "None"
		}
		
		FileLogger.log("XY Plot", "Available X-axis datasets updated. Count: " + allDatasets.size());
		
	}
	
	/**
	 * Updates the widget and chart based on settings from a layout file.
	 * 
	 * @param lines    A queue of remaining lines from the layout file.
	 */
	@Override public void importState(ConnectionsController.QueueOfLines lines) {
		
		// Parse format: "x-axis dataset = connection 0 location 1"
		String text = ChartUtils.parseString(lines.remove(), "x-axis dataset = %s");
		
		if(text.equals("none") || text.isEmpty()) {
			datasetCombobox.setSelectedIndex(0);
			return;
		}
		
		// Parse "connection X location Y"
		String[] parts = text.split(" ");
		if(parts.length != 4 || !parts[0].equals("connection") || !parts[2].equals("location")) {
			FileLogger.logException("XY Plot", new Exception("Invalid X-axis dataset format: " + text));
			return;
		}
		
		try {
			int connectionIndex = Integer.parseInt(parts[1]);
			int location = Integer.parseInt(parts[3]);
			
			if(connectionIndex >= 0 && connectionIndex < ConnectionsController.telemetryConnections.size()) {
				ConnectionTelemetry connection = ConnectionsController.telemetryConnections.get(connectionIndex);
				Dataset dataset = connection.datasets.getByLocation(location);
				if(dataset != null && !dataset.isBitfield) {
					datasetCombobox.setSelectedItem(dataset);
					FileLogger.log("XY Plot", "Imported X-axis dataset: connection " + connectionIndex + " location " + location);
					return;
				}
			}
		} catch(Exception e) {
			FileLogger.logException("XY Plot", e);
		}
		
		// If we get here, couldn't find the dataset
		datasetCombobox.setSelectedIndex(0);
		
	}
	
	/**
	 * Saves the current state to one or more lines of text.
	 * 
	 * @return    A String[] where each element is a line of text.
	 */
	@Override public String[] exportState() {
		
		if(selectedDataset == null) {
			return new String[] { "x-axis dataset = none" };
		}
		
		int connectionIndex = ConnectionsController.allConnections.indexOf(selectedDataset.connection);
		return new String[] {
			"x-axis dataset = connection " + connectionIndex + " location " + selectedDataset.location
		};
		
	}
	
}

