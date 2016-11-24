// License: GPL. For details, see LICENSE file

package hu.cartographia.inventory;
 
import java.io.*;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.text.*;
import javafx.util.Callback;
import javafx.css.PseudoClass;
import java.nio.file.Paths;

/**
 * InventoryApp is a graphical stock-taking application
 *
 * Receives barcodes, displays products in a table, allows to edit product data (count, publication year, comment)
 * and writes every record into a log file
 *
 * @since 2016-11-14
 * @author Báthory Péter <peter.bathory@cartographia.hu>
 */
public class InventoryApp extends Application {
	/** List of choices shown in "select location" field of login screen. */
	private static final ObservableList<String> locationChoices = FXCollections.observableArrayList();
	private static String locationsFileName = "locations.txt";
	private static String databaseFileName = "database.csv";
	
	private final AudioClip alertSound1 = new AudioClip(
			getClass().getResource("resources/Beep_Ping-SoundBible.com-217088958.wav").toString()
		);
	private final AudioClip alertSound2 = new AudioClip(
			getClass().getResource("resources/Computer_Error_Alert-SoundBible.com-783113881.wav").toString()
		);
		
	/** List of available products. */
	private static final Database db = new Database();
	/** Log registry and writer. */
	private static Logger logger;
	/** Full path of the application. */
	private static String basePath = "";
	/** Name of the operator working on stock-taking. Will be written to the log and added to logfile name. */
	private static String operatorName;
	/** Name of the warehouse/shop. Will be written to the log and added to logfile name.  */
	private static String location;
	
	/** The table which displays the log entries */
	private final TableView<LogEntry> table = new TableView<LogEntry>();
	private Stage primaryStage;


	/**
	 * Application entry point
	 *
	 * Sets the application base path and launches the main GUI
	 *
	 * @param args Array of command-line arguments passed to this method
	 */
	public static void main(String[] args) {
	
		File thisClassFile = null;
		try {
			thisClassFile = new File(InventoryApp.class.getProtectionDomain().getCodeSource().getLocation().getPath());
			if (thisClassFile.getPath().toLowerCase().endsWith(".jar")) {
				basePath = URLDecoder.decode(thisClassFile.getParentFile().getAbsolutePath() + File.separatorChar, "UTF-8");
			} else {
				basePath = URLDecoder.decode(thisClassFile.getAbsolutePath() + File.separatorChar, "UTF-8");
			}
		} catch (Exception e) {
			basePath = "";
		}
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(locationsFileName), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				locationChoices.add(line.trim());
			}
			in.close();
		} catch (IOException e) {}
		if (locationChoices.size() == 0) {
			locationChoices.add("raktár1");
		}
		
		if (args.length >= 1) {
			if (args[0].equals("-d") && args.length >= 2) {
				databaseFileName = args[1];
			}
		} else {
			launch(args);
		}
	}

	/**
	 * Entry point of the GUI
	 * Sets the window title and size, application icon, loads the database and displays the login screen
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;
		stage.setTitle("Cartographia Leltározó");
		
		primaryStage.getIcons().addAll(
			new Image("hu/cartographia/inventory/resources/images/app_icon128x128.png"),
			new Image("hu/cartographia/inventory/resources/images/app_icon64x64.png"),
			new Image("hu/cartographia/inventory/resources/images/app_icon32x32.png"),
			new Image("hu/cartographia/inventory/resources/images/app_icon16x16.png")
		);
		primaryStage.setWidth(1024);
		primaryStage.setHeight(600);
		
		stage.show();

		try {
			db.readFromFile(basePath + databaseFileName);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Nem sikerült beolvasni az adatbázist.");
			alert.setHeaderText(e.getMessage() + "\n\nLeltározni adatbázis nélkül is lehet, de a program nem fogja felismerni a termékeket.");
			alert.showAndWait();
		}
		
		login();
	}
	
	@Override
	public void stop() {
		// Save the last log entry and close logfile
		if (logger != null) {
			logger.close();
		}
	}
	
	/**
	 * Builds the main GUI: an input field and the TableView
	 */
	private void initializeMainGUI() {
		try {
			logger = new Logger(basePath, operatorName, location);
		} catch (Exception e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("A naplófájlt nem sikerült létrehozni.");
			alert.setHeaderText(e.getMessage());
			alert.showAndWait();
			System.exit(1);
		}
		
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(15, 5, 15, 5));
		
		Scene scene = new Scene(pane);
		scene.getStylesheets().add("hu/cartographia/inventory/resources/style.css");
		
		Label tooltipLabel = new Label();
		tooltipLabel.setMaxHeight(Double.MAX_VALUE);
		tooltipLabel.setAlignment(Pos.CENTER_RIGHT);
		tooltipLabel.setId("tooltipLabel");

		TextField inputField = new TextField();
		inputField.setId("inputField");
		inputField.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(inputField, Priority.ALWAYS);
		inputField.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ENTER)) {
					String input = inputField.getText();
					String normInput = input.replace("ö", "0").replace("-", "");
					LogEntry loggedItem = null;
					try {
						if (normInput.length() >= 8 && normInput.length() <= 14) {
							List<DatabaseEntry> dbEntries = db.getByBarcode(normInput);
							if (dbEntries == null) {
								loggedItem = logger.parseInput(input, null);
								alertSound2.play();
							} else if (dbEntries.size() == 1) {
								loggedItem = logger.parseInput(input, dbEntries.get(0));
							} else {
								loggedItem = logger.parseInput(input, chooseDbEntry(dbEntries));
							}
							
							if (loggedItem != null && loggedItem.getStockCount() != null && loggedItem.getStockCount() == 0) {
								alertSound1.play();
								Alert alert = new Alert(AlertType.WARNING);
								alert.setTitle("Készlet szerint a termékből nulla darab van.");
								alert.setHeaderText("A nyilvántartás szerint ebből a termékből nincs készletünk.\n\n"
								 + "Ellenőrizd le alaposan az összes adatot, eltérés esetén írj megjegyzést.");
								alert.showAndWait();
							}
						} else {
							loggedItem = logger.parseInput(input, null);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Naplófálj írása sikertelen!");
						alert.setHeaderText(e.getMessage());
						alert.showAndWait();
						System.exit(1);
					}
					inputField.clear();
				}
			}
		});
		inputField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(final ObservableValue<? extends String> observable, final String oldValue, String newValue) {
				if (newValue.matches("[-_ö\\d]+")) {
					newValue = newValue.replace("ö", "0").replace("-", "");
					if (newValue.length() >= 8) {
						List<DatabaseEntry> dbEntries = db.getByBarcode(newValue);
						if (dbEntries != null) {
							if (dbEntries.size() == 1) {
								tooltipLabel.setText(dbEntries.get(0).getName());
							} else {
								tooltipLabel.setText("Több termék azonos vonalkóddal: " + dbEntries.size());
							}
						} else {
							tooltipLabel.setText("Ismeretlen termék");
						}
						if (!isValidGTIN(newValue)) {
							tooltipLabel.setText(tooltipLabel.getText() + " (ÉRVÉNYTELEN ISBN!)");
						}
					} else if (newValue.length() < 4) {
						tooltipLabel.setText("Darabszám módosítás");
					} else if (newValue.length() == 4 && newValue.matches("19[0-9]{2}|20[0-9]{2}")) {
						tooltipLabel.setText("Kiadási évszám megadása");
					} else {
						tooltipLabel.setText("?");
					}
				} else if (!newValue.isEmpty()) {
					tooltipLabel.setText("Megjegyzés hozzáadása");
				} else {
					tooltipLabel.setText("");
				}
			}
		});
		inputField.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
				if (!newPropertyValue) {
					inputField.requestFocus();
				}
			}
		});
		
		final HBox hb = new HBox();
		hb.setAlignment(Pos.CENTER_LEFT);
		hb.getChildren().addAll(inputField, tooltipLabel);
		
		pane.setTop(hb);
		
		TableColumn<LogEntry, String> timestampCol = new TableColumn<LogEntry, String>("Mikor");
		timestampCol.getStyleClass().add("timestampTableCell");
		timestampCol.setPrefWidth(78);
		timestampCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("time"));

		TableColumn<LogEntry, String> barcodeCol = new TableColumn<LogEntry, String>("Vonalkód");
		barcodeCol.getStyleClass().add("barcodeTableCell");
		barcodeCol.setPrefWidth(150);
		barcodeCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("barcode"));

		TableColumn<LogEntry, Integer> countCol = new TableColumn<LogEntry, Integer>("db");
		countCol.getStyleClass().add("countTableCell");
		countCol.setPrefWidth(50);
		countCol.setCellValueFactory(new PropertyValueFactory<LogEntry, Integer>("count"));

		TableColumn<LogEntry, String> commentCol = new TableColumn<LogEntry, String>("Megjegyzés");
		commentCol.getStyleClass().add("commentTableCell");
		commentCol.setPrefWidth(170);
		commentCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("comment"));

		TableColumn<LogEntry, String> productNameCol = new TableColumn<LogEntry, String>("Név");
		productNameCol.getStyleClass().add("nameTableCell");
		productNameCol.setPrefWidth(385);
		productNameCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("name"));
		
		TableColumn<LogEntry, String> publisherCol = new TableColumn<LogEntry, String>("Kiadó");
		publisherCol.getStyleClass().add("publisherTableCell");
		publisherCol.setPrefWidth(120);
		publisherCol.setCellValueFactory(new PropertyValueFactory<LogEntry, String>("publisher"));
		
		TableColumn<LogEntry, Integer> publicationDateCol = new TableColumn<LogEntry, Integer>("K. év");
		publicationDateCol.getStyleClass().add("publicationDateTableCell");
		publicationDateCol.setPrefWidth(55);
		publicationDateCol.setCellValueFactory(new PropertyValueFactory<LogEntry, Integer>("publicationDate"));
		
		table.getColumns().addAll(Arrays.asList(
				timestampCol, barcodeCol, countCol, commentCol, publicationDateCol, productNameCol, publisherCol
		));
		table.setItems(logger.logEntries);
		table.setPlaceholder(new Label(
			"Elkezdheted a leltározást.\n\n" +
			"A fenti mezőbe írhatod a vonalkódot, adhatod meg a darabszámot,\névszámot, valamint a megjegyzést (ha van).\n" +
			"A naplóba írt bejegyzések itt fognak megjelenni."));
		
		PseudoClass activeRowSelector = PseudoClass.getPseudoClass("active");
		PseudoClass emptySelector = PseudoClass.getPseudoClass("empty");
		PseudoClass missingSelector = PseudoClass.getPseudoClass("missing");
		
		table.setRowFactory(param -> new TableRow<LogEntry>() {
			@Override
			protected void updateItem(LogEntry item, boolean empty) {
				super.updateItem(item, empty);
				pseudoClassStateChanged(emptySelector,  empty);
				pseudoClassStateChanged(activeRowSelector,  item != null && !item.isCommited());
				pseudoClassStateChanged(missingSelector,  item != null && item.getName().isEmpty());
				if (item != null && item.isInDb()) {
					setTooltip(new Tooltip(String.format(
							"vonalkód: %1$s, cikkszám: %2$s, terméknév: %3$s, kiadó: %4$s",
							item.getBarcode(), item.getProductId(), item.getName(), item.getPublisher()
					)));
				}
				if (item != null && !item.isInDb()) {
					setTooltip(new Tooltip(
							item.getBarcode() + " nem található az adatbázisban." +
							(!isValidGTIN(item.getBarcode()) ? " (ÉRVÉNYTELEN ISBN)" : "")
					));
				}
			}
			
			
		});

		pane.setCenter(table);
		
		primaryStage.setScene(scene);
	}
	
	private DatabaseEntry chooseDbEntry(List<DatabaseEntry> dbEntries) {
		
		alertSound1.play();
		
		Stage stage = new Stage();
		
		ListView<String> list = new ListView<String>();
		ObservableList<String> listItems =FXCollections.observableArrayList ();
		if (dbEntries != null && dbEntries.size() > 0) {
			for (DatabaseEntry dbEntry : dbEntries) {
				listItems.add(dbEntry.getBarcode() + "\t" + dbEntry.getName() + "\t" + dbEntry.getPublisher() + "\t" + dbEntry.getId());
			}
		}
		listItems.add("Egyik sem (Írj hozzá megjegyzést!)");
		list.setItems(listItems);
		final IntegerProperty selectedIndex = new SimpleIntegerProperty();
		selectedIndex.set(listItems.size()-1);
		list.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				selectedIndex.set(list.getSelectionModel().getSelectedIndex());
				try {
					Thread.sleep(100);	// For visual "selected" feedback
				} catch (InterruptedException e) {}
				stage.close();
			}
		});
		list.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ENTER)) {
					selectedIndex.set(list.getSelectionModel().getSelectedIndex());
					stage.close();
				}
			}
		});
		
		Label description = new Label("Ebből a termékből több is található az adatbázisban. Melyiket leltározod éppen?");
		description.setPadding(new Insets(10, 20, 10, 20));
		description.setStyle("-fx-font-size: 20;");
		
		BorderPane pane = new BorderPane();
		pane.setCenter(list);
		pane.setTop(description);
		
		
		Scene scene = new Scene(pane, 900, 300);
		stage.setScene(scene);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle("Melyik terméket leltározod?");
		
		stage.showAndWait();
		
		if (selectedIndex.get() < dbEntries.size()) {
			return dbEntries.get(selectedIndex.get());
		} else{
			return null;
		}
	}

	/**
	 * Shows a login screen.
	 */
	private void login() {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(15);
		grid.setVgap(15);
		grid.setPadding(new Insets(25, 25, 25, 25));
		
		final Text actiontarget = new Text();
		grid.add(actiontarget, 1, 6);

		Scene scene = new Scene(grid, 300, 275);
		//scene.getStylesheets().add("leltarozo/resources/style.css");

		final Text scenetitle = new Text("Bejelentkezés");
		scenetitle.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
		grid.add(scenetitle, 0, 0, 2, 1);

		final Label userNameLabel = new Label("Neved:");
		grid.add(userNameLabel, 0, 1);

		final TextField userTextField = new TextField();
		userTextField.setTooltip(new Tooltip("Ez a név kerül majd a leltár naplófájljába."));
		grid.add(userTextField, 1, 1);

		final Label locationLabel = new Label("Helyszín: ");
		grid.add(locationLabel, 0, 2);

		final ChoiceBox<String> locationChoiceBox = new ChoiceBox<String>(locationChoices);
		//locationChoiceBox.getSelectionModel().selectFirst();
		locationChoiceBox.setTooltip(new Tooltip("Válaszd ki, melyik helyen dolgozol."));
		grid.add(locationChoiceBox, 1, 2);

		final Button nextButton = new Button("Tovább");
		nextButton.setDefaultButton(true);
		nextButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (!userTextField.getText().isEmpty() && locationChoiceBox.getSelectionModel().getSelectedItem() != null) {
					operatorName = userTextField.getText();
					location = locationChoiceBox.getSelectionModel().getSelectedItem().toString();
					
					initializeMainGUI();
				}
			}
		});

		final HBox hbBtn = new HBox(10);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(nextButton);
		grid.add(hbBtn, 1, 4);

		final Button createPivotButton = new Button("Kimutatás készítése");
		createPivotButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				pivotCreator();
			}
		});
		grid.add(createPivotButton, 0, 4);

		primaryStage.setScene(scene);
	}

	/**
	 * Process log files and creates a summary and a pivot table output
	 */
	private void pivotCreator() {
		final String startTimeString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		final String concatFileName = startTimeString + "_leltár_részletes_adatok.csv";
		final String pivotFileName = startTimeString + "_leltár_eredmény.csv";
		final File logDir = new File(basePath + "log");
		final File pivotDir = new File(basePath + "kimutatások");
		BufferedWriter mergeWriter = null;
		BufferedWriter pivotWriter = null;
		
		Map<String, LogEntry> logItems = new HashMap<String, LogEntry>();
		final Date fakeDate = new Date();
		try {
			if (!logDir.exists()) {
				return;
			}
			if (!pivotDir.exists()) {
				pivotDir.mkdir();
			}
			File[] filesList = logDir.listFiles();
			Arrays.sort(filesList);
			mergeWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pivotDir.toString() + File.separator + concatFileName),"UTF-8"));
		
			for(File file : filesList) {
				if (file.isFile() && file.toString().toLowerCase().endsWith(".csv")) {
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
 
					String line;
					while ((line = in.readLine()) != null) {
						mergeWriter.write(line);
						mergeWriter.newLine();
					
						if (!line.trim().isEmpty()) {
							String[] values = new String[12];
							String[] lineArray = line.trim()
									.replace("\"\"", "۝")	// change excaped double quotes to a special character
									.replace("\"", "")		// remove double quotes
									.replace("۝", "\"")		// replace special character with one double quote character
									.split("\t");			// split by tab characters
							System.arraycopy(lineArray, 0, values, 0, lineArray.length);
							if (logItems.containsKey(values[1])) {
								logItems.get(values[1]).setCount(logItems.get(values[1]).getCount() + Integer.parseInt(values[2]));
							} else {
								DatabaseEntry dbEntry = null;
								if (db.getByBarcode(values[1]) != null) {
									for (DatabaseEntry item : db.getByBarcode(values[1])) {
										if (values[1].equals(item.getBarcode())) {
											dbEntry = item;
										}
									}
								}
								
								logItems.put(values[1], new LogEntry(
									values[1], Integer.parseInt(values[2]), values[3], values[4], dbEntry
								));
							}
						}
					}

					in.close();
					mergeWriter.flush();
				}
			}
			
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Naplófájlok összefűzve");
			alert.setHeaderText("A log mappa naplófájljai sikeresen össze lettek fűzve az alábbi fájlba: " + concatFileName);
			alert.showAndWait();
			
			
			pivotWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pivotDir.toString() + File.separator + pivotFileName),"UTF-8"));
			pivotWriter.write("\"Cikkszám\"\t\"Vonalkód\"\t\"Norm. vonalkód\"\t\"Cikknév\"\t\"Kiadó\"\t\"Készlet sz. m.\"\t\"Talált db\"\t\"Eltérés\"");
			pivotWriter.newLine();
			for (Map.Entry<String, LogEntry> entry : logItems.entrySet()) {
				final LogEntry item = entry.getValue();
				final Integer stockCount = item.getStockCount() != null ? item.getStockCount() : 0;

				pivotWriter.write(String.format("\"%1$s\"\t\"%2$s\"\t\"%3$s\"\t\"%4$s\"\t\"%5$s\"\t%6$d\t%7$d\t%8$s",
						item.getProductId(),
						item.getBarcode(),
						item.getNormalizedBarcode(),
						item.getName().replace("\"", "\"\""),
						item.getPublisher(),
						stockCount,
						item.getCount(),
						stockCount != item.getCount() ? "ELTÉR" : "OK"
				));
				pivotWriter.newLine();
			}
			pivotWriter.flush();
			
			alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Kimutatás elkészült");
			alert.setHeaderText("A log mappa naplófájljaiból elkészült a kimutatás: " + pivotFileName);
			alert.showAndWait();
		} catch (Exception e) {
			System.err.println("Kimutatások készítése sikertelen: " + e.getMessage());
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Kimutatások készítése sikertelen!");
			alert.setHeaderText(e.getMessage());
			alert.showAndWait();
		} finally {
			try {
				if (mergeWriter != null) {
					mergeWriter.close();
				}
				if (pivotWriter != null) {
					pivotWriter.close();
				}
			} catch (IOException e) {}
		}
	}
	
	/** 
	 * Calculates check digit for GTIN/EAN/ISBN codes. Supports every length from 8 to 14 digits (7 to 13 without check digit)
	 * 
	 * @param The GTIN/EAN/ISBN code without check digit
	 * @returns The check digit
	 */
	public static Integer calculateGTINCheckDigit(String code) {
		code = code.replaceAll("[^0-9]", "");
		if (code.length() < 7 || code.length() > 13) {
			return null;
		}
		code = "0000000000000".substring(code.length()) + code;
		int[] c = Arrays.stream(code.split("")).map(String::trim).mapToInt(Integer::parseInt).toArray();
		int sum = c[0]*3 + c[1] + c[2]*3 + c[3] + c[4]*3 + c[5] + c[6]*3 + c[7] + c[8]*3 + c[9] + c[10]*3 + c[11] + c[12]*3;
		return (sum % 10 > 0) ? (10 - (sum % 10)) : 0;
	}
	
	/** 
	 * Calculates check digit for old ISBN-10 codes. Input should be 9 digit length.
	 * 
	 * @param The ISBN-10 code without check digit
	 * @returns The check digit
	 */
	public static Integer calculateISBN10CheckDigit(String code) {
		code = code.replaceAll("[^0-9]", "");
		if (code.length() != 9) {
			return null;
		}
		int[] c = Arrays.stream(code.split("")).map(String::trim).mapToInt(Integer::parseInt).toArray();
		int sum = c[0]*10 + c[1]*9 + c[2]*8 + c[3]*7 + c[4]*6 + c[5]*5 + c[6]*4 + c[7]*3 + c[8]*2;
		return (11 - sum % 11) % 11;
	}
	
	/** 
	 * Validats GTIN/EAN/ISBN codes by checking its check digit (the last digit)
	 * 
	 * @param The code to check. Ignores non numerical characters
	 * @returns Returns true if the code has a proper length
	 *          and the check sum, calculated from the code equals to its last digit
	 */
	public static boolean isValidGTIN(String code) {
		code = code.replaceAll("[^0-9]", "");
		if (code.length() < 8 || code.length() > 14) {
			return false;
		}
		String baseCode = code.substring(0, code.length()-1);
		Integer checkDigit;
		if (code.length() == 10) {
			checkDigit = calculateISBN10CheckDigit(baseCode);
		} else {
			checkDigit = calculateGTINCheckDigit(baseCode);
		}
		return checkDigit != null ? checkDigit == code.charAt(code.length()-1)-'0' : false;
	}
}
