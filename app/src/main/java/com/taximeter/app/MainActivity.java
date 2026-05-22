package com.taximeter.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // Screens
    private View screenHome, screenRunning, screenResult;

    // Home
    private LinearLayout tariffContainer;
    private TextView     tvNoTariffs;

    // Running
    private TextView tvRunDist, tvRunPrice, tvRunTime, tvRunFuel, tvRunSpeed, tvRunTariff;
    private View     gpsDot;

    // Result
    private TextView tvResDist, tvResPrice, tvResTime, tvResFuel, tvResTariff;

    // State
    private List<Tariff> tariffs = new ArrayList<>();
    private TariffStore  store;
    private Tariff       selected = null;
    private TripService  svc;
    private boolean      bound = false;
    private double       finalKm = 0;
    private long         finalSec = 0;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            svc = ((TripService.TripBinder) b).get();
            bound = true;
        }
        @Override public void onServiceDisconnected(ComponentName n) {
            bound = false; svc = null;
        }
    };

    private final BroadcastReceiver gpsRx = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) {
            double km = i.getDoubleExtra(TripService.EXTRA_KM, 0);
            long   s  = i.getLongExtra(TripService.EXTRA_SECONDS, 0);
            finalSec = s;
            updateRunning(km, s);
        }
    };

    private final ActivityResultLauncher<String[]> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> {
            boolean ok = Boolean.TRUE.equals(r.get(Manifest.permission.ACCESS_FINE_LOCATION))
                      || Boolean.TRUE.equals(r.get(Manifest.permission.ACCESS_COARSE_LOCATION));
            if (ok) doStart();
            else Toast.makeText(this, "Нужен доступ к геолокации", Toast.LENGTH_LONG).show();
        });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        store = new TariffStore(this);
        bindViews();
        showScreen(screenHome, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tariffs = store.load();
        buildList();
        ContextCompat.registerReceiver(this, gpsRx,
            new IntentFilter(TripService.ACTION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(gpsRx); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) { unbindService(conn); bound = false; }
    }

    private void bindViews() {
        screenHome    = findViewById(R.id.screen_home);
        screenRunning = findViewById(R.id.screen_running);
        screenResult  = findViewById(R.id.screen_result);
        tariffContainer = findViewById(R.id.tariff_container);
        tvNoTariffs     = findViewById(R.id.tv_no_tariffs);
        tvRunDist  = findViewById(R.id.tv_run_dist);
        tvRunPrice = findViewById(R.id.tv_run_price);
        tvRunTime  = findViewById(R.id.tv_run_time);
        tvRunFuel  = findViewById(R.id.tv_run_fuel);
        tvRunSpeed = findViewById(R.id.tv_run_speed);
        tvRunTariff= findViewById(R.id.tv_run_tariff);
        gpsDot     = findViewById(R.id.gps_dot);
        tvResDist   = findViewById(R.id.tv_res_dist);
        tvResPrice  = findViewById(R.id.tv_res_price);
        tvResTime   = findViewById(R.id.tv_res_time);
        tvResFuel   = findViewById(R.id.tv_res_fuel);
        tvResTariff = findViewById(R.id.tv_res_tariff);

        findViewById(R.id.btn_start).setOnClickListener(v -> onStart());
        findViewById(R.id.btn_stop).setOnClickListener(v -> onStop());
        findViewById(R.id.btn_new_trip).setOnClickListener(v -> {
            showScreen(screenHome, true);
            buildList();
        });
        findViewById(R.id.btn_add_tariff).setOnClickListener(v ->
            startActivity(new Intent(this, EditTariffActivity.class)));
        findViewById(R.id.btn_settings).setOnClickListener(v ->
            startActivity(new Intent(this, EditTariffActivity.class)));
    }

    private void buildList() {
        tariffContainer.removeAllViews();
        if (tariffs.isEmpty()) {
            tvNoTariffs.setVisibility(View.VISIBLE);
            tariffContainer.setVisibility(View.GONE);
            selected = null;
            return;
        }
        tvNoTariffs.setVisibility(View.GONE);
        tariffContainer.setVisibility(View.VISIBLE);

        // Keep previous selection
        if (selected != null) {
            boolean found = false;
            for (Tariff t : tariffs) if (t.id.equals(selected.id)) { selected = t; found = true; break; }
            if (!found) selected = null;
        }
        if (selected == null) selected = tariffs.get(0);

        LayoutInflater inf = LayoutInflater.from(this);
        for (Tariff t : tariffs) {
            View card = inf.inflate(R.layout.item_tariff, tariffContainer, false);
            ((TextView) card.findViewById(R.id.tv_name)).setText(t.name);
            ((TextView) card.findViewById(R.id.tv_rate)).setText(
                String.format(Locale.getDefault(), "%.0f ₽/км", t.pricePerKm));
            ((TextView) card.findViewById(R.id.tv_min)).setText(
                String.format(Locale.getDefault(), "мин. %.0f ₽", t.minPrice));

            boolean sel = selected.id.equals(t.id);
            applySelection(card, sel);

            card.setOnClickListener(v -> { selected = t; buildList(); });
            card.setOnLongClickListener(v -> {
                Intent i = new Intent(this, EditTariffActivity.class);
                i.putExtra("tariff_id", t.id);
                startActivity(i);
                return true;
            });

            Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            card.startAnimation(anim);
            tariffContainer.addView(card);
        }
    }

    private void applySelection(View card, boolean sel) {
        MaterialCardView mc = (MaterialCardView) card;
        mc.setStrokeColor(sel ? getColor(R.color.accent_blue) : getColor(R.color.card_stroke));
        mc.setStrokeWidth(sel ? 6 : 2);
        card.findViewById(R.id.sel_mark).setVisibility(sel ? View.VISIBLE : View.GONE);
    }

    private void onStart() {
        if (selected == null) {
            Toast.makeText(this, "Создайте тариф через кнопку «+»", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean fine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) doStart();
        else permLauncher.launch(new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    private void doStart() {
        tvRunTariff.setText(selected.name);
        tvRunDist.setText("0.00");
        tvRunPrice.setText("0 ₽");
        tvRunTime.setText("00:00");
        tvRunFuel.setText("0.00 л");
        tvRunSpeed.setText("0 км/ч");
        showScreen(screenRunning, true);
        startGpsDot();

        Intent svcIntent = new Intent(this, TripService.class);
        ContextCompat.startForegroundService(this, svcIntent);
        bindService(svcIntent, conn, BIND_AUTO_CREATE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (bound && svc != null) svc.startTrip();
        }, 400);
    }

    private void onStop() {
        if (bound && svc != null) {
            finalKm = svc.stopTrip();
            unbindService(conn);
            bound = false; svc = null;
        }
        gpsDot.clearAnimation();
        showResult();
    }

    private void updateRunning(double km, long sec) {
        if (selected == null) return;
        tvRunDist.setText(String.format(Locale.getDefault(), "%.2f", km));
        tvRunPrice.setText(String.format(Locale.getDefault(), "%.0f ₽", selected.calcPrice(km)));
        tvRunTime.setText(fmt(sec));
        tvRunFuel.setText(String.format(Locale.getDefault(), "%.2f л", selected.calcFuel(km)));
        double spd = sec > 0 ? km / (sec / 3600.0) : 0;
        tvRunSpeed.setText(String.format(Locale.getDefault(), "%.0f км/ч", spd));
    }

    private void showResult() {
        if (selected == null) return;
        tvResTariff.setText(selected.name);
        tvResPrice.setText(String.format(Locale.getDefault(), "%.0f ₽", selected.calcPrice(finalKm)));
        tvResDist.setText(String.format(Locale.getDefault(), "%.2f км", finalKm));
        tvResTime.setText(fmt(finalSec));
        tvResFuel.setText(String.format(Locale.getDefault(), "%.2f л", selected.calcFuel(finalKm)));
        showScreen(screenResult, true);
    }

    private void showScreen(View target, boolean animate) {
        View[] all = {screenHome, screenRunning, screenResult};
        for (View v : all) {
            if (v == target) {
                v.setVisibility(View.VISIBLE);
                if (animate) {
                    Animation a = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                    v.startAnimation(a);
                }
            } else {
                v.setVisibility(View.GONE);
            }
        }
    }

    private void startGpsDot() {
        AlphaAnimation pulse = new AlphaAnimation(1f, 0.15f);
        pulse.setDuration(700);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        gpsDot.startAnimation(pulse);
    }

    private String fmt(long s) {
        return String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60);
    }
}
