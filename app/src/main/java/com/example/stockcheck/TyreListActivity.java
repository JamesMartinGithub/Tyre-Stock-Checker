package com.example.stockcheck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TableRow;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.stockcheck.databinding.ActivityTyreListBinding;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Activity that displays tyres and allows searching, sorting, and selecting.
 */
public class TyreListActivity extends AppCompatActivity {

    private enum SortCategory { PARTNUMBER, LASTSOLDDATE, STOCK }

    private ActivityTyreListBinding binding;
    private ArrayList<Tyre> tyreList;
    private ArrayList<Integer> displayedTyreIndexes = new ArrayList<>();
    private int lastTopTyreIndex = 0;
    private int rowCount = 0;
    private boolean inEditMode = false;
    private boolean inOptions = false;
    int dragStartPosX = 0;
    int startDragWidth = 0;
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

        binding = ActivityTyreListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (inEditMode) {
                    CloseEditor();
                } else {
                    DeselectTyre();
                }
            }
        });

        // Create handle listeners
        HandleTouchListener handleTouchListener = new HandleTouchListener();
        binding.partHandle.setOnTouchListener(handleTouchListener);
        binding.supplierPartCodeHandle.setOnTouchListener(handleTouchListener);
        binding.descriptionHandle.setOnTouchListener(handleTouchListener);
        binding.locationHandle.setOnTouchListener(handleTouchListener);

        // Create scroll listener that creates TyreFragments as it scrolls and deletes ones outside of view
        binding.verticalScroll.SetOnScrollCallback(new ScrollViewNotifying.OnScrollCallback() {
            @Override
            public void OnScrollChanged(int x, int y, int oldx, int oldy) {
                // Subtract options and select tabs from scrollY if visible
                int maxNewY = y - (inOptions ? 158 : 0) - (selectedTyre != null ? 158 : 0);
                // Cap minimum to category row + 1.5 tyre rows (posY at which first tyre row can be culled)
                maxNewY = Math.max(maxNewY, 271);
                // Divide y by tyre row height to get number of tyres scrolled by (= no. of tyre rows to remove and add)
                int topTyreIndex = Math.abs(maxNewY - 271) / 101;
                if (topTyreIndex != lastTopTyreIndex) {
                    if (topTyreIndex > lastTopTyreIndex) {
                        // Scrolled down
                        for (int i = lastTopTyreIndex; i < topTyreIndex; i++) {
                            // Remove previous tyre fragment
                            Fragment tyreFragment = getSupportFragmentManager().findFragmentByTag("TyreFrag" + (i + 10));
                            if (tyreFragment != null) {
                                getSupportFragmentManager().beginTransaction().remove(tyreFragment).commit();
                            }
                            // Add new tyre fragment
                            if (rowCount > i + 20) {
                                boolean shouldBeSelected = false;
                                if (selectedTyreTag != null && selectedTyreTag.equals("TyreFrag" + (i + 30))) shouldBeSelected = true;
                                getSupportFragmentManager().beginTransaction().add(i + 30, TyreFragment.newInstance(tyreList.get(displayedTyreIndexes.get(i + 20)), shouldBeSelected), "TyreFrag" + (i + 30)).commit();

                            }
                        }
                    } else {
                        // Scrolled up
                        for (int i = topTyreIndex; i < lastTopTyreIndex; i++) {
                            // Remove previous tyre fragment
                            Fragment tyreFragment = getSupportFragmentManager().findFragmentByTag("TyreFrag" + (i + 30));
                            if (tyreFragment != null) {
                                getSupportFragmentManager().beginTransaction().remove(tyreFragment).commit();
                            }
                            // Add new tyre fragment
                            if (rowCount > i) {
                                boolean shouldBeSelected = false;
                                if (selectedTyreTag != null && selectedTyreTag.equals("TyreFrag" + (i + 10))) shouldBeSelected = true;
                                getSupportFragmentManager().beginTransaction().add(i + 10, TyreFragment.newInstance(tyreList.get(displayedTyreIndexes.get(i)), shouldBeSelected), "TyreFrag" + (i + 10)).commit();
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
                    binding.lastSoldDateText.setText("Last Sold");
                    binding.stockText.setText("Stock");
                }
                binding.partText.setText(sortAscending ? "Part▲" :  "Part▼");
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
                    binding.partText.setText("Part");
                    binding.stockText.setText("Stock");
                }
                binding.lastSoldDateText.setText(sortAscending ? "Last Sold▲" :  "Last Sold▼");
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
                    binding.partText.setText("Part");
                    binding.lastSoldDateText.setText("Last Sold");
                }
                binding.stockText.setText(sortAscending ? "Stock▲" :  "Stock▼");
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
                Tyre newTyre = new Tyre("", "", "", "", "", "");
                tyreList.add(newTyre);
                // Add a new tyre fragment to display table, as a child of a new table row
                int newRowId = rowCount + 10;
                TableRow newRow = new TableRow(getApplicationContext());
                newRow.setId(newRowId);
                getSupportFragmentManager().beginTransaction().add(newRowId, TyreFragment.newInstance(newTyre, true), "TyreFrag" + newRowId).commit();
                binding.tyreTable.addView(newRow);
                rowCount++;
                // Set new tyre fragment as selected
                SelectTyre(newTyre, "TyreFrag" + newRowId);
                // Launch tyre edit fragment
                inEditMode = true;
                binding.rootLinearLayout.setVisibility(View.GONE);
                getSupportFragmentManager().beginTransaction().add(binding.rootFrame.getId(), TyreEditFragment.newInstance(newTyre), "TyreEditFrag").commit();
            }
        });
        binding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                filterWidth = binding.widthText.getText().toString();
                filterRatio = binding.aspectRatioText.getText().toString();
                filterRim = binding.rimDiameterText.getText().toString();
                filterSearch = binding.searchText.getText().toString();
                HideSelectBar();
                DisplayTyres();
            }
        });
        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.widthText.setText("");
                binding.aspectRatioText.setText("");
                binding.rimDiameterText.setText("");
                binding.searchText.setText("");
                filterWidth = "";
                filterRatio = "";
                filterRim = "";
                filterSearch = "";
                HideSelectBar();
                DisplayTyres();
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
        binding.unstockedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUnstocked = !showUnstocked;
                HideTextEditCursor();
                DisplayTyres();
                if (showUnstocked) {
                    binding.unstockedButton.setText("Hide 0 Stocked");
                } else {
                    binding.unstockedButton.setText("Show All");
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
                boolean isDone = ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).ToggleDone();
                SetSelectBarButtonStates(isDone, selectedTyre.IsSeenNumber());
            }
        });
        binding.addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                selectedTyre.EditSeen(true);
                binding.seenValueText.setText(selectedTyre.GetSeen());
                ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).UpdateText();
            }
        });
        binding.minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HideTextEditCursor();
                selectedTyre.EditSeen(false);
                binding.seenValueText.setText(selectedTyre.GetSeen());
                ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).UpdateText();
            }
        });

        // Get tyre list from container
        tyreList = TyreContainer.getInstance().GetTyreList();
        // Display tyres in table
        DisplayTyres();
    }

    //@Override
    //public void onResume() {
    //    super.onResume();
    //    DisplayTyres();
    //}

    //@Override
    //public void onPause() {
    //    super.onPause();
    //    ClearTyres();
    //}

    /**
     * Recreates tyre table and applies search+sort parameters
     */
    private void DisplayTyres() {
        int tyreId = 10;
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
                newRow.setId(tyreId);
                if (rowCount < 20) {
                    getSupportFragmentManager().beginTransaction().add(tyreId, TyreFragment.newInstance(tyreList.get(i), false), "TyreFrag" + tyreId).commit();
                }
                binding.tyreTable.addView(newRow);
                tyreId++;
                rowCount++;
                displayedTyreIndexes.add(i);
            }
        }
        for (Tyre tyre : tyreList) {

        }
        HideTextEditCursor();
    }

    private void ClearTyres() {
        // Clear table rows
        if (rowCount > 0) {
            for (int i = lastTopTyreIndex; i < Math.min(lastTopTyreIndex + 20, rowCount); i++) {
                Fragment tyreFragment = getSupportFragmentManager().findFragmentByTag("TyreFrag" + (i + 10));
                if (tyreFragment != null) {
                    getSupportFragmentManager().beginTransaction().remove(tyreFragment).commit();
                }
            }
            binding.tyreTable.removeViews(1, rowCount);
            rowCount = 0;
            displayedTyreIndexes.clear();
            lastTopTyreIndex = 0;
            binding.verticalScroll.setScrollY(0);
        }
    }

    public void SelectTyre(Tyre tyre, String fragmentTag) {
        HideTextEditCursor();
        if (selectedTyre != null) {
            ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).SetUnselected();
        }
        selectedTyre = tyre;
        selectedTyreTag = fragmentTag;
        binding.seenValueText.setText(selectedTyre.GetSeen());
        SetSelectBarButtonStates(selectedTyre.isDone, selectedTyre.IsSeenNumber());
        binding.selectBar.setVisibility(View.VISIBLE);
    }

    private void DeselectTyre() {
        HideTextEditCursor();
        if (selectedTyre != null) {
            ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).SetUnselected();
        }
        HideSelectBar();
    }

    public void HideSelectBar() {
        binding.selectBar.setVisibility(View.GONE);
        selectedTyre = null;
        selectedTyreTag = null;
    }

    public void CloseEditor() {
        Fragment tyreEditFragment = getSupportFragmentManager().findFragmentByTag("TyreEditFrag");
        if (tyreEditFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(tyreEditFragment).commit();
            binding.rootLinearLayout.setVisibility(View.VISIBLE);
        }
        ((TyreFragment)getSupportFragmentManager().findFragmentByTag(selectedTyreTag)).UpdateText();
        binding.seenValueText.setText(selectedTyre.GetSeen());
        SetSelectBarButtonStates(selectedTyre.isDone, selectedTyre.IsSeenNumber());
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
                tyre.GetPart(true).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetSupplierPartCode(true).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetDescription(true).toLowerCase().contains(filterSearch.toLowerCase()) ||
                tyre.GetLocation(true).toLowerCase().contains(filterSearch.toLowerCase());
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
        StringBuilder htmlTable = new StringBuilder("<head><style>@page{margin:20px}table,th,td{border:0.1pt solid DarkGray;border-collapse:collapse;font-size:12px;color:DimGrey;}th{text-align:left;}th,td{padding:1px;}</style></head><body>");
        htmlTable.append("<h3>").append(LocalDate.now().toString().replace('-', '/')).append("</h3>");
        htmlTable.append("<table><tr><th></th><th>Part</th><th style=\"width:20%\">Supplier Part Code</th><th>Description</th><th>Location</th><th>Stock</th><th>Seen</th><th>Last Sold</th></tr>");
        tyreList.sort(new Tyre.SortByPartNumber());
        for (Tyre tyre : tyreList) {
            int stockNum;
            try {
                stockNum = Integer.parseInt(tyre.GetStock());
            } catch (Exception e) {
                stockNum = 0;
            }
            if (tyre.IsEdited() || stockNum > 0) {
                htmlTable.append("<tr><td style=\"color:Black;width:1%;\">")
                        .append(tyre.IsEdited() ? "●" : "")
                        .append("</td><td>")
                        .append(tyre.GetPart(false))
                        .append("</td><td>")
                        .append(tyre.GetSupplierPartCode(false))
                        .append("</td><td>")
                        .append(tyre.GetDescription(false))
                        .append("</td><td>")
                        .append(tyre.GetLocation(false))
                        .append("</td><td style=\"text-align:center;\">")
                        .append(tyre.GetStock())
                        .append("</td><td style=\"text-align:center;\">")
                        .append(tyre.GetSeen())
                        .append("</td><td>")
                        .append(tyre.GetLastSoldDate(false))
                        .append("</tr>");
                if (!tyre.GetComment(true).isEmpty()) htmlTable.append("<tr><td colspan=\"7\">▲ ")
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
        PrintDocumentAdapter printAdapter = new PrintDocumentAdapterWrapper(webView.createPrintDocumentAdapter(fileName), new PrintDocumentAdapterWrapper.OnFinishListener() {
            @Override
            public void OnFinish() {
                DisplayTyres();
            }
        });
        // Create a print job with name and adapter instance
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
        builder.setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME);
        //builder.setMinMargins(new PrintAttributes.Margins(60, 60, 60, 60));
        printManager.print(fileName, printAdapter, builder.build());
    }

    public class HandleTouchListener implements View.OnTouchListener {
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
                    int widthDelta = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, (int)event.getRawX() - dragStartPosX, getResources().getDisplayMetrics());
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
        for (int i = lastTopTyreIndex; i < Math.min(lastTopTyreIndex + 20, rowCount); i++) {
            View tyreRow = this.findViewById(i + 10);
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
        }  else if (handleId == R.id.descriptionHandle) {
            return R.id.descriptionText;
        }  else if (handleId == R.id.locationHandle) {
            return R.id.locationText;
        }
        return -1;
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
        binding.markDoneButton.setText(isDone ? "Unmark" : "Mark Done");
    }

    private void HideTextEditCursor() {
        binding.widthText.clearFocus();
        binding.aspectRatioText.clearFocus();
        binding.rimDiameterText.clearFocus();
        binding.searchText.clearFocus();
    }
}