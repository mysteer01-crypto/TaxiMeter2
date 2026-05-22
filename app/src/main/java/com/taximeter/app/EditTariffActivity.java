package com.taximeter.app;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class EditTariffActivity extends AppCompatActivity {

    private EditText etName, etPriceKm, etMinPrice, etFuel, etRounding;
    private TariffStore store;
    private List<Tariff> tariffs;
    private Tariff editing = null;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_edit_tariff);
        store   = new TariffStore(this);
        tariffs = store.load();

        etName     = findViewById(R.id.et_name);
        etPriceKm  = findViewById(R.id.et_price_km);
        etMinPrice = findViewById(R.id.et_min_price);
        etFuel     = findViewById(R.id.et_fuel);
        etRounding = findViewById(R.id.et_rounding);

        String editId = getIntent().getStringExtra("tariff_id");
        if (editId != null) {
            for (Tariff t : tariffs) if (t.id.equals(editId)) { editing = t; break; }
        }

        if (editing != null) {
            etName.setText(editing.name);
            etPriceKm.setText(String.valueOf((int) editing.pricePerKm));
            etMinPrice.setText(String.valueOf((int) editing.minPrice));
            etFuel.setText(String.valueOf(editing.fuelPer100km));
            etRounding.setText(String.valueOf(editing.rounding));
            ((TextView) findViewById(R.id.tv_screen_title)).setText("Редактировать тариф");
            findViewById(R.id.btn_delete).setVisibility(View.VISIBLE);
        } else {
            etRounding.setText("10");
            etFuel.setText("8");
            etMinPrice.setText("100");
            etPriceKm.setText("30");
            ((TextView) findViewById(R.id.tv_screen_title)).setText("Новый тариф");
            findViewById(R.id.btn_delete).setVisibility(View.GONE);
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) { etName.setError("Введите название"); return; }
        double priceKm, minPrice, fuel;
        int rounding;
        try {
            priceKm  = Double.parseDouble(etPriceKm.getText().toString().trim());
            minPrice = Double.parseDouble(etMinPrice.getText().toString().trim());
            fuel     = Double.parseDouble(etFuel.getText().toString().trim());
            rounding = Integer.parseInt(etRounding.getText().toString().trim());
            if (priceKm <= 0 || minPrice < 0 || fuel < 0 || rounding < 1)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Проверьте введённые числа", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editing == null) {
            Tariff t = new Tariff();
            t.name = name; t.pricePerKm = priceKm;
            t.minPrice = minPrice; t.fuelPer100km = fuel; t.rounding = rounding;
            store.add(t, tariffs);
        } else {
            editing.name = name; editing.pricePerKm = priceKm;
            editing.minPrice = minPrice; editing.fuelPer100km = fuel; editing.rounding = rounding;
            store.update(editing, tariffs);
        }
        Toast.makeText(this, "Сохранено ✓", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Удалить тариф?")
            .setMessage("«" + editing.name + "» будет удалён.")
            .setPositiveButton("Удалить", (d, w) -> {
                store.delete(editing.id, tariffs);
                finish();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
}
