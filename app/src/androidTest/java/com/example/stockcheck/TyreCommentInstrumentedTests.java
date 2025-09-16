package com.example.stockcheck;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import com.example.stockcheck.model.TyreComment;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class TyreCommentInstrumentedTests {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final String iStart = context.getString(R.string.inserted_start, context.getString(R.color.char_inserted));
    final String iEnd = context.getString(R.string.inserted_end);
    final String dStart = context.getString(R.string.deleted_start, context.getString(R.color.char_deleted));
    final String dEnd = context.getString(R.string.deleted_end);

    @Test
    public void Comment_Raw_Matches() {
        TyreComment comment = new TyreComment("abcd");
        assertEquals("abcd", comment.GetString(true, null));
    }

    @Test
    public void Comment_Raw_Spaces_Matches() {
        TyreComment comment = new TyreComment("a bc  d");
        assertEquals("a bc  d", comment.GetString(true, null));
    }

    @Test
    public void Comment_Add_1() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("abcde");
        assertEquals("abcd" + iStart + "e" + iEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Add_Multiple() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("abcdefg");
        assertEquals("abcd" + iStart + "efg" + iEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Remove_1() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("abc");
        assertEquals("abc" + dStart + "d" + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Remove_Multiple() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("a");
        assertEquals("a" + dStart + "bcd" + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Remove_All() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("");
        assertEquals(dStart + "abcd" + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Remove_All_Spaces() {
        TyreComment comment = new TyreComment("a b  cd ");
        comment.Edit("");
        assertEquals(dStart + "a b  cd " + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Add_Then_Remove() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("abcdefg");
        comment.Edit("abcd");
        assertEquals("abcd", comment.GetString(false, context));
    }

    @Test
    public void Comment_Add_Then_Remove_Spaces() {
        TyreComment comment = new TyreComment("ab c  d ");
        comment.Edit("ab c  d e fg  ");
        comment.Edit("ab c  d ");
        assertEquals("ab c  d ", comment.GetString(false, context));
    }

    @Test
    public void Comment_Add_Then_Remove_More() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("abcdefg");
        comment.Edit("abc");
        assertEquals("abc" + dStart + "d" + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Add_Then_Remove_More_Spaces() {
        TyreComment comment = new TyreComment("ab c  d ");
        comment.Edit("ab c  d e fg  ");
        comment.Edit("ab c ");
        assertEquals("ab c " + dStart + " d " + dEnd, comment.GetString(false, context));
    }

    @Test
    public void Comment_Remove_Then_Add() {
        TyreComment comment = new TyreComment("abcdefg");
        comment.Edit("abcd");
        comment.Edit("abcdxyz");
        String[] acceptedResults = {"abcd" + dStart + "efg" + dEnd + iStart + "xyz" + iEnd, "abcd" + iStart + "xyz" + iEnd + dStart + "efg" + dEnd};
        assertTrue(Arrays.asList(acceptedResults).contains(comment.GetString(false, context)));
    }

    @Test
    public void Comment_Remove_Then_Add_Spaces() {
        TyreComment comment = new TyreComment("abc de fg  ");
        comment.Edit("abc d");
        comment.Edit("abc dxy z");
        String[] acceptedResults = {"abc d" + dStart + "e fg  " + dEnd + iStart + "xy z" + iEnd, "abc d" + iStart + "xy z" + iEnd + dStart + "e fg  " + dEnd};
        assertTrue(Arrays.asList(acceptedResults).contains(comment.GetString(false, context)));
    }

    @Test
    public void Comment_Remove_Add_Same_Chars() {
        TyreComment comment = new TyreComment("abcdefg");
        comment.Edit("abcd");
        comment.Edit("abcdefg");
        String[] acceptedResults = {"abcd" + dStart + "efg" + dEnd + iStart + "efg" + iEnd, "abcd" + iStart + "efg" + iEnd + dStart + "efg" + dEnd};
        assertTrue(Arrays.asList(acceptedResults).contains(comment.GetString(false, context)));
    }

    @Test
    public void Comment_Remove_All_Then_Add() {
        TyreComment comment = new TyreComment("abcd");
        comment.Edit("");
        comment.Edit("efgh");
        String[] acceptedResults = {dStart + "abcd" + dEnd + iStart + "efgh" + iEnd, iStart + "efgh" + iEnd + dStart + "abcd" + dEnd};
        assertTrue(Arrays.asList(acceptedResults).contains(comment.GetString(false, context)));
    }

    @Test
    public void Comment_Remove_All_Spaces_Then_Add() {
        TyreComment comment = new TyreComment(" ab cd  ");
        comment.Edit("");
        comment.Edit(" efg h ");
        String[] acceptedResults = {dStart + " ab cd  " + dEnd + iStart + " efg h " + iEnd, iStart + " efg h " + iEnd + dStart + " ab cd  " + dEnd};
        assertTrue(Arrays.asList(acceptedResults).contains(comment.GetString(false, context)));
    }
}