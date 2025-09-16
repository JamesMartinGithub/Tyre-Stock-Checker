package com.example.stockcheck;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.stockcheck.model.Tyre;

/**
 * Fragment allowing editing of tyre data.
 * Use the {@link TyreEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TyreEditFragment extends Fragment {

    private TyreListActivity activity;
    private Tyre tyre;

    public TyreEditFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance, with a specified tyre to edit.
     * @param tyre The tyre to edit.
     * @return A new instance of fragment TyreEditFragment.
     */
    public static TyreEditFragment newInstance(Tyre tyre) {
        TyreEditFragment fragment = new TyreEditFragment();
        Bundle args = new Bundle();
        args.putParcelable("Tyre", tyre);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tyre = getArguments().getParcelable("Tyre");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tyre_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise text edits with text from tyre
        TextView partText = (TextView) view.findViewById(R.id.partText);
        partText.setText(tyre.GetPart(true, null));
        TextView supplierPartCodeText = (TextView) view.findViewById(R.id.supplierPartCodeText);
        supplierPartCodeText.setText(tyre.GetSupplierPartCode(true, getContext()));
        TextView descriptionText = (TextView) view.findViewById(R.id.descriptionText);
        descriptionText.setText(tyre.GetDescription(true, null));
        TextView locationText = (TextView) view.findViewById(R.id.locationText);
        locationText.setText(tyre.GetLocation(true, null));
        TextView seenText = (TextView) view.findViewById(R.id.seenText);
        seenText.setText(tyre.GetSeen());
        TextView stockText = (TextView) view.findViewById(R.id.stockText);
        stockText.setText(tyre.GetStock());
        TextView lastSoldDateText = (TextView) view.findViewById(R.id.lastSoldDateText);
        lastSoldDateText.setText(tyre.GetLastSoldDate(true));
        TextView commentText = (TextView) view.findViewById(R.id.commentText);
        commentText.setText(tyre.GetComment(true));

        // Add back button listener
        view.findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.CloseEditor();
            }
        });

        // Assign text edit listeners
        partText.addTextChangedListener(new EditTextListener(0));
        supplierPartCodeText.addTextChangedListener(new EditTextListener(1));
        descriptionText.addTextChangedListener(new EditTextListener(2));
        locationText.addTextChangedListener(new EditTextListener(3));
        seenText.addTextChangedListener(new EditTextListener(4));
        lastSoldDateText.addTextChangedListener(new EditTextListener(5));
        commentText.addTextChangedListener(new EditTextListener(6));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TyreListActivity) {
            activity = (TyreListActivity) context;
        }
    }

    public class EditTextListener implements TextWatcher {
        private final int id;
        public EditTextListener(int id) {
            this.id = id;
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            switch (id) {
                case 0:
                    tyre.EditPart(s.toString());
                    break;
                case 1:
                    tyre.EditSupplierPartCode(s.toString());
                    break;
                case 2:
                    tyre.EditDescription(s.toString());
                    break;
                case 3:
                    tyre.EditLocation(s.toString());
                    break;
                case 4:
                    tyre.EditSeen(s.toString());
                    break;
                case 5:
                    tyre.EditLastSoldDate(s.toString());
                    break;
                case 6:
                    tyre.EditComment(s.toString());
                    break;
            }
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }
}