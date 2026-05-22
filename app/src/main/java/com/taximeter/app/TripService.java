package com.taximeter.app;

import android.app.*;
import android.content.*;
import android.location.Location;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.*;

public class TripService extends Service {

    public static final String ACTION_UPDATE = "com.taximeter.TRIP_UPDATE";
    public static final String EXTRA_KM      = "km";
    public static final String EXTRA_SECONDS = "seconds";

    private static final String CH_ID = "trip_ch";
    private static final int    NOTIF = 77;

    private final IBinder binder = new TripBinder();

    public class TripBinder extends Binder {
        TripService get() { return TripService.this; }
    }

    private FusedLocationProviderClient fused;
    private LocationCallback locationCb;
    private Location lastLoc     = null;
    private double   totalMeters = 0;
    private long     startMillis = 0;
    private boolean  running     = false;

    private final Handler ticker = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;
            broadcast();
            ticker.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        fused = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    public void startTrip() {
        totalMeters = 0;
        lastLoc     = null;
        startMillis = System.currentTimeMillis();
        running     = true;

        startForeground(NOTIF, buildNotif("Поездка идёт…"));

        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .setMinUpdateDistanceMeters(2f)
                .build();

        locationCb = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc == null) return;
                if (lastLoc != null) {
                    float d = lastLoc.distanceTo(loc);
                    if (d < 250f) totalMeters += d;
                }
                lastLoc = loc;
            }
        };

        try {
            fused.requestLocationUpdates(req, locationCb, Looper.getMainLooper());
        } catch (SecurityException ignored) {}

        ticker.post(tickRunnable);
    }

    public double stopTrip() {
        running = false;
        ticker.removeCallbacks(tickRunnable);
        if (locationCb != null) {
            fused.removeLocationUpdates(locationCb);
            locationCb = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        return totalMeters / 1000.0;
    }

    public double getKm()       { return totalMeters / 1000.0; }
    public long   getSeconds()  { return (System.currentTimeMillis() - startMillis) / 1000; }
    public boolean isRunning()  { return running; }

    private void broadcast() {
        Intent i = new Intent(ACTION_UPDATE);
        i.putExtra(EXTRA_KM,      getKm());
        i.putExtra(EXTRA_SECONDS, getSeconds());
        sendBroadcast(i);
        updateNotif(String.format("%.2f км · %s", getKm(), fmt(getSeconds())));
    }

    private String fmt(long s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
                CH_ID, "Поездка", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotif(String text) {
        return new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle("TaxiMeter")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true).setSilent(true).build();
    }

    private void updateNotif(String text) {
        getSystemService(NotificationManager.class)
                .notify(NOTIF, buildNotif(text));
    }
}
