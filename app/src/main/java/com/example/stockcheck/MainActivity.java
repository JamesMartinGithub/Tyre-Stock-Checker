package com.example.stockcheck;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.stockcheck.databinding.ActivityMainBinding;
import com.example.stockcheck.filemanagement.CSVReader;
import com.example.stockcheck.filemanagement.XLSXReader;
import com.example.stockcheck.model.Tyre;
import com.example.stockcheck.model.TyreContainer;
import com.example.stockcheck.storage.MetaData;
import com.example.stockcheck.storage.StoredTyre;
import com.example.stockcheck.storage.TyreDatabase;
import java.util.ArrayList;
import java.util.List;

/**
 * First loaded activity that handles file selection
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                fileName = null;
                binding.savedVersionPanel.setVisibility(View.GONE);
                binding.fileButton.setVisibility(View.VISIBLE);
            }
        });

        // Register file selector launcher
        ActivityResultLauncher<Intent> fileSelectorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                MainActivity.this::FileSelected
        );

        // Add listener to file select button
        binding.fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open file picker with file type restrictions
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String [] mimeTypes = {"text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                fileSelectorLauncher.launch(intent);
            }
        });

        binding.saveLoadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Update tyres with saved versions
                ArrayList<Tyre> tyreList = TyreContainer.getInstance().GetTyreList();
                TyreDatabase database = TyreDatabase.getDatabase(getApplicationContext());
                TyreDatabase.databaseWriteExecutor.execute(() -> {
                    List<StoredTyre> storedTyreList = database.storedTyreDao().Get();
                    for (StoredTyre storedTyre : storedTyreList) {
                        int id = storedTyre.id;
                        boolean matched = false;
                        for (Tyre tyre : tyreList) {
                            if (tyre.GetId() == id) {
                                tyre.FromStoredTyre(storedTyre);
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            Tyre addedTyre = new Tyre(id, "", "", "", "", "", "", true);
                            addedTyre.FromStoredTyre(storedTyre);
                            tyreList.add(addedTyre);
                        }
                    }
                    runOnUiThread(() -> { LoadTyreList(); });
                });
            }
        });
        binding.saveIgnoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadTyreList();
            }
        });
    }

    /**
     * Takes a file selector activity result, tries to parse the selected file, and starts the tyre list activity.
     * @param result The file selector activity result
     */
    private void FileSelected(androidx.activity.result.ActivityResult result) {
        Uri fileUri;
        Cursor cursor = null;
        Intent resultIntent = result.getData();
        if (resultIntent != null) {
            try {
                // Get file Uri and determine file name and type
                fileUri = resultIntent.getData();
                String fileType;
                String fileName;
                fileType = getContentResolver().getType(fileUri);
                cursor = getContentResolver().query(fileUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        fileName = cursor.getString(columnIndex);
                    } else {
                        throw new Exception("Cannot get file name");
                    }
                } else {
                    fileName = "";
                }

                // Parse file to get tyre list
                ArrayList<Tyre> tyreList;
                if (fileType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                    // Parse as .xlsx
                    tyreList = XLSXReader.Read(fileUri, getApplicationContext());
                } else if (fileType.equals("text/csv") || fileType.equals("text/comma-separated-values") || fileType.equals("text/plain") || fileType.equals("application/csv") || fileType.equals("application/vnd.ms-excel")) {
                    // Parse as .csv
                    tyreList = CSVReader.Read(fileUri, getApplicationContext());
                } else {
                    throw new Exception("Invalid file type selected");
                }

                // Put tyre list into singleton container for access by other activities
                TyreContainer.getInstance().SetTyreList(tyreList);
                this.fileName = fileName;
                binding.fileButton.setVisibility(View.GONE);

                // Check database for save data
                TyreDatabase database = TyreDatabase.getDatabase(getApplicationContext());
                TyreDatabase.databaseWriteExecutor.execute(() -> {
                    MetaData metaData = database.metaDataDao().Get();
                    if (metaData != null && metaData.savedFilename.equals(fileName)) {
                        // Save data exists for selected file, show option to load save data
                        this.runOnUiThread(() -> {
                            binding.timeDateText.setText(getString(R.string.brackets, metaData.savedTime));
                            binding.savedVersionPanel.setVisibility(View.VISIBLE);
                        });
                    } else {
                        // No save data, load tyre list activity
                        LoadTyreList();
                    }
                });
            } catch (Exception e) {
                // Could not read file
                binding.errorTextView.setText(e.toString());
                System.out.println(e.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void LoadTyreList() {
        Intent tyreListIntent = new Intent(MainActivity.this,TyreListActivity.class);
        tyreListIntent.putExtra("filename", fileName);
        startActivity(tyreListIntent);
    }
}