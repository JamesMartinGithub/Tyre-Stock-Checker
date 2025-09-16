package com.example.stockcheck;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import com.example.stockcheck.filemanagement.CSVReader;
import com.example.stockcheck.model.Tyre;
import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class CSVParsingInstrumentedTests {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    boolean setupDone = false;
    ArrayList<Tyre> categoriesTyreList = null;
    ArrayList<Tyre> noCategoriesTyreList = null;
    Tyre categoriesTyre0 = null;
    Tyre categoriesTyre5 = null;
    Tyre categoriesTyre8 = null;
    boolean categoriesTyresNotNull = false;
    Tyre noCategoriesTyre0 = null;
    Tyre noCategoriesTyre5 = null;
    Tyre noCategoriesTyre8 = null;
    boolean noCategoriesTyresNotNull = false;

    @Before
    public void Setup() {
        if (!setupDone) {
            // Parse csv with categories
            Uri categoriesUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority("com.example.stockcheck.test")
                    .appendPath("raw")
                    .appendPath("test_csv_with_categories")
                    .build();
            try {
                categoriesTyreList = CSVReader.Read(categoriesUri, context);
            } catch (Exception ignored) {}
            // Parse csv without categories
            Uri noCategoriesUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority("com.example.stockcheck.test")
                    .appendPath("raw")
                    .appendPath("test_csv_without_categories")
                    .build();
            try {
                noCategoriesTyreList = CSVReader.Read(noCategoriesUri, context);
            } catch (Exception ignored) {}

            // Get individual Tyres with categories
            try {
                categoriesTyre0 = categoriesTyreList.get(0);
                categoriesTyre5 = categoriesTyreList.get(5);
                categoriesTyre8 = categoriesTyreList.get(8);
                categoriesTyresNotNull = true;
            } catch (Exception ignored) {}
            // Get individual Tyres without categories
            try {
                noCategoriesTyre0 = noCategoriesTyreList.get(0);
                noCategoriesTyre5 = noCategoriesTyreList.get(5);
                noCategoriesTyre8 = noCategoriesTyreList.get(8);
                noCategoriesTyresNotNull = true;
            } catch (Exception ignored) {}

            setupDone = true;
        }
    }

    @Test
    public void Parse_CSV_Categories_All_Parsed() {
        assertNotNull(categoriesTyreList);
        assertEquals(9, categoriesTyreList.size());
    }

    @Test
    public void Parse_CSV_No_Categories_All_Parsed() {
        assertNotNull(noCategoriesTyreList);
        assertEquals(9, noCategoriesTyreList.size());
    }

    @Test
    public void Parse_CSV_Categories_Correct_Part() {
        assertTrue(categoriesTyresNotNull);
        assertEquals("1954515, r15", categoriesTyre0.GetPart(true, null));
        assertEquals("2254518", categoriesTyre5.GetPart(true, null));
        assertEquals("2553019", categoriesTyre8.GetPart(true, null));
    }

    @Test
    public void Parse_CSV_Categories_Correct_Supplier_Part_Code() {
        assertTrue(categoriesTyresNotNull);
        assertEquals("Posuere, cubilia", categoriesTyre0.GetSupplierPartCode(true, null));
        assertEquals("Efficitur", categoriesTyre5.GetSupplierPartCode(true, null));
        assertEquals("Nullam id", categoriesTyre8.GetSupplierPartCode(true, null));
    }

    @Test
    public void Parse_CSV_Categories_Correct_Description() {
        assertTrue(categoriesTyresNotNull);
        assertEquals(" 195/45 R15 80T Hendrerit", categoriesTyre0.GetDescription(true, null));
        assertEquals("225/45 R18 94W Suspendisse", categoriesTyre5.GetDescription(true, null));
        assertEquals("255/30 R19 110W Suspendisse", categoriesTyre8.GetDescription(true, null));
    }

    @Test
    public void Parse_CSV_Categories_Correct_Location() {
        assertTrue(categoriesTyresNotNull);
        assertEquals("Aliquam", categoriesTyre0.GetLocation(true, null));
        assertEquals("Luctus", categoriesTyre5.GetLocation(true, null));
        assertEquals("Aliquam, Massa", categoriesTyre8.GetLocation(true, null));
    }

    @Test
    public void Parse_CSV_Categories_Correct_Stock() {
        assertTrue(categoriesTyresNotNull);
        assertEquals("3", categoriesTyre0.GetStock());
        assertEquals("4", categoriesTyre5.GetStock());
        assertEquals("4", categoriesTyre8.GetStock());
    }

    @Test
    public void Parse_CSV_Categories_Correct_Last_Sold_Date() {
        assertTrue(categoriesTyresNotNull);
        assertEquals("17/01/2023", categoriesTyre0.GetLastSoldDate(true));
        assertEquals("08/06/2023", categoriesTyre5.GetLastSoldDate(true));
        assertEquals("18/06/2025", categoriesTyre8.GetLastSoldDate(true));
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Part() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals("1954515, r15", noCategoriesTyre0.GetPart(true, null));
        assertEquals("2254518", noCategoriesTyre5.GetPart(true, null));
        assertEquals("2553019", noCategoriesTyre8.GetPart(true, null));
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Supplier_Part_Code() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals("Posuere, cubilia", noCategoriesTyre0.GetSupplierPartCode(true, null));
        assertEquals("Efficitur", noCategoriesTyre5.GetSupplierPartCode(true, null));
        assertEquals("Nullam id", noCategoriesTyre8.GetSupplierPartCode(true, null));
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Description() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals(" 195/45 R15 80T Hendrerit", noCategoriesTyre0.GetDescription(true, null));
        assertEquals("225/45 R18 94W Suspendisse", noCategoriesTyre5.GetDescription(true, null));
        assertEquals("255/30 R19 110W Suspendisse", noCategoriesTyre8.GetDescription(true, null));
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Location() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals("Aliquam", noCategoriesTyre0.GetLocation(true, null));
        assertEquals("Luctus", noCategoriesTyre5.GetLocation(true, null));
        assertEquals("Aliquam, Massa", noCategoriesTyre8.GetLocation(true, null));
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Stock() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals("3", noCategoriesTyre0.GetStock());
        assertEquals("4", noCategoriesTyre5.GetStock());
        assertEquals("4", noCategoriesTyre8.GetStock());
    }

    @Test
    public void Parse_CSV_No_Categories_Correct_Last_Sold_Date() {
        assertTrue(noCategoriesTyresNotNull);
        assertEquals("17/01/2023", noCategoriesTyre0.GetLastSoldDate(true));
        assertEquals("08/06/2023", noCategoriesTyre5.GetLastSoldDate(true));
        assertEquals("18/06/2025", noCategoriesTyre8.GetLastSoldDate(true));
    }
}
