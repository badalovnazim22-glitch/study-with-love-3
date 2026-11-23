package com.studywithlove.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Local SQLite store for the weakness trainer, with one-time SharedPreferences migration. */
public final class MistakeStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "study_mistakes.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "mistakes";

    public MistakeStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "topic TEXT NOT NULL,"
                + "prompt TEXT NOT NULL,"
                + "payload TEXT NOT NULL,"
                + "updated_at INTEGER NOT NULL,"
                + "UNIQUE(topic, prompt))");
        db.execSQL("CREATE INDEX mistakes_updated_idx ON " + TABLE + "(updated_at DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Versioned migrations belong here; v1 is the initial SQLite schema.
    }

    public synchronized void migrateLegacy(SharedPreferences preferences) {
        if (preferences.getBoolean("mistakes_sqlite_migrated", false)) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                if (!entry.getKey().startsWith("mistake_") || !(entry.getValue() instanceof String)) continue;
                String payload = (String) entry.getValue();
                try {
                    org.json.JSONObject item = new org.json.JSONObject(payload);
                    String topic = item.optString("topic", "");
                    String prompt = item.optString("prompt", "");
                    if (topic.isEmpty() || prompt.isEmpty()) continue;
                    ContentValues values = new ContentValues();
                    values.put("topic", topic);
                    values.put("prompt", prompt);
                    values.put("payload", payload);
                    values.put("updated_at", now++);
                    db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                } catch (Exception ignored) { }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        preferences.edit().putBoolean("mistakes_sqlite_migrated", true).apply();
        trimToLatest100();
    }

    public synchronized void save(String topic, String prompt, String payload) {
        if (topic == null || topic.isEmpty() || prompt == null || prompt.isEmpty() || payload == null) return;
        ContentValues values = new ContentValues();
        values.put("topic", topic);
        values.put("prompt", prompt);
        values.put("payload", payload);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        trimToLatest100();
    }

    public synchronized void delete(String topic, String prompt) {
        if (topic == null || prompt == null) return;
        getWritableDatabase().delete(TABLE, "topic=? AND prompt=?", new String[]{topic, prompt});
    }

    public synchronized List<String> newestPayloads(int limit) {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE,
                new String[]{"payload"}, null, null, null, null, "updated_at DESC, id DESC",
                String.valueOf(Math.max(1, Math.min(100, limit))))) {
            while (cursor.moveToNext()) result.add(cursor.getString(0));
        }
        return result;
    }

    private void trimToLatest100() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE
                + " WHERE id NOT IN (SELECT id FROM " + TABLE
                + " ORDER BY updated_at DESC, id DESC LIMIT 100)");
    }
}
