package com.example.stockcheck;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment that displays tyre information as a selectable table row
 * Use the {@link TyreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TyreFragment extends Fragment {

    private TyreListActivity activity;
    private Tyre tyre;
    private boolean startSelected;
    private boolean isSelected = false;

    public TyreFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of the fragment, with specified tyre to display.
     * @param tyre Tyre to display
     * @return A new instance of fragment TyreFragment.
     */
    public static TyreFragment newInstance(Tyre tyre, boolean startSelected) {
        TyreFragment fragment = new TyreFragment();
        Bundle args = new Bundle();
        args.putParcelable("Tyre", tyre);
        args.putBoolean("startSelected", startSelected);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Set the tyre as unselected and update its view.
     */
    public void SetUnselected() {
        isSelected = false;
        if (tyre.isDone) {
            SetBackgroundColour(getView(), R.color.column_bg_done);
        } else {
            SetBackgroundColour(getView(), R.color.column_bg);
        }
    }

    /**
     * Toggle the fragment's 'done' state
     * @return The new 'done' state
     */
    public boolean ToggleDone() {
        tyre.isDone = !tyre.isDone;
        if (tyre.isDone) {
            SetBackgroundColour(getView(), R.color.column_bg_done_selected);
        } else {
            SetBackgroundColour(getView(), R.color.column_bg_selected);
        }
        return tyre.isDone;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tyre = getArguments().getParcelable("Tyre");
            startSelected = getArguments().getBoolean("startSelected");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tyre, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UpdateText();
        if (startSelected) {
            SetBackgroundColour(view, tyre.isDone ? R.color.column_bg_done_selected : R.color.column_bg_selected);
            isSelected = true;
        } else {
            SetBackgroundColour(view, tyre.isDone ? R.color.column_bg_done : R.color.column_bg);
        }
        view.setOnClickListener(new ClickListener());
        // Set column widths to match category headers
        SetCategoryWidth(R.id.partText, view);
        SetCategoryWidth(R.id.supplierPartCodeText, view);
        SetCategoryWidth(R.id.descriptionText, view);
        SetCategoryWidth(R.id.locationText, view);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TyreListActivity) {
            activity = (TyreListActivity) context;
        }
    }

    private void SetCategoryWidth(int id, View v) {
        int width = activity.findViewById(id).getWidth();
        if (width != 0) {
            int widthPlusHandle = width + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
            v.findViewById(id).getLayoutParams().width = widthPlusHandle;
            v.findViewById(id).requestLayout();
        }
    }

    public class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!isSelected) {
                isSelected = true;
                if (tyre.isDone) {
                    SetBackgroundColour(view, R.color.column_bg_done_selected);
                } else {
                    SetBackgroundColour(view, R.color.column_bg_selected);
                }
                activity.SelectTyre(tyre, getTag());
            } else {
                SetUnselected();
                activity.HideSelectBar();
            }
        }
    }

    /**
     * Updates all displayed text.
     */
    public void UpdateText() {
        View view = getView();
        TextView partText = (TextView) view.findViewById(R.id.partText);
        partText.setText(Html.fromHtml(tyre.GetPart(false), Html.FROM_HTML_MODE_COMPACT));
        TextView supplierPartCodeText = (TextView) view.findViewById(R.id.supplierPartCodeText);
        supplierPartCodeText.setText(Html.fromHtml(tyre.GetSupplierPartCode(false), Html.FROM_HTML_MODE_COMPACT));
        TextView descriptionText = (TextView) view.findViewById(R.id.descriptionText);
        descriptionText.setText(Html.fromHtml(tyre.GetDescription(false), Html.FROM_HTML_MODE_COMPACT));
        TextView locationText = (TextView) view.findViewById(R.id.locationText);
        locationText.setText(Html.fromHtml(tyre.GetLocation(false), Html.FROM_HTML_MODE_COMPACT));
        TextView stockText = (TextView) view.findViewById(R.id.stockText);
        stockText.setText(tyre.GetStock());
        TextView lastSoldDateText = (TextView) view.findViewById(R.id.lastSoldDateText);
        lastSoldDateText.setText(Html.fromHtml(tyre.GetLastSoldDate(false), Html.FROM_HTML_MODE_COMPACT));
    }

    /**
     * Sets the background colour of all text views
     * @param v Root view object
     * @param colourId Id of the colour to use
     */
    private void SetBackgroundColour(View v, int colourId) {
        Context c = getContext();
        if (c != null) {
            v.findViewById(R.id.partText).setBackgroundColor(ContextCompat.getColor(c, colourId));
            v.findViewById(R.id.supplierPartCodeText).setBackgroundColor(ContextCompat.getColor(c, colourId));
            v.findViewById(R.id.descriptionText).setBackgroundColor(ContextCompat.getColor(c, colourId));
            v.findViewById(R.id.locationText).setBackgroundColor(ContextCompat.getColor(c, colourId));
            v.findViewById(R.id.stockText).setBackgroundColor(ContextCompat.getColor(c, colourId));
            v.findViewById(R.id.lastSoldDateText).setBackgroundColor(ContextCompat.getColor(c, colourId));
        }
    }


}