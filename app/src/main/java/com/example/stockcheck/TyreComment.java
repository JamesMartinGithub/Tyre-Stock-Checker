package com.example.stockcheck;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores a string and all edits (deletions and insertions), with a method to get an html-annotated string of the changes.
 */
public class TyreComment implements Parcelable {

    private String rawField;
    private ArrayList<Character> commentChars;
    private ArrayList<Tyre.CharType> commentTypes;
    private Map<Integer, Integer> charIndexMap;

    /**
     * Constructor taking the initial string
     * @param data Initial string
     */
    public TyreComment(String data) {
        this.rawField = data;
    }

    /**
     * Returns either the raw string, or the html-annotated string with changes.
     * @param getRaw Whether to get the raw string
     * @return String
     */
    public String GetString(boolean getRaw) {
        if (commentChars == null || getRaw) {
            return rawField;
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < commentChars.size(); i++) {
                switch (commentTypes.get(i)) {
                    case UNCHANGED:
                        builder.append(commentChars.get(i));
                        break;
                    case INSERTED:
                        builder.append("<b><u><font color=\"#007700\">");
                        builder.append(commentChars.get(i));
                        builder.append("</font></u></b>");
                        break;
                    case DELETED:
                        builder.append("<s><font color=\"#770000\">");
                        builder.append(commentChars.get(i));
                        builder.append("</font></s>");
                        break;
                }
            }
            return builder.toString();
        }
    }

    /**
     * Updates the comment with a new one.
     * @param newComment Edited comment, with either one or many deletions, or one or many insertions
     */
    public void Edit(String newComment) {
        if (commentChars == null) {
            // Initialise lists and maps for editing
            commentChars = new ArrayList<>();
            commentTypes = new ArrayList<>();
            charIndexMap = new HashMap<>();
            for (int i = 0; i < rawField.length(); i++) {
                commentChars.add(rawField.charAt(i));
                commentTypes.add(Tyre.CharType.UNCHANGED);
                charIndexMap.put(i, i);
            }
        }
        // Find length and start of difference
        int lengthDiff = rawField.length() - newComment.length();
        int diffAbs = Math.abs(lengthDiff);
        if (lengthDiff > 0) {
            // Deletion
            int start = newComment.length();
            for (int i = 0; i < newComment.length(); i++) {
                if (rawField.charAt(i) != newComment.charAt(i)) {
                    start = i;
                    break;
                }
            }
            int loopOffset = 0;
            for (int i = start; i < start + diffAbs; i++) {
                if (commentTypes.get(charIndexMap.get(i - loopOffset)) == Tyre.CharType.INSERTED) {
                    // Deleted inserted characters can be removed from the lists as they don't need to be displayed
                    commentChars.remove(charIndexMap.get(i - loopOffset).intValue());
                    commentTypes.remove(charIndexMap.get(i - loopOffset).intValue());
                    RecreateMap();
                } else if (commentTypes.get(charIndexMap.get(i - loopOffset)) == Tyre.CharType.UNCHANGED) {
                    // Deleted unchanged characters are set to be of type deleted as they need to be displayed
                    commentTypes.set(charIndexMap.get(i - loopOffset), Tyre.CharType.DELETED);
                    RecreateMap();
                }
                loopOffset++;
            }
        } else if (lengthDiff < 0) {
            // Insertion
            // Find start of text difference (passed start index not usable for insertion)
            int start = rawField.length();
            for (int i = 0; i < rawField.length(); i++) {
                if (rawField.charAt(i) != newComment.charAt(i)) {
                    start = i;
                    break;
                }
            }
            for (int i = start; i < start + diffAbs; i++) {
                if (i >= charIndexMap.size()) {
                    // char at end of string, add new map entry at end
                    charIndexMap.put(i, (charIndexMap.size() > 0) ? (charIndexMap.get(i - 1) + 1) : 0);
                } else {
                    // Add 1 to key and value from i onwards
                    for (int mapI = charIndexMap.size() - 1; mapI >= i; mapI--) {
                        charIndexMap.put(mapI + 1, charIndexMap.get(mapI) + 1);
                    }
                }
                // Add new char to lists, with type inserted
                commentChars.add(charIndexMap.get(i), newComment.charAt(i));
                commentTypes.add(charIndexMap.get(i), Tyre.CharType.INSERTED);
            }
        }
        rawField = newComment;
    }

    /**
     * Updates the map between the raw string and list of all past and present characters
     */
    private void RecreateMap() {
        charIndexMap.clear();
        int visibleStringIndex = 0;
        for (int i = 0; i < commentTypes.size(); i++) {
            Tyre.CharType charType = commentTypes.get(i);
            if (charType == Tyre.CharType.UNCHANGED || charType == Tyre.CharType.INSERTED) {
                charIndexMap.put(visibleStringIndex, i);
                visibleStringIndex++;
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(rawField);
        parcel.writeList(commentChars);
        parcel.writeList(commentTypes);
    }

    public static final Parcelable.Creator<TyreComment> CREATOR = new Parcelable.Creator<TyreComment>() {
        public TyreComment createFromParcel(Parcel in) {
            return new TyreComment(in);
        }

        public TyreComment[] newArray(int size) {
            return new TyreComment[size];
        }
    };

    private TyreComment(Parcel in) {
        rawField = in.readString();
        commentChars = in.readArrayList(null);
        commentTypes = in.readArrayList(null);
    }
}
