package com.example.stockcheck;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class UIInstrumentedTests {

    @Before
    public void Setup() {
        Intents.init();
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        Intent resultData = new Intent();
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority("com.example.stockcheck.test")
                .appendPath("raw")
                .appendPath("test_xlsx_with_categories")
                .build();
        resultData.setData(uri);
        Instrumentation.ActivityResult result =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData);

        // Set content resolver to test mode to bypass calls to android file system
        TestableContentResolver.MakeTest();
        // Set file picker intent to return with test file uri
        Set<String> set = new HashSet<>();
        set.add(Intent.CATEGORY_OPENABLE);
        intending(hasCategories(set)).respondWith(result);
        // Start file picker intent
        onView(withId(R.id.fileButton)).perform(click());
    }

    @AfterClass
    public static void Cleanup() {
        Intents.release();
    }

    @Test
    public void Comment_Raw_Matches() {
        intended(hasComponent(TyreListActivity.class.getName()));
        onView((withId(R.id.searchButton))).check(matches(isDisplayed()));
    }
}
