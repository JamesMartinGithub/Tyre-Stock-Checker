package com.example.stockcheck;

import static org.junit.Assert.assertEquals;
import com.example.stockcheck.model.Tyre;
import org.junit.Test;
import java.util.ArrayList;

public class TyreUnitTests {

    @Test
    public void Tyre_Part_Number_Parsed() {
        Tyre tyre = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        assertEquals("1954515", tyre.GetPartNumber());
    }

    @Test
    public void Tyre_Part_Number_Parsed_With_Extra() {
        Tyre tyre = new Tyre(0, "1954515 test", "partCode", "description", "location", "5", "17/01/2023", false);
        assertEquals("1954515", tyre.GetPartNumber());
    }

    @Test
    public void Tyre_Part_Number_Parsed_Unparsable() {
        Tyre tyre = new Tyre(0, "test", "partCode", "description", "location", "5", "17/01/2023", false);
        assertEquals("0", tyre.GetPartNumber());
    }

    @Test
    public void Tyre_Stock_Integer_Parsed() {
        Tyre tyre = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        assertEquals("5", tyre.GetStock());
    }

    @Test
    public void Tyre_Stock_Float_Parsed() {
        Tyre tyre = new Tyre(0, "1954515", "partCode", "description", "location", "5.00", "17/01/2023", false);
        assertEquals("5", tyre.GetStock());
    }

    @Test
    public void Tyre_Stock_Parsed_Unparsable() {
        Tyre tyre = new Tyre(0, "1954515", "partCode", "description", "location", "test", "17/01/2023", false);
        assertEquals("test", tyre.GetStock());
    }

    @Test
    public void Tyre_Part_Number_Sorts_Small_Diff() {
        Tyre tyre1 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        Tyre tyre2 = new Tyre(0, "1954520", "partCode", "description", "location", "5", "17/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByPartNumber());
        assertEquals(tyre1, tyreList.get(0));
    }

    @Test
    public void Tyre_Part_Number_Sorts_Large_Diff() {
        Tyre tyre1 = new Tyre(0, "2644515", "partCode", "description", "location", "5", "17/01/2023", false);
        Tyre tyre2 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByPartNumber());
        assertEquals(tyre2, tyreList.get(0));
    }

    @Test
    public void Tyre_Last_Sold_Date_Sorts_Day_Diff() {
        Tyre tyre1 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        Tyre tyre2 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "20/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByLastSoldDate());
        assertEquals(tyre1, tyreList.get(0));
    }

    @Test
    public void Tyre_Last_Sold_Date_Sorts_Month_Diff() {
        Tyre tyre1 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/04/2023", false);
        Tyre tyre2 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByLastSoldDate());
        assertEquals(tyre2, tyreList.get(0));
    }

    @Test
    public void Tyre_Last_Sold_Date_Sorts_Year_Diff() {
        Tyre tyre1 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2025", false);
        Tyre tyre2 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByLastSoldDate());
        assertEquals(tyre2, tyreList.get(0));
    }

    @Test
    public void Tyre_Stock_Sorts() {
        Tyre tyre1 = new Tyre(0, "1954515", "partCode", "description", "location", "1", "17/01/2023", false);
        Tyre tyre2 = new Tyre(0, "1954515", "partCode", "description", "location", "5", "17/01/2023", false);
        ArrayList<Tyre> tyreList = new ArrayList<>();
        tyreList.add(tyre1);
        tyreList.add(tyre2);
        tyreList.sort(new Tyre.SortByStock());
        assertEquals(tyre1, tyreList.get(0));
    }
}