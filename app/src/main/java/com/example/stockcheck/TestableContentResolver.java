package com.example.stockcheck;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class TestableContentResolver {

    private static volatile boolean isTest = false;

    public static String GetType(Context context, Uri uri) throws Exception {
        if (!isTest) {
            String fileType = context.getContentResolver().getType(uri);
            if (fileType == null) {
                throw new Exception("Cannot get file type");
            } else {
                return fileType;
            }
        } else {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
    }

    public static String GetName(Context context, Uri uri) throws Exception {
        if (!isTest) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex != -1) {
                    String fileName = cursor.getString(columnIndex);
                    cursor.close();
                    return fileName;
                } else {
                    throw new Exception("Cannot get file name");
                }
            } else {
                throw new Exception("Cannot read file metadata");
            }
        } else {
            return "test_xlsx_with_categories";
        }
    }

    public static void MakeTest() {
        synchronized (TestableContentResolver.class) {
            isTest = true;
        }
    }
}
