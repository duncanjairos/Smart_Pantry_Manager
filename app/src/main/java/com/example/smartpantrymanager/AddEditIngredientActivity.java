package com.example.smartpantrymanager;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartpantrymanager.models.PantryItem;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen 2: Add / Edit Ingredient. Handles both create and update, with input
 * validation on every field before writing to the database.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";

    private static final String[] UNIT_OPTIONS = {
            "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb", "unit"
    };

    private DatabaseHelper dbHelper;
    private long editingItemId = -1;

    private TextInputEditText etName, etQuantity, etExpiry;
    private Spinner spinnerUnit;
    private Button btnDelete;

    private String selectedExpiryDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        dbHelper = DatabaseHelper.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etName);
        etQuantity = findViewById(R.id.etQuantity);
        etExpiry = findViewById(R.id.etExpiry);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        Button btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNIT_OPTIONS);
        spinnerUnit.setAdapter(unitAdapter);

        etExpiry.setOnClickListener(v -> showDatePicker());

        editingItemId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        if (editingItemId != -1) {
            toolbar.setTitle("Edit Ingredient");
            btnDelete.setVisibility(android.view.View.VISIBLE);
            loadExistingItem();
        } else {
            toolbar.setTitle("Add Ingredient");
        }

        btnSave.setOnClickListener(v -> saveIngredient());
        btnDelete.setOnClickListener(v -> {
            dbHelper.deletePantryItem(editingItemId);
            Toast.makeText(this, "Ingredient deleted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadExistingItem() {
        PantryItem item = dbHelper.getPantryItem(editingItemId);
        if (item == null) return;

        etName.setText(item.getName());
        etQuantity.setText(formatQuantity(item.getQuantity()));

        int unitIndex = indexOfUnit(item.getUnit());
        spinnerUnit.setSelection(unitIndex >= 0 ? unitIndex : UNIT_OPTIONS.length - 1);

        if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
            selectedExpiryDate = item.getExpiryDate();
            etExpiry.setText(selectedExpiryDate);
        }
    }

    private int indexOfUnit(String unit) {
        for (int i = 0; i < UNIT_OPTIONS.length; i++) {
            if (UNIT_OPTIONS[i].equalsIgnoreCase(unit)) return i;
        }
        return -1;
    }

    private String formatQuantity(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedExpiryDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etExpiry.setText(selectedExpiryDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * Validates all fields before writing to the database. Returns true only if
     * the form is valid; otherwise shows an inline error and returns false.
     */
    private boolean validateInputs(String name, String quantityStr) {
        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            etName.setError("Ingredient name is required");
            valid = false;
        }

        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity is required");
            valid = false;
        } else {
            try {
                double qty = Double.parseDouble(quantityStr);
                if (qty <= 0) {
                    etQuantity.setError("Quantity must be greater than zero");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etQuantity.setError("Enter a valid number");
                valid = false;
            }
        }

        return valid;
    }

    private void saveIngredient() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";

        if (!validateInputs(name, quantityStr)) {
            return;
        }

        double quantity = Double.parseDouble(quantityStr);
        String unit = (String) spinnerUnit.getSelectedItem();

        PantryItem item = new PantryItem(editingItemId, name, quantity, unit, selectedExpiryDate);

        if (editingItemId == -1) {
            dbHelper.addPantryItem(item);
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            dbHelper.updatePantryItem(item);
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
