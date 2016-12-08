package hu.cartographia.inventory;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * @author Caleb Brinkman
 * @source https://gist.github.com/floralvikings/10290131#file-autocompletetextbox-java
 */
public class AutoCompleteTextField extends TextField {

	final int maxSearchResults = 15;

	/** The existing autocomplete entries. */
	private final List<DatabaseEntry> entries;
	/** The popup used to select an entry. */
	private ContextMenu entriesPopup;

	/** Construct a new AutoCompleteTextField. */
	public AutoCompleteTextField() {
		super();
		entries = new ArrayList<DatabaseEntry>();
		entriesPopup = new ContextMenu();
		entriesPopup.setId("autoCompleteMenu");
		textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String s, String s2) {
				if (getText().length() == 0) {
					entriesPopup.hide();
				} else {
					
					if (entries.size() > 0 && !getText().matches("[-_ö\\d]+")) {
						String[] patterns = getText().toLowerCase().split(" ");
						
						populatePopup(entries.stream().filter(p-> {
								for (String pattern : patterns) {
									if (!p.getName().toLowerCase().contains(pattern)) {
										return false;
									}
								}
								return true;
						}).collect(Collectors.toList()));
						
						if (!entriesPopup.isShowing()) {
							entriesPopup.show(AutoCompleteTextField.this, Side.BOTTOM, 5, 0);
						}
					} else {
						entriesPopup.hide();
					}
				}
			}
		});

		focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
				entriesPopup.hide();
			}
		});

	}

	/**
	 * Get the existing set of autocomplete entries.
	 * @return The existing autocomplete entries.
	 */
	public List<DatabaseEntry> getEntries() {
		return entries;
	}

	/**
	 * Populate the entry set with the given search results.	Display is limited to 10 entries, for performance.
	 * @param searchResult The set of matching strings.
	 */
	private void populatePopup(List<DatabaseEntry> searchResult) {
		List<CustomMenuItem> menuItems = new LinkedList<>();
		// If you'd like more entries, modify this line.
		int count = Math.min(searchResult.size(), maxSearchResults);
		for (int i = 0; i < count; i++) {
			final DatabaseEntry result = searchResult.get(i);
			Label entryLabel = new Label(
				result.getName() + (!result.getId().isEmpty() ? " (" + result.getId() + ")" : "")
			);
			CustomMenuItem item = new CustomMenuItem(entryLabel, true);
			item.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					setText(result.getBarcode());
					fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, true, true, true, true));
					entriesPopup.hide();
				}
			});
			menuItems.add(item);
		}
		entriesPopup.getItems().clear();
		entriesPopup.getItems().addAll(menuItems);
	}
}
