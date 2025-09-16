package com.example.stockcheck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.stockcheck.databinding.ActivityTyreListBinding;
import com.example.stockcheck.model.Tyre;
import com.example.stockcheck.model.TyreContainer;
import com.example.stockcheck.storage.MetaData;
import com.example.stockcheck.storage.StoredTyre;
import com.example.stockcheck.storage.TyreDatabase;
import com.google.android.material.snackbar.Snackbar;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Activity that displays tyres and allows searching, sorting, and selecting.
 */
public class TyreListActivity extends AppCompatActivity {

    private enum SortCategory { PARTNUMBER, LASTSOLDDATE, STOCK }

    private ActivityTyreListBinding binding;
    private String filename;
    private ArrayList<Tyre> tyreList;
    private final ArrayList<Integer> displayedTyreIndexes = new ArrayList<>();
    private final int tyreIdOffset = 10;
    /**
     * Number of tyres displayed at a time, should be 20 minimum.
     */
    private int maxDisplayedTyres;
    /**
     * Minimum value to read from vertical scrollbar (value at which previous tyre rows can start being hidden).
     */
    private final int minScrollY = 119 + (int)(101 * (5 / 2.0f));
    private int lastTopTyreIndex = 0;
    private final ArrayList<Integer> tableRowIDs = new ArrayList<>();
    private int rowCount = 0;
    private boolean inEditMode = false;
    private boolean inOptions = false;
    private int dragStartPosX = 0;
    private int startDragWidth = 0;
    private boolean supplierPartCodeExpanded = false;
    private Tyre selectedTyre;
    private String selectedTyreTag;
    private SortCategory sortCategory = SortCategory.PARTNUMBER;
    private boolean sortAscending = true;
    private String filterWidth = "";
    private String filterRatio = "";
    private String filterRim = "";
    private String filterSearch = "";
    private boolean showUnstocked = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve data passed through intent
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            filename = intentExtras.getString("filename");
        }

        binding = ActivityTyreListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialise maxDisplayedTyres to fit the device's screen size
        int heightDp = (int)(getResources().getDisplayMetrics().heightPixels /  getApplicationContext().getResources().getDisplayMetrics().density) - 174;
        maxDisplayedTyres = (heightDp / 38) + 5;

        // Create back button callback to intercept back button presses
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (inEditMode) {
                    CloseEditor();
                } else {
                    HideTextEditCursor();
                    DeselectTyre();
                }
            }
        });

        // Create handle listeners
        HandleTouchListener handleTouchListener = new HandleTouchListener();
        binding.partHandle.setOnTouchListener(handleTouchListener);
        binding.descriptionHandle.setOnTouchListener(handleTouchListener);
        binding.locationHandle.setOnTouchListener(handleTouchListener);
        binding.supplierPartCodeHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                supplierPartCodeExpanded = !supplierPartCodeExpanded;
                if (supplierPartCodeExpanded) {
                    SetColumnWidths(R.id.supplierPartCodeText, 1500);
                } else {
                    SetColumnWidths(R.id.supplierPartCodeText, 0);
                }
            }
        });

        // Create scroll listener that creates TyreFragments as it scrolls and deletes ones outside of view
        binding.verticalScroll.SetOnScrollCallback(new ScrollViewNotifying.OnScrollCallback() {
            @Override
            public void OnScrollChanged(int x, int y, int oldx, int oldy) {
                // Subtract options and select tabs from scrollY if visible
                int maxNewY = y - (inOptions ? 158 : 0) - (selectedTyre != null ? 158 : 0);
                // Cap minimum to y pos at which first tyre row can be culled)
                maxNewY = Math.max(maxNewY, minScrollY);
                // Cap maximum to y pos 10 rows above last row
                maxNewY = Math.min(maxNewY, 119 + ((Math.max(rowCount - 6, 2)) * 101));
                // Divide y by tyre row height to get number of tyres scrolled by (= no. of tyre rows to remove and add)
                int topTyreIndex = Math.abs(maxNewY - minScrollY) / 101;
                if (topTyreIndex != lastTopTyreIndex) {
                    if (topTyreIndex > lastTopTyreIndex) {
                        // Scrolled down
                        for (int i = lastTopTyreIndex; i < topTyreIndex; i++) {
                            // Remove previous tyre fragment
                            Fragment tyreFragment = getSupportFragmentManager().findFragmentByTag("TyreFrag" + (i + tyreIdOffset));
                            if (tyreFragment != null) {
                                getSupportFragmentManager().beginTransaction().remove(tyreFragment).commit();
                            }
                            // Add new tyre fragment
                            String newFragTag = "TyreFrag" + (i + tyreIdOffset + maxDisplayedTyres);
                            if (rowCount > i + maxDisplayedTyres && getSupportFragmentManager().findFragmentByTag(newFragTag) == null) {
                                boolean shouldBeSelected = selectedTyreTag != null && selectedTyreTag.equals(newFragTag);
                                getSupportFragmentManager().beginTransaction().add(tableRowIDs.get(i + maxDisplayedTyres), TyreFragment.newInstance(tyreList.get(displayedTyreIndexes.get(i + maxDisplayedTyres)), shouldBeSelected, new TyreFragment.Widths(0, 0, 0, 0)), newFragTag).commit();
                            }
                        }
                    } else {
                        // Scrolled up
                        for (int i = topTyreIndex; i < lastTopTyreIndex; i++) {
                            // Remove previous tyre fragment
                            Fragment tyreFragment = getSupportFragmentManager().findFragmentByTag("TyreFrag" + (i + tyreIdOffset + maxDisplayedTyres));
                            if (tyreFragment != null) {
                                getSupportFragmentManager().beginTransaction().remove(tyreFragment).commit();
                            }
                            // Add new tyre fragment
                            String newFragTag = "TyreFrag" + (i + tyreIdOffset);
                            if (rowCount > i && getSupportFragmentManager().findFragmentByTag(newFragTag) == null) {
                                boolean shouldBeSelected = selectedTyreTag != null && selectedTyreTag.equals(newFragTag);
                                getSupportFragmentManager().beginTransaction().add(tableRowIDs.get(i), TyreFragment.newInstance(tyreList.get(displayedTyreIndexes.get(i)), shouldBeSelected, new TyreFragment.Widths(0, 0, 0, 0)), newFragTag).commit();
                            }
                        }
                    }
                    lastTopTyreIndex = topTyreIndex;
                }
            }
        });

        // Create sort listeners
        binding.partText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                if (sortCategory == SortCategory.PARTNUMBER) {
                    sortAscending = !sortAscending;
                } else {
                    sortCategory = SortCategory.PARTNUMBER;
                    sortAscending = true;
                }
                UpdateSortColumnArrows(R.id.partText);
                HideSelectBar();
                DisplayTyres();
            }
        });
        binding.lastSoldDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                if (sortCategory == SortCategory.LASTSOLDDATE) {
                    sortAscending = !sortAscending;
                } else {
                    sortCategory = SortCategory.LASTSOLDDATE;
                    sortAscending = true;
                }
                UpdateSortColumnArrows(R.id.lastSoldDateText);
                HideSelectBar();
                DisplayTyres();
            }
        });
        binding.stockText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                if (sortCategory == SortCategory.STOCK) {
                    sortAscending = !sortAscending;
                } else {
                    sortCategory = SortCategory.STOCK;
                    sortAscending = true;
                }
                UpdateSortColumnArrows(R.id.stockText);
                HideSelectBar();
                DisplayTyres();
            }
        });

        // Create search bar listeners
        binding.optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                inOptions = !inOptions;
                if (inOptions) {
                    binding.optionsBar.setVisibility(View.VISIBLE);
                    binding.optionsButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.search_button_bg_tint_disabled)));
                } else {
                    binding.optionsBar.setVisibility(View.GONE);
                    binding.optionsButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.search_button_bg_tint)));
                }
            }
        });
        binding.newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeselectTyre();
                Tyre newTyre = new Tyre(tyreList.size(), "", "", "", "", "", "", true);
                tyreList.add(newTyre);
                // Clear search parameters and re-display tyres to ensure new tyre row is shown
                selectedTyre = newTyre;
                ClearSearch(false, false);
                // Determine what index the new tyre fragment has, and use it to select the new tyre
                String newTyreFragTag = "TyreFrag" + (tyreList.indexOf(newTyre) + tyreIdOffset);
                SelectTyre(newTyre, newTyreFragTag);
                // Open edit page
                inEditMode = true;
                binding.rootLinearLayout.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().add(binding.rootFrame.getId(), TyreEditFragment.newInstance(newTyre), "TyreEditFrag").commit();
            }
        });
        binding.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedTyre != null && selectedTyre.IsAdded()) {
                    tyreList.remove(selectedTyre);
                    DeselectTyre();
                    DisplayTyres();
                }
            }
        });
        binding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterWidth = binding.widthText.getText().toString();
                filterRatio = binding.aspectRatioText.getText().toString();
                filterRim = binding.rimDiameterText.getText().toString();
                filterSearch = binding.searchText.getText().toString();
                DeselectTyre();
                DisplayTyres();
            }
        });
        binding.searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    binding.searchButton.performClick();
                    return true;
                } else {
                    return false;
                }
            }
        });
        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClearSearch(true, true);
            }
        });

        // Create options bar listeners
        binding.optionsBar.setVisibility(View.GONE);
        binding.printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                ClearTyres();
                Print();
            }
        });
        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveEditedTyres();
            }
        });
        binding.saveButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ClearSaveData();
                return true;
            }
        });
        binding.countSeenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Count seen values for all displayed tyres
                int totalCount = 0;
                int uncountableTyres = 0;
                for (int i : displayedTyreIndexes) {
                    try {
                        int count = CountSeenString(tyreList.get(i).GetSeen());
                        totalCount += count;
                    } catch (Exception ignored) {
                        uncountableTyres++;
                    }
                }
                // Display snackbar with counted value
                DisplaySnackbar("Total seen: " + totalCount + (uncountableTyres > 0 ? "  (" + uncountableTyres + " uncountable seen values)" : ""));
            }
        });
        binding.unstockedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUnstocked = !showUnstocked;
                HideTextEditCursor();
                DisplayTyres();
                if (showUnstocked) {
                    binding.unstockedButton.setText(getText(R.string.unstocked_hide));
                } else {
                    binding.unstockedButton.setText(getText(R.string.unstocked_show));
                }
            }
        });

        // Create edit bar listeners
        binding.selectBar.setVisibility(View.GONE);
        binding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch tyre edit fragment
                inEditMode = true;
                binding.rootLinearLayout.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().add(binding.rootFrame.getId(), TyreEditFragment.newInstance(selectedTyre), "TyreEditFrag").commit();
            }
        });
        binding.markDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
                if (selectedTyreFrag != null) {
                    // Tyre row visible on screen, toggle done
                    boolean isDone = selectedTyreFrag.ToggleDone();
                    SetSelectBarButtonStates(isDone, selectedTyre.IsSeenNumber());
                } else {
                    // Tyre row not visible on screen, update Tyre isDone directly
                    if (selectedTyre != null) {
                        selectedTyre.isDone = !selectedTyre.isDone;
                        SetSelectBarButtonStates(selectedTyre.isDone, selectedTyre.IsSeenNumber());
                    }
                }
            }
        });
        binding.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                selectedTyre.EditSeen(true);
                binding.seenSelectBarText.setText(selectedTyre.GetSeen());
                TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
                if (selectedTyreFrag != null) selectedTyreFrag.UpdateText();
            }
        });
        binding.minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                selectedTyre.EditSeen(false);
                binding.seenSelectBarText.setText(selectedTyre.GetSeen());
                TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
                if (selectedTyreFrag != null) selectedTyreFrag.UpdateText();
            }
        });

        // Recover data from saved instance
        if (savedInstanceState != null) {
            filename = savedInstanceState.getString("filename");
            inEditMode = savedInstanceState.getBoolean("inEditMode");
            if (savedInstanceState.getBoolean("inOptions")) {
                binding.optionsButton.performClick();
            }
            if (savedInstanceState.getBoolean("supplierPartCodeExpanded")) {
                binding.supplierPartCodeHandle.performClick();
                SetColumnWidths(R.id.supplierPartCodeText, savedInstanceState.getInt("supplierPartCodeTextWidth"));
            }
            SetColumnWidths(R.id.partText, savedInstanceState.getInt("partTextWidth"));
            SetColumnWidths(R.id.descriptionText, savedInstanceState.getInt("descriptionTextWidth"));
            SetColumnWidths(R.id.locationText, savedInstanceState.getInt("locationTextWidth"));
            selectedTyre = savedInstanceState.getParcelable("selectedTyre");
            if (selectedTyre != null) {
                ShowSelectBar();
            }
            selectedTyreTag = savedInstanceState.getString("selectedTyreTag");
            sortCategory = (SortCategory) savedInstanceState.getSerializable("sortCategory");
            sortAscending = savedInstanceState.getBoolean("sortAscending");
            switch (sortCategory) {
                case PARTNUMBER:
                    UpdateSortColumnArrows(R.id.partText);
                    break;
                case STOCK:
                    UpdateSortColumnArrows(R.id.stockText);
                    break;
                case LASTSOLDDATE:
                    UpdateSortColumnArrows(R.id.lastSoldDateText);
                    break;
            }
            filterWidth = savedInstanceState.getString("filterWidth");
            filterRatio = savedInstanceState.getString("filterRatio");
            filterRim = savedInstanceState.getString("filterRim");
            filterSearch = savedInstanceState.getString("filterSearch");
            showUnstocked = savedInstanceState.getBoolean("showUnstocked");
            if (showUnstocked) {
                binding.unstockedButton.setText(getText(R.string.unstocked_hide));
            } else {
                binding.unstockedButton.setText(getText(R.string.unstocked_show));
            }
        }

        // Get tyre list from container
        tyreList = TyreContainer.getInstance().GetTyreList();
        // Display tyres in table
        if (savedInstanceState != null) {
            DisplayTyres(savedInstanceState.getInt("partTextWidth"), savedInstanceState.getInt("descriptionTextWidth"), savedInstanceState.getInt("locationTextWidth"), savedInstanceState.getInt("supplierPartCodeTextWidth"));
        } else {
            DisplayTyres();
        }
    }

    @Override
    protected void onSaveInstanceState (@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("filename", filename);
        bundle.putBoolean("inEditMode", inEditMode);
        bundle.putBoolean("inOptions", inOptions);
        bundle.putBoolean("supplierPartCodeExpanded", supplierPartCodeExpanded);
        bundle.putInt("partTextWidth", binding.categoryRow.findViewById(R.id.partText).getWidth());
        bundle.putInt("descriptionTextWidth", binding.categoryRow.findViewById(R.id.descriptionText).getWidth());
        bundle.putInt("locationTextWidth", binding.categoryRow.findViewById(R.id.locationText).getWidth());
        bundle.putInt("supplierPartCodeTextWidth", binding.categoryRow.findViewById(R.id.supplierPartCodeText).getWidth());
        bundle.putParcelable("selectedTyre", selectedTyre);
        bundle.putString("selectedTyreTag", selectedTyreTag);
        bundle.putSerializable("sortCategory", sortCategory);
        bundle.putBoolean("sortAscending", sortAscending);
        bundle.putString("filterWidth", filterWidth);
        bundle.putString("filterRatio", filterRatio);
        bundle.putString("filterRim", filterRim);
        bundle.putString("filterSearch", filterSearch);
        bundle.putBoolean("showUnstocked", showUnstocked);
    }

    /**
     * Recreates tyre table and applies search+sort parameters
     */
    private void DisplayTyres(int pWidth, int dWidth, int lWidth, int sWidth) {
        int tyreId = tyreIdOffset;
        // Clear table
        ClearTyres();
        // Sort tyre list
        Comparator<Tyre> comparator = null;
        switch (sortCategory) {
            case PARTNUMBER: {
                comparator = new Tyre.SortByPartNumber();
                break; }
            case LASTSOLDDATE: {
                comparator = new Tyre.SortByLastSoldDate();
                break; }
            case STOCK: {
                comparator = new Tyre.SortByStock();
                break; }
        }
        tyreList.sort(sortAscending ? comparator : comparator.reversed());
        // Iterate over each tyre, adding to display if it passes the filter
        for (int i = 0; i < tyreList.size(); i++) {
            // Filter tyres
            if (FilterTyre(tyreList.get(i))) {
                // Add a new tyre fragment to display table, as a child of a new table row
                TableRow newRow = new TableRow(getApplicationContext());
                newRow.setMinimumHeight(101);
                int newId = View.generateViewId();
                tableRowIDs.add(newId);
                newRow.setId(newId);
                if (rowCount < maxDisplayedTyres) {
                    getSupportFragmentManager().beginTransaction().add(newId, TyreFragment.newInstance(
                            tyreList.get(i),
                            selectedTyre != null && selectedTyre.equals(tyreList.get(i)),
                            new TyreFragment.Widths(pWidth, dWidth, lWidth, sWidth)), "TyreFrag" + tyreId).commit();
                }
                binding.tyreTable.addView(newRow);
                tyreId++;
                rowCount++;
                displayedTyreIndexes.add(i);
            }
        }
        HideTextEditCursor();
    }
    private void DisplayTyres() {
        DisplayTyres(0, 0, 0, 0);
    }

    private void ClearTyres() {
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag.getClass() == TyreFragment.class) {
                getSupportFragmentManager().beginTransaction().remove(frag).commit();
            }
        }
        binding.tyreTable.removeViews(1, rowCount);
        tableRowIDs.clear();
        rowCount = 0;
        displayedTyreIndexes.clear();
        lastTopTyreIndex = 0;
        binding.verticalScroll.setScrollY(0);
        binding.verticalScroll.fling(0);
    }

    public void SelectTyre(Tyre tyre, String fragmentTag) {
        HideTextEditCursor();
        if (selectedTyre != null) {
            TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
            if (selectedTyreFrag != null) selectedTyreFrag.SetUnselected();
        }
        selectedTyre = tyre;
        selectedTyreTag = fragmentTag;
        ShowSelectBar();
    }

    private void DeselectTyre() {
        HideTextEditCursor();
        binding.newButton.setVisibility(View.VISIBLE);
        binding.removeButton.setVisibility(View.GONE);
        if (selectedTyre != null) {
            TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
            if (selectedTyreFrag != null) selectedTyreFrag.SetUnselected();
        }
        HideSelectBar();
    }

    public void HideSelectBar() {
        binding.selectBar.setVisibility(View.GONE);
        binding.newButton.setVisibility(View.VISIBLE);
        binding.removeButton.setVisibility(View.GONE);
        selectedTyre = null;
        selectedTyreTag = null;
    }

    public void ShowSelectBar() {
        if (selectedTyre != null) {
            SetSelectBarButtonStates(selectedTyre.isDone, selectedTyre.IsSeenNumber());
            binding.seenSelectBarText.setText(selectedTyre.GetSeen());
            binding.stockSelectBarText.setText(selectedTyre.GetStock());
            binding.selectBar.setVisibility(View.VISIBLE);
            if (selectedTyre.IsAdded()) {
                binding.newButton.setVisibility(View.GONE);
                binding.removeButton.setVisibility(View.VISIBLE);
            } else {
                binding.newButton.setVisibility(View.VISIBLE);
                binding.removeButton.setVisibility(View.GONE);
            }
        }
    }

    public void CloseEditor() {
        Fragment tyreEditFragment = getSupportFragmentManager().findFragmentByTag("TyreEditFrag");
        if (tyreEditFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(tyreEditFragment).commit();
            binding.rootLinearLayout.setVisibility(View.VISIBLE);
        }
        TyreFragment selectedTyreFrag = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag));
        if (selectedTyreFrag != null) selectedTyreFrag.UpdateText();
        if (selectedTyre != null) {
            binding.seenSelectBarText.setText(selectedTyre.GetSeen());
            SetSelectBarButtonStates(selectedTyre.isDone, selectedTyre.IsSeenNumber());
        }
        inEditMode = false;
        HideTextEditCursor();
    }

    /**
     * Checks whether a specific tyre passes the current filter parameters
     * @param tyre Tyre to check
     * @return Whether the tyre passes
     */
    private boolean FilterTyre(Tyre tyre) {
        String partNumber = tyre.GetPartNumber();
        // Filter by stock count
        if (!showUnstocked && filterWidth.isEmpty() && filterRatio.isEmpty() && filterRim.isEmpty() && filterSearch.isEmpty()) {
            try {
                if (Integer.parseInt(tyre.GetStock()) <= 0) {
                    return false;
                }
            } catch (NumberFormatException ignored) {}
        }
        // Ensure partNumber is valid
        if (partNumber.length() == 7) {
            // Filter by tyre sizes
            if (!filterWidth.isEmpty() && !partNumber.startsWith(filterWidth)) {
                return false;
            }
            if (!filterRatio.isEmpty() && !partNumber.substring(3).startsWith(filterRatio)) {
                return false;
            }
            if (!filterRim.isEmpty() && !partNumber.substring(5).startsWith(filterRim)) {
                return false;
            }
        } else {
            if (!filterWidth.isEmpty() || !filterRatio.isEmpty() || !filterRim.isEmpty()) {
                return false;
            }
        }
        // Filter by search
        return filterSearch.isEmpty() ||
                tyre.GetPart(true, null).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetSupplierPartCode(true, null).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetDescription(true, null).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetLocation(true, null).toLowerCase().contains(filterSearch.toLowerCase());
    }

    private void ClearSearch(boolean showKeyboard, boolean deselect) {
        binding.widthText.setText("");
        binding.aspectRatioText.setText("");
        binding.rimDiameterText.setText("");
        binding.searchText.setText("");
        filterWidth = "";
        filterRatio = "";
        filterRim = "";
        filterSearch = "";
        if (deselect) DeselectTyre();
        DisplayTyres();
        if(showKeyboard && binding.widthText.requestFocus()){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.widthText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void UpdateSortColumnArrows(int sortTextId) {
        String arrow = getString(sortAscending ? R.string.sort_arrow_up : R.string.sort_arrow_down);
        binding.lastSoldDateText.setText(getString(R.string.category_last_sold_arrow, sortTextId == R.id.lastSoldDateText ? arrow : ""));
        binding.stockText.setText(getString(R.string.category_stock_arrow, sortTextId == R.id.stockText ? arrow : ""));
        binding.partText.setText(getString(R.string.category_part_arrow, sortTextId == R.id.partText ? arrow : ""));
    }

    private void Print() {
        // Create webview to layout html for printing
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                SendPrintJob(view);
            }
        });
        // Generate html string to visualise tyre table
        StringBuilder htmlTable = new StringBuilder("<head><style>@page{margin:20px;margin-bottom:35px;@bottom-center{content:counter(page)\" of \"counter(pages);margin-bottom:30px;}}" +
                "table{page-break-after:auto;}tr{page-break-inside:avoid;page-break-after:auto}td{page-break-inside:avoid;page-break-after:auto}" +
                "table,th,td{border:0.1pt solid DarkGray;border-collapse:collapse;font-size:12px;color:DimGrey;}" +
                "th{text-align:left;}th,td{padding:1px;}u{text-decoration-color:#007700;}" +
                "s{text-decoration-color:#770000;}</style></head><body>");
        htmlTable.append("<h3>").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</h3>");
        htmlTable.append("<table><tr><th></th><th>Part</th><th style=\"width:20%\">Supplier Part Code</th><th>Description</th><th>Location</th><th>Stock</th><th>Seen</th><th>Last Sold</th></tr>");
        tyreList.sort(new Tyre.SortByPartNumber());
        for (Tyre tyre : tyreList) {
            int stockNum;
            try {
                stockNum = Integer.parseInt(tyre.GetStock());
            } catch (Exception e) {
                stockNum = -1;
            }
            boolean seenMatchesStock = false;
            if (tyre.GetSeen().equals(tyre.GetStock())) {
                seenMatchesStock = true;
            } else {
                try {
                    int seenInt = CountSeenString(tyre.GetSeen());
                    if (seenInt == Integer.parseInt(tyre.GetStock())) seenMatchesStock = true;
                } catch (Exception ignored) {}
            }
            if (tyre.IsEdited() || !seenMatchesStock || stockNum > 0) {
                htmlTable.append("<tr><td style=\"color:Black;width:1%;\">")
                        .append(tyre.IsAdded() ? "✚" : (tyre.IsEdited() || !seenMatchesStock) ? "●" : "")
                        .append("</td><td>")
                        .append(tyre.GetPart(false, getApplicationContext()))
                        .append("</td><td>")
                        .append(tyre.GetSupplierPartCode(false, getApplicationContext()))
                        .append("</td><td>")
                        .append(tyre.GetDescription(false, getApplicationContext()))
                        .append("</td><td>")
                        .append(tyre.GetLocation(false, getApplicationContext()))
                        .append("</td><td style=\"text-align:center;" + (!seenMatchesStock ? "color:#770000;" : "") + "\">" + (!seenMatchesStock ? "<b>" : ""))
                        .append(tyre.GetStock())
                        .append((!seenMatchesStock ? "</b>" : "") + "</td><td style=\"text-align:center;" + (!seenMatchesStock ? "color:#770000;" : "") + "\">" + (!seenMatchesStock ? "<b>" : ""))
                        .append(tyre.GetSeen())
                        .append((!seenMatchesStock ? "</b>" : "") + "</td><td>")
                        .append(tyre.GetLastSoldDate(false))
                        .append("</tr>");
                if (!tyre.GetComment(true).isBlank()) htmlTable.append("<tr><td colspan=\"8\">▲ ")
                        .append(tyre.GetComment(false))
                        .append("</td></tr>");
            }
        }
        htmlTable.append("</table></body>");
        // Pass html string into webview
        webView.loadDataWithBaseURL(null, htmlTable.toString(), "text/HTML", "UTF-8", null);
    }

    private void SendPrintJob(WebView webView) {
        String fileName = "StockCheck_" + LocalDate.now();
        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        // Get a print adapter instance
        PrintDocumentAdapter printAdapter = new PrintDocumentAdapterWrapper(webView.createPrintDocumentAdapter(fileName), this::DisplayTyres);
        // Create a print job with name and adapter instance
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
        builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        printManager.print(fileName, printAdapter, builder.build());
    }

    private void SaveEditedTyres() {
        try {
            boolean tyresSaved = false;
            ArrayList<StoredTyre> tyresToStore = new ArrayList<>();
            for (Tyre tyre : tyreList) {
                if (tyre.IsEdited() || !tyre.GetSeen().equals("0") || tyre.isDone || tyre.IsAdded()) {
                    tyresToStore.add(tyre.ToStoredTyre());
                    tyresSaved = true;
                }
            }
            final boolean finalTyresSaved = tyresSaved;
            MetaData metaData = new MetaData();
            metaData.savedFilename = filename;
            metaData.savedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
            TyreDatabase database = TyreDatabase.getDatabase(getApplicationContext());
            TyreDatabase.databaseWriteExecutor.execute(() -> {
                if (finalTyresSaved) {
                    database.storedTyreDao().Clear();
                    database.metaDataDao().Clear();
                    database.storedTyreDao().Insert(tyresToStore);
                    database.metaDataDao().Insert(metaData);
                }
                this.runOnUiThread(() -> {
                    DisplaySnackbar(finalTyresSaved ? getString(R.string.save_successful) : getString(R.string.save_nothing_to_save));
                });
            });
        } catch (Exception ignored) {
            DisplaySnackbar(getString(R.string.save_error));
        }
    }

    private void ClearSaveData() {
        try {
            TyreDatabase database = TyreDatabase.getDatabase(getApplicationContext());
            TyreDatabase.databaseWriteExecutor.execute(() -> {
                MetaData metaData = database.metaDataDao().Get();
                database.storedTyreDao().Clear();
                database.metaDataDao().Clear();
                this.runOnUiThread(() -> {
                    DisplaySnackbar(metaData == null ? getString(R.string.save_nothing_to_delete) : getString(R.string.save_delete_successful));
                });
            });
        } catch (Exception ignored) {
            DisplaySnackbar(getString(R.string.save_delete_error));
        }
    }

    public class HandleTouchListener implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            v.getParent().getParent().getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
            v.getParent().getParent().getParent().getParent().getParent().getParent().getParent().requestDisallowInterceptTouchEvent(true);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragStartPosX = (int)event.getRawX();
                    startDragWidth = binding.categoryRow.findViewById(HandleToTextId(v.getId())).getWidth();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int widthDelta = (int)event.getRawX() - dragStartPosX;
                    SetColumnWidths(HandleToTextId(v.getId()), startDragWidth + widthDelta);
                    break;
            }
            return true;
        }
    }

    private void SetColumnWidths(int textId, int newWidth) {
        int clampedWidth = Math.clamp(newWidth,
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()),
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 450, getResources().getDisplayMetrics()));
        int clampedWidthPlusHandle = clampedWidth + (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        View categoryText = binding.categoryRow.findViewById(textId);
        categoryText.getLayoutParams().width = clampedWidth;
        categoryText.requestLayout();
        for (int i = lastTopTyreIndex; i < Math.min(lastTopTyreIndex + maxDisplayedTyres, rowCount); i++) {
            View tyreRow = this.findViewById(tableRowIDs.get(i));
            if (tyreRow != null) {
                View tyreRowText = tyreRow.findViewById(textId);
                if (tyreRowText != null) {
                    tyreRowText.getLayoutParams().width = clampedWidthPlusHandle;
                    tyreRowText.requestLayout();
                }
            }
        }
    }

    private int HandleToTextId(int handleId) {
        if (handleId == R.id.partHandle) {
            return R.id.partText;
        } else if (handleId == R.id.supplierPartCodeHandle) {
            return R.id.supplierPartCodeText;
        } else if (handleId == R.id.descriptionHandle) {
            return R.id.descriptionText;
        }  else {
            return R.id.locationText;
        }
    }

    private void SetSelectBarButtonStates(boolean isDone, boolean seenIsNumber) {
        Context c = getApplicationContext();
        boolean canModifySeen = !isDone && seenIsNumber;
        int seenColourId = canModifySeen ? R.color.search_button_bg_tint : R.color.search_button_bg_tint_disabled;
        if (c != null) {
            binding.editButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c,
                    isDone ? R.color.search_button_bg_tint_disabled : R.color.search_button_bg_tint)));
            binding.addButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, seenColourId)));
            binding.minusButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, seenColourId)));
        }
        binding.editButton.setEnabled(!isDone);
        binding.addButton.setEnabled(canModifySeen);
        binding.minusButton.setEnabled(canModifySeen);
        binding.markDoneButton.setText(isDone ? R.string.select_unmark : R.string.select_mark);
    }

    private void HideTextEditCursor() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        binding.widthText.clearFocus();
        binding.aspectRatioText.clearFocus();
        binding.rimDiameterText.clearFocus();
        binding.searchText.clearFocus();
    }

    private static int CountSeenString(String seen) throws Exception {
        int count = 0;
        try {
            if (seen.contains("/")) {
                String[] splits = seen.split("/");
                for (String s : splits) {
                    count += Integer.parseUnsignedInt(s);
                }
            } else if (seen.contains("\\")) {
                String[] splits = seen.split("\\\\");
                for (String s : splits) {
                    count += Integer.parseUnsignedInt(s);
                }
            } else {
                count = Integer.parseUnsignedInt(seen);
            }
            return count;
        } catch (Exception ignored) {
            throw new Exception("Not an integer, or integers separated by slashes");
        }
    }

    private void DisplaySnackbar(String message) {
        Snackbar snackbar = Snackbar.make(
                binding.rootFrame,
                message,
                Snackbar.LENGTH_LONG);
        ((TextView)snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text)).setTextSize(18);
        snackbar.show();
    }
}
