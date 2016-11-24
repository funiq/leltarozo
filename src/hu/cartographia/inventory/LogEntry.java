// License: GPL. For details, see LICENSE file

package hu.cartographia.inventory;
 
import java.util.Date;
import java.text.SimpleDateFormat;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 *
 * @since 2016-11-14
 * @author Báthory Péter <peter.bathory@cartographia.hu>
 */
public class LogEntry {
    private boolean isCommited = false;
    private Date timestamp;
    private DatabaseEntry data = null;
    
    private String time;
    private final StringProperty barcode;
    private final IntegerProperty count;
    private final StringProperty publicationDate;	// IntegerProperty doesn't support null value
    private final StringProperty comment;
 
    public LogEntry(String barcode, int count, String comment, String publicationDate, DatabaseEntry dbEntry) {
        this.timestamp = new Date();
        
        this.time = new SimpleDateFormat("HH:mm:ss").format(timestamp);
        this.barcode = new SimpleStringProperty(barcode);
        this.count = new SimpleIntegerProperty(count);
        this.publicationDate = new SimpleStringProperty(publicationDate);
        this.comment = new SimpleStringProperty(comment);
		this.data = dbEntry;
    }

    public LogEntry(String barcode, DatabaseEntry dbEntry) {
        this(barcode, 1, "", null, dbEntry);
    }

    public LogEntry(String barcode, int count, String comment, DatabaseEntry dbEntry) {
        this(barcode, count, comment, null, dbEntry);
    }


    public String getTime() {
        return time;
    }

    public String getBarcode() {
        return barcode.get();
    }
    public void setBarcode(String barcode) {
        this.barcode.set(barcode);
    }
    public StringProperty barcodeProperty() {
        return barcode;
    }

    public int getCount() {
        return count.get();
    }
    public void setCount(int count) {
        this.count.set(count);
    }
    public IntegerProperty countProperty() {
        return count;
    }

    public String getPublicationDate() {
        return publicationDate.get();
    }
    public void setPublicationDate(String publicationDate) {
        this.publicationDate.set(publicationDate);
    }
    public StringProperty publicationDateProperty() {
        return publicationDate;
    }

    public String getComment() {
        return comment.get();
    }
    public void setComment(String comment) {
        this.comment.set(comment);
    }
    public StringProperty commentProperty() {
        return comment;
    }


    public boolean isInDb() {
        return data != null ? true : false;
    }
    public String getName() {
        return data != null ? data.getName() : "";
    }
    public String getPublisher() {
        return data != null ? data.getPublisher() : "";
    }
    public Integer getStockCount() {
        return data != null ? data.getStockCount() : null;
    }
    public String getProductId() {
        return data != null ? data.getId() : "";
    }
    public String getNormalizedBarcode() {
        return data != null ? data.getNormalizedBarcode() : "";
    }
    public boolean isCommited() {
        return this.isCommited;
    }
    public void setIsCommited(boolean state) {
        this.isCommited = state;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
 
}
