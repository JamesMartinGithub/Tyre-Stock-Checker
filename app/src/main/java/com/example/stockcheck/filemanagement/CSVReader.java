package com.example.stockcheck.filemanagement;

import android.content.Context;
import android.net.Uri;
import com.example.stockcheck.model.Tyre;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains a static method to parse .csv files
 */
public class CSVReader {

    // Tyre data array:
    // Part = 0
    // Supplier Part Codes = 1
    // Description = 2
    // Location = 3
    // Stock = 4
    // Last Sold Date = 5
    // Category = 6

    /**
     * Reads and parses a .csv file to get a list of tyres
     * @param uri Uri of .csv file
     * @param applicationContext Application context
     * @return An ArrayList of tyres
     * @throws Exception If file cannot be read or contains invalid categories
     */
    public static ArrayList<Tyre> Read(Uri uri, Context applicationContext) throws Exception {
        ArrayList<Tyre> tyreList = new ArrayList<>();
        int tyresAdded = 0;
        try (InputStream inputStream = applicationContext.getContentResolver().openInputStream(uri)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            // Skip BOM char, if it appears
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != 65279) reader.reset();
            // Map category positions
            String line = reader.readLine();
            Map<Integer, Integer> categoryMap = new HashMap<>();
            final String[] categories = line.split(",");
            for (int i = 0; i < categories.length; i++) {
                switch (categories[i]) {
                    case "Part":
                        categoryMap.put(i, 0);
                        break;
                    case "Supplier Part Codes":
                        categoryMap.put(i, 1);
                        break;
                    case "Description":
                        categoryMap.put(i, 2);
                        break;
                    case "Location":
                        categoryMap.put(i, 3);
                        break;
                    case "On Stock":
                        categoryMap.put(i, 4);
                        break;
                    case "Last Sold Date":
                        categoryMap.put(i, 5);
                        break;
                    case "Category":
                        categoryMap.put(i, 6);
                        break;
                }
            }
            if (categoryMap.size() != 7) {
                // No category headers found, initialise categoryMap with default order
                categoryMap.put(0, 0);
                categoryMap.put(1, 1);
                categoryMap.put(2, 2);
                categoryMap.put(3, 3);
                categoryMap.put(6, 4);
                categoryMap.put(13, 5);
                categoryMap.put(7, 6);
            } else {
                line = reader.readLine();
            }
            while (line != null) {
                // Parse row
                String[] stockEntryData = new String[7];
                int categoryIndex = 0;
                StringBuilder builder = new StringBuilder();
                boolean inQuotes = false;
                for (char c : line.toCharArray()) {
                    switch (c) {
                        case ',':
                            if (!inQuotes) {
                                if (categoryMap.containsKey(categoryIndex)) stockEntryData[categoryMap.get(categoryIndex)] = builder.toString();
                                builder.setLength(0);
                                categoryIndex++;
                            } else {
                                builder.append(c);
                            }
                            break;
                        case '\"':
                            inQuotes = !inQuotes;
                            break;
                        default:
                            builder.append(c);
                            break;
                    }
                }
                if (builder.length() > 0 && categoryMap.containsKey(categoryIndex)) {
                    stockEntryData[categoryMap.get(categoryIndex)] = builder.toString();
                }
                // Check entry is a tyre
                if (stockEntryData[6].equals("Tyres") && (stockEntryData[0].startsWith("1") || stockEntryData[0].startsWith("2") || stockEntryData[0].startsWith("3"))) {
                    Tyre newTyre = new Tyre(tyresAdded, stockEntryData[0], stockEntryData[1], stockEntryData[2], stockEntryData[3], stockEntryData[4], stockEntryData[5], false);
                    tyreList.add(newTyre);
                    tyresAdded++;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            System.out.println(e.toString());
            throw e;
        }
        return tyreList;
    }
}