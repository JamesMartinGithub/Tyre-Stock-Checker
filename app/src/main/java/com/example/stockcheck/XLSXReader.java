package com.example.stockcheck;

import android.content.Context;
import android.net.Uri;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Contains a static method to parse .xlsx files
 */
public class XLSXReader {

    // Tyre data array:
    // Part = 0
    // Supplier Part Codes = 1
    // Description = 2
    // Location = 3
    // Stock = 4
    // Last Sold Date = 5
    // Category = 6

    /**
     * Reads and parses a .xlsx file to get a list of tyres
     * @param uri Uri of a .xlsx file
     * @param applicationContext Application context
     * @return An ArrayList of tyres
     * @throws Exception If file cannot be read, or contains invalid categories
     */
    public static ArrayList<Tyre> Read(Uri uri, Context applicationContext) throws Exception {
        // Get list of shared strings
        ArrayList<String> sharedStrings = new ArrayList<>();
        try (InputStream inputStream = applicationContext.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                // Since .xlsx is a zip file, use ZipInputStream to read contents
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    if (entryName.contains("sharedStrings.xml")) {
                        // Parse shared strings xml
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        InputSource source = new InputSource(reader);
                        // Skip BOM char, if it appears
                        reader.mark(1);
                        int firstChar = reader.read();
                        if (firstChar != 65279) reader.reset();
                        // Create document object to represent xml
                        Document document = builder.parse(source);
                        Element rootElement = document.getDocumentElement();
                        NodeList allNodes = rootElement.getChildNodes();
                        // Add all shared strings, in order, to list
                        for (int i = 0; i < allNodes.getLength(); i++) {
                            sharedStrings.add(allNodes.item(i).getChildNodes().item(0).getTextContent());
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            throw e;
        }

        // Get list of tyres
        ArrayList<Tyre> tyreList = new ArrayList<>();
        try (InputStream inputStream = applicationContext.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                // Since .xlsx is a zip file, use ZipInputStream to read contents
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    if (entryName.contains("sheet1.xml")) {
                        // Parse spreadsheet xml
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        InputSource source = new InputSource(reader);
                        // Skip BOM char, if it appears
                        reader.mark(1);
                        int firstChar = reader.read();
                        if (firstChar != 65279) reader.reset();
                        // Create document object to represent xml
                        Document document = builder.parse(source);
                        Element rootElement = document.getDocumentElement();
                        NodeList allNodes = rootElement.getChildNodes();
                        // Traverse document tree to get column data for each row
                        for (int i = 0; i < allNodes.getLength(); i++) {
                            if (allNodes.item(i).getNodeName().equals("sheetData")) {
                                NodeList rows = allNodes.item(i).getChildNodes();
                                // Map category names
                                Map<Character, Integer> categoryMap = new HashMap<>();
                                NodeList categoryColumns = rows.item(0).getChildNodes();
                                for (int column = 0; column < categoryColumns.getLength(); column++) {
                                    Element columnElement = (Element)categoryColumns.item(column);
                                    String categoryName = columnElement.getChildNodes().item(0).getTextContent();
                                    if (columnElement.getAttribute("t").equals("s")) {
                                        // Replace shared string
                                        categoryName = sharedStrings.get(Integer.parseInt(categoryName));
                                    }
                                    switch (categoryName) {
                                        case "Part":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 0);
                                            break;
                                        case "Supplier Part Codes":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 1);
                                            break;
                                        case "Description":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 2);
                                            break;
                                        case "Location":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 3);
                                            break;
                                        case "On Stock":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 4);
                                            break;
                                        case "Last Sold Date":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 5);
                                            break;
                                        case "Category":
                                            categoryMap.put(columnElement.getAttribute("r").charAt(0), 6);
                                            break;
                                    }
                                }
                                if (categoryMap.size() != 7) throw new Exception("Invalid spreadsheet categories");
                                // Iterate over each row (stock entry)
                                for (int row = 1; row < rows.getLength(); row++) {
                                    String[] stockEntryData = new String[7];
                                    NodeList columns = rows.item(row).getChildNodes();
                                    for (int column = 0; column < columns.getLength(); column++) {
                                        Element columnElement = (Element)columns.item(column);
                                        String columnData = columnElement.getChildNodes().item(0).getTextContent();
                                        if (columnElement.getAttribute("t").equals("s")) {
                                            columnData = sharedStrings.get(Integer.parseInt(columnData));
                                        }
                                        char columnId = columnElement.getAttribute("r").charAt(0);
                                        if (categoryMap.containsKey(columnId)) {
                                            stockEntryData[categoryMap.get(columnId)] = columnData;
                                        }
                                    }
                                    // Ensure no strings are null
                                    for (int c = 0; c < 7; c++) if (stockEntryData[c] == null) stockEntryData[c] = "";
                                    // Check entry is a tyre
                                    if (stockEntryData[6].equals("Tyres") && (stockEntryData[0].startsWith("1") || stockEntryData[0].startsWith("2") || stockEntryData[0].startsWith("3"))) {
                                        // Create new tyre and add it to tyre list
                                        Tyre newTyre = new Tyre(stockEntryData[0], stockEntryData[1], stockEntryData[2], stockEntryData[3], stockEntryData[4], stockEntryData[5], false);
                                        tyreList.add(newTyre);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            throw e;
        }
        return tyreList;
    }
}
