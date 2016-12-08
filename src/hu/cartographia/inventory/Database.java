// License: GPL. For details, see LICENSE file

package hu.cartographia.inventory;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * A DatabaseEntry stores known properites of a product
 * @since 2016-11-14
 * @author Báthory Péter <peter.bathory@cartographia.hu>
 */
class DatabaseEntry {
    private String name;
    private String publisher;
    private Integer stockCount;
    private String id;
    private String originalBarcode;
    private String normalizedBarcode;
    
    public DatabaseEntry(String name, String publisher, Integer stockCount, String id, String barcode) {
        this.name = name;
        this.publisher = publisher;
        this.stockCount = stockCount;
        this.id = id;
        this.originalBarcode = barcode;
        if (barcode.matches(".*-[0-9]{2}$") || barcode.matches(".*-[0-9]{5}$")) {
        	this.normalizedBarcode = barcode.replaceAll("-[0-9]+", "").replaceAll("[^0-9]", "");
        } else {
	        this.normalizedBarcode = barcode.replaceAll("[^0-9]", "");
        }
    };
    
    public String getName() {
    	return this.name;
    }
    
    public String getPublisher() {
    	return this.publisher;
    }
    
    public Integer getStockCount() {
    	return this.stockCount;
    }
    
    public String getId() {
    	return this.id;
    }
    
    public String getBarcode() {
    	return this.originalBarcode;
    }
    
    public String getNormalizedBarcode() {
    	return this.normalizedBarcode;
    }
}

/**
 * Product database. Stores products in a hash map where the primary key is the barcode (as string)
 * Can load products from CSV file
 * @since 2016-11-14
 */
class Database implements Iterable<Map.Entry<String, List<DatabaseEntry>>> {
	private final Map<String, List<DatabaseEntry>> entries = new HashMap<String, List<DatabaseEntry>>();

	@Override
	public Iterator<Map.Entry<String, List<DatabaseEntry>>> iterator() {
		return entries.entrySet().iterator();
	}

	/**
	 * Searches and returns a database entry with a given barcode
	 * @param barcode The barcode to find
	 * @return A database entry or null
	 */
	public List<DatabaseEntry> getByBarcode(String barcode) {
		return entries.get(barcode);
	}
	
	/**
	 * Generates a list of database entry values
	 * @return The list of database entries
	 */
	public List<DatabaseEntry> getList() {
		List<DatabaseEntry> items = new ArrayList<DatabaseEntry>();
		
		for (List<DatabaseEntry> entry : entries.values()) {
			items.addAll(entry);
		}
		return items;
	}

	/**
	 * Adds a DatabaseEntry to the database
	 * @param item The database entry to add
	 */
	public void addItem(DatabaseEntry item) {
		if (getByBarcode(item.getNormalizedBarcode()) == null) {
			List<DatabaseEntry> newEntry = new ArrayList<DatabaseEntry>();
			newEntry.add(item);
			entries.put(item.getNormalizedBarcode(), newEntry);
		} else {
			getByBarcode(item.getNormalizedBarcode()).add(item);
		}
	}

	/**
	 * Creates a database entry and adds to the database
	 * @param barcode    The barcode of the product. Shuld be unique in the database
	 * @param name       The name of the product
	 * @param publisher  The publisher of the product
	 * @param stockCount Number of pieces in the stock
	 * @param id         A custom product id
	 */
	public void addItem(String barcode, String name, String publisher, Integer stockCount, String id) {
		addItem(new DatabaseEntry(name, publisher, stockCount, id, barcode));
	}
	
	/** @see Database#addItem(String, String, String, Integer, String) */
	public void addItem(String barcode, String name, String publisher, String stockCount, String id) {
		Integer count;
		try {
			count = Integer.parseInt(stockCount);
		} catch (NumberFormatException e) {
			count = null;
		}
		addItem(new DatabaseEntry(name, publisher, count, id, barcode));
	}

	/**
	 * Parses the given CSV file, creates DatabaseEntry-es and adds them to entries
	 * Can read every CSV format: comma, semicolon or tab separated; quoted or unqouted values
	 *
	 * The file should be UTF-8 encoded. Each line shuld contain the following field in this order:
	 *    barcode, name, publisher, stock count, product id
	 *
	 * @param csvFile Name of the CSV file to read
	 * @throws Exception If fails reading
	 */
	public void readFromFile(String csvFile) throws Exception {
		Scanner scanner = null;
		try {
			scanner = new Scanner(new File(csvFile), "UTF-8");
			List<String> list;
			char separator = '\t';
			boolean separatorFound = false;
			
			while (scanner.hasNext()) {
				final String line = scanner.nextLine();
				if (!separatorFound) {
					list = CSVUtils.parseLine(line, separator);
					if (list.size() > 1) {
						separatorFound = true;
					} else {
						separator = ';';
						list = CSVUtils.parseLine(line, separator);
						if (list.size() > 1) {
							separatorFound = true;
						} else {
							separator = CSVUtils.DEFAULT_SEPARATOR;
							list = CSVUtils.parseLine(line, separator);
							if (list.size() > 1) {
								separatorFound = true;
							}
						}
					}
				} else {
					list = CSVUtils.parseLine(line, separator);
				}
				
				if (list.size() > 0) {
					final String barcode = list.get(0);
					if (getByBarcode(barcode) != null) {
						System.err.println("Duplikált vonalkód: " + barcode);
					}
				
					if (list.size() >= 5) {
						addItem(barcode, list.get(1), list.get(2), list.get(3), list.get(4));
					} else if (list.size() == 4)  {
						addItem(barcode, list.get(1), list.get(2), list.get(3), "");
					} else if (list.size() == 3) {
						addItem(barcode, list.get(1), list.get(2), "", "");
					}
				}
			}
				
			if (entries.size() == 0) {
				throw new Exception("\"" + csvFile + "\" adatbázisfájl nem taratalmazott érvényes bejegyzést.");
			}
			
			scanner.close();
		} catch (IOException e) {
			throw new Exception("\"" + csvFile + "\" adatbázisfájl nem található az alábbi útvonalon:\n"
					 + new File(csvFile).getAbsoluteFile() + "\n\n" + e.getMessage());
		} catch (Exception e) {
		e.printStackTrace();
			throw new Exception("Hiba történt \"" + csvFile + "\" adatbázisfájl olvasása közben\n\n" + e.getMessage());
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}
}
