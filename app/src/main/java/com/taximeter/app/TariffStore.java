package com.taximeter.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TariffStore {
    private static final String PREFS = "taximeter_prefs";
    private static final String KEY   = "tariffs_v2";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public TariffStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Tariff> load() {
        String json = prefs.getString(KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Tariff>>(){}.getType();
        List<Tariff> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void save(List<Tariff> list) {
        prefs.edit().putString(KEY, gson.toJson(list)).apply();
    }

    public void add(Tariff t, List<Tariff> list) {
        list.add(t);
        save(list);
    }

    public void update(Tariff t, List<Tariff> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(t.id)) { list.set(i, t); break; }
        }
        save(list);
    }

    public void delete(String id, List<Tariff> list) {
        list.removeIf(t -> t.id.equals(id));
        save(list);
    }
}
