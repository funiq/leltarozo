package hu.cartographia.inventory;

import java.io.*;
import java.util.*;

class BarcodeValidator {
	private static String inputFileName, outputFileName;
	private static final Database db = new Database();
	private static BufferedWriter fileWriter = null;

	public static void main(String[] args) {
		if (args.length >= 2) {
			inputFileName = args[0];
			outputFileName = args[1];
		} else {
			System.err.println("Bemeneti és kimeneti fájlnév paraméter megadása kötelező");
			System.exit(1);
		}
		
		try {
			db.readFromFile(inputFileName);
			fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		/*
		try {
			fileWriter.write("Vonalkód\tNormalizált\tGyanús?\tOK?\tNév\tKiadó\tDB\tId\r\n");
			for (Map.Entry<String,DatabaseEntry> entry : db) {
				DatabaseEntry item = entry.getValue();
			
				fileWriter.write(String.format("\"%1$s\"\t\"%2$s\"\t\"%3$s\"\t\"%4$s\"\t\"%5$s\"\t\"%6$s\"\t%7$s\t\"%8$s\"\r\n",
					item.getBarcode(),
					item.getBarcode().replaceAll("[^\\d]", ""),
					!item.getBarcode().matches("[\\d]+") ? "GYANÚS" : "",
					InventoryApp.isValidGTIN(item.getBarcode()) ? "OK" : "ÉRVÉNYTELEN",
					item.getName(),
					item.getPublisher(),
					item.getStockCount(),
					item.getId()
				));
				fileWriter.flush();
			}
		} catch (IOException e) {}*/
	}
}
