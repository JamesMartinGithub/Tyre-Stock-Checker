package com.example.stockcheck;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.Comparator;

/**
 * A store of tyre data with methods to edit fields, and part number, last sold date, and stock comparators.
 */
public class Tyre implements Parcelable {

    public enum CharType { UNCHANGED, DELETED, INSERTED }

    private TyreComment part;
    private int partNumber;
    private TyreComment supplierPartCode;
    private TyreComment description;
    private TyreComment location;
    private String stock;
    private float stockFloat;
    private String seen = "0";
    private String lastSoldDateString;
    private int lastSoldDateInt;
    private boolean lastSoldDateChanged = false;
    private String extraComment = "";
    private boolean seenIsNumber = true;
    private boolean isEdited = false;
    public boolean isDone = false;

    /**
     * Constructor that takes initial field strings, all must be non-null.
     * @param part
     * @param supplierPartCode
     * @param description
     * @param location
     * @param stock
     * @param lastSoldDate
     */
    public Tyre(String part, String supplierPartCode, String description, String location, String stock, String lastSoldDate) {
        this.part = new TyreComment(part);
        this.supplierPartCode = new TyreComment(supplierPartCode);
        this.description = new TyreComment(description);
        this.location = new TyreComment(location);
        if (stock.contains(".")) {
            this.stock = stock.split("\\.")[0];
        } else {
            this.stock = stock;
        }
        TryParseStockNumber(stock);
        this.lastSoldDateString = lastSoldDate;
        this.lastSoldDateInt = TryParseDate(lastSoldDate);
        TryParsePartNumber(part);
    }

    public String GetPart(boolean getRaw) {
        return part.GetString(getRaw);
    }

    public String GetPartNumber() {
        return String.valueOf(partNumber);
    }

    public String GetSupplierPartCode(boolean getRaw) {
        return supplierPartCode.GetString(getRaw);
    }

    public String GetDescription(boolean getRaw) {
        return description.GetString(getRaw);
    }

    public String GetLocation(boolean getRaw) {
        return location.GetString(getRaw);
    }

    public String GetStock() {
        return stock;
    }

    public String GetSeen() {
        return seen;
    }

    public boolean IsSeenNumber() {
        return seenIsNumber;
    }

    public String GetLastSoldDate(boolean getRaw) {
        if (getRaw || !lastSoldDateChanged) {
            return lastSoldDateString;
        } else {
            return "<b><u><font color=\"#007700\">" + lastSoldDateString + "</font></u></b>";
        }
    }

    public String GetComment(boolean getRaw) {
        if (getRaw) return extraComment;
        else return "<i><font color=\"#007700\">" + extraComment + "</font></i>";
    }
    public boolean IsEdited() {
        return isEdited;
    }

    public void EditPart(String newComment, int start) {
        part.Edit(newComment, start);
        TryParsePartNumber(newComment);
        isEdited = true;
    }

    public void EditSupplierPartCode(String newComment, int start) {
        supplierPartCode.Edit(newComment, start);
        isEdited = true;
    }

    public void EditDescription(String newComment, int start) {
        description.Edit(newComment, start);
        isEdited = true;
    }

    public void EditLocation(String newComment, int start) {
        location.Edit(newComment, start);
        isEdited = true;
    }

    public void EditSeen(String newComment) {
        seen = newComment;
        try {
            Integer.parseUnsignedInt(seen);
            seenIsNumber = true;
        } catch (NumberFormatException e) {
            seenIsNumber = false;
        }
        isEdited = true;
    }

    public void EditSeen(boolean isPlus) {
        if (seenIsNumber) {
            try {
                int currentNum = Integer.parseUnsignedInt(seen);
                currentNum = Math.max(isPlus ? currentNum + 1 : currentNum - 1, 0);
                seen = String.valueOf(currentNum);
                seenIsNumber = true;
                isEdited = true;
            } catch (NumberFormatException e) {
                seenIsNumber = false;
            }
        }
    }

    public void EditLastSoldDate(String newComment) {
        lastSoldDateString = newComment;
        lastSoldDateChanged = true;
        lastSoldDateInt = TryParseDate(newComment);
        isEdited = true;
    }

    public void EditComment(String newComment) {
        extraComment = newComment;
        isEdited = true;
    }

    /**
     * Converts a date string to an integer for comparison, if in the correct format. If invalid, returns 0;
     * @param dateString / seperated date in DD/MM/YYYY format
     * @return An integer representing the date
     */
    static private int TryParseDate(String dateString) {
        try {
            String[] parts = dateString.split("/");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return (year * 10000) + (month * 100) + day;
            } else {
                throw new Exception("Invalid date string");
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Converts a part number to an integer for comparison, if in the correct format. If invalid, sets to 0;
     * @param part Full part field, starting with the 7 digit part number
     */
    private void TryParsePartNumber(String part) {
        try {
            String partNumString = part.substring(0, 7);
            partNumber = Integer.parseUnsignedInt(partNumString);
        } catch (Exception e) {
            partNumber = 0;
        }
    }

    /**
     * Converts a stock number to an integer for comparison, if in the correct format. If invalid, sets to 0;
     * @param stock Full part field, starting with the 7 digit part number
     */
    private void TryParseStockNumber(String stock) {
        try {
            stockFloat = Float.parseFloat(stock);
        } catch (Exception e) {
            stockFloat = 0f;
        }
    }

    /**
     * Comparator that sorts first on part number, and then on the part field string
     */
    public static class SortByPartNumber implements Comparator<Tyre> {
        public int compare(Tyre tyre1, Tyre tyre2) {
            int compResult = Integer.compare(tyre1.partNumber, tyre2.partNumber);
            if (compResult == 0) {
                return tyre1.GetPart(true).compareTo(tyre2.GetPart(true));
            }
            return compResult;
        }
    }

    /**
     * Comparator that sorts first on date, and then on the part field string
     */
    public static class SortByLastSoldDate implements Comparator<Tyre> {
        public int compare(Tyre tyre1, Tyre tyre2) {
            int compResult = Integer.compare(tyre1.lastSoldDateInt, tyre2.lastSoldDateInt);
            if (compResult == 0) {
                return tyre1.GetPart(true).compareTo(tyre2.GetPart(true));
            }
            return compResult;
        }
    }

    /**
     * Comparator that sorts first on stock, and then on the part field string
     */
    public static class SortByStock implements Comparator<Tyre> {
        public int compare(Tyre tyre1, Tyre tyre2) {
            int compResult = Float.compare(tyre1.stockFloat, tyre2.stockFloat);
            if (compResult == 0) {
                return tyre1.GetPart(true).compareTo(tyre2.GetPart(true));
            }
            return compResult;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(part, 0);
        parcel.writeInt(partNumber);
        parcel.writeParcelable(supplierPartCode, 0);
        parcel.writeParcelable(description, 0);
        parcel.writeParcelable(location, 0);
        parcel.writeString(stock);
        parcel.writeString(seen);
        parcel.writeString(lastSoldDateString);
        parcel.writeInt(lastSoldDateInt);
    }

    public static final Parcelable.Creator<Tyre> CREATOR = new Parcelable.Creator<Tyre>() {
        public Tyre createFromParcel(Parcel in) {
            return new Tyre(in);
        }

        public Tyre[] newArray(int size) {
            return new Tyre[size];
        }
    };

    private Tyre(Parcel in) {
        part = in.readParcelable(TyreComment.class.getClassLoader());
        partNumber = in.readInt();
        supplierPartCode = in.readParcelable(TyreComment.class.getClassLoader());
        description = in.readParcelable(TyreComment.class.getClassLoader());
        location = in.readParcelable(TyreComment.class.getClassLoader());
        stock = in.readString();
        seen = in.readString();
        lastSoldDateString = in.readString();
        lastSoldDateInt = in.readInt();
    }
}
