package com.taximeter.app;

import java.util.UUID;

public class Tariff {
    public String id;
    public String name;
    public double pricePerKm;
    public double minPrice;
    public double fuelPer100km;
    public int rounding;

    public Tariff() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.pricePerKm = 30;
        this.minPrice = 100;
        this.fuelPer100km = 8;
        this.rounding = 10;
    }

    public double calcPrice(double distanceKm) {
        double raw = distanceKm * pricePerKm;
        double withMin = Math.max(raw, minPrice);
        if (rounding <= 1) return withMin;
        return Math.ceil(withMin / rounding) * rounding;
    }

    public double calcFuel(double distanceKm) {
        return distanceKm * fuelPer100km / 100.0;
    }
}
