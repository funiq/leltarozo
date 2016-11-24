// License: GPL. For details, see LICENSE file

package hu.cartographia.inventory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @since 2016-11-14
 * @author Báthory Péter <peter.bathory@cartographia.hu>
 */
public class Logger {

	public final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();
	
	private BufferedWriter logWriter = null;
	private String operatorName;
	private String location;
	
	public Logger(String path, String operatorName, String location) throws Exception {
		this.operatorName = operatorName;
		this.location = location;
		
		final String startTimeString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		final File logDir = new File(path + "log");
		final String fileNameAndPath =
				logDir.getPath() + File.separatorChar
				+ startTimeString + "_" + operatorName + "_" + location + ".csv";

		try {
			if (!logDir.exists()) {
				logDir.mkdir();
			}
			logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileNameAndPath, true), "UTF-8"));
		} catch (IOException e) {
			throw new Exception("A naplófájlt nem sikerült létrehozni:\n" + fileNameAndPath + "\n" + e.getMessage());
		}
	}
	
	private void writeToLog() throws Exception {
		if (logWriter == null || logEntries.size() == 0) {
			return;
		}
		try {
			final LogEntry newLogEntry = logEntries.get(0);
			
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(newLogEntry.getTimestamp());
			logWriter.write(String.format(
				"%1$s\t\"%2$s\"\t%3$s\t\"%4$s\"\t%5$s\t\"%6$s\"\t\"%7$s\"\t\"%8$s\"\t\"%9$s\"\t\"%10$s\"\t\"%11$s\"\r\n",
				timeStamp,
				newLogEntry.getBarcode(),
				newLogEntry.getCount(),
				newLogEntry.getComment(),
				newLogEntry.getPublicationDate() != null ? newLogEntry.getPublicationDate() : "",
				location,
				operatorName,
				newLogEntry.getProductId(),
				newLogEntry.getName().replace("\"", "\"\""),
				newLogEntry.getPublisher(),
				newLogEntry.getNormalizedBarcode()
			));
			logWriter.flush();
			
			newLogEntry.setIsCommited(true);
		} catch (IOException e) {
			throw new Exception("Naplófálj írása sikertelen: " + e.getMessage());
		}
	}

	public LogEntry parseInput(String input, DatabaseEntry dbEntry) throws Exception {
		if (input.isEmpty()) {
			return null;
		}
		LogEntry lastLogEntry = logEntries.size() > 0 ? logEntries.get(0) : null;

		if (input.matches("[-ö_\\d]+")) {	// Barcode
			input = input.replace("ö", "0").replace("-", "");
			if (input.length() >= 8) {
				// New barcode has been entered, flush the previous one
				writeToLog();

				// Set the new barcode
				if (dbEntry != null) {
					lastLogEntry = new LogEntry(dbEntry.getBarcode(), dbEntry);
				} else {
					lastLogEntry = new LogEntry(input, null);
				}
				logEntries.add(0, lastLogEntry);
			} else if (lastLogEntry != null) {
				try {
					Integer num = Integer.parseInt(input);
					if (num < 1900) {	// Count
						lastLogEntry.setCount(num);
					} else if (num < 2100) {	// Publication year
						lastLogEntry.setPublicationDate(num.toString());
					}
				} catch (NumberFormatException e) {
					return null;
				}
			}
		} else if (lastLogEntry != null) {	// Comment
			lastLogEntry.setComment(input);
		}
		return lastLogEntry;
	}
	
	public void close() {
		if (logWriter != null) {
			try {
				writeToLog();
				logWriter.close();
			} catch (Exception e) {
				System.err.println("Naplófájl bezárása sikertelen!");
			}
		}
	}
	
	@Override
	public void finalize() {
		close();
	}
}
