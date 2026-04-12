package com.example.antyspamer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "TrustCallDB";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_HISTORY = "history";
    public static final String COL_ID = "id";
    public static final String COL_KEYWORD = "keyword";
    public static final String COL_CONTEXT = "context";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_STATUS = "status";

    public static final String TABLE_GUARDIANS = "guardians";
    public static final String COL_G_ID = "id";
    public static final String COL_G_NAME = "name";
    public static final String COL_G_PHONE = "phone";

    // Modeli danych wewnątrz Helpera, by ograniczyć liczbę plików
    public static class Guardian {
        public int id;
        public String name, phone;
        public Guardian(int id, String name, String phone) { this.id = id; this.name = name; this.phone = phone; }
    }

    public static class AlertItem {
        public int id;
        public String keyword, context, timestamp, status;
        public AlertItem(int id, String keyword, String context, String timestamp, String status) {
            this.id = id; this.keyword = keyword; this.context = context; this.timestamp = timestamp; this.status = status;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_KEYWORD + " TEXT, " + COL_CONTEXT + " TEXT, " + COL_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " + COL_STATUS + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_GUARDIANS + " (" + COL_G_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_G_NAME + " TEXT, " + COL_G_PHONE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + TABLE_GUARDIANS + " (" + COL_G_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_G_NAME + " TEXT, " + COL_G_PHONE + " TEXT)");
        }
    }

    public void addAlert(String keyword, String context, String status) {
        ContentValues v = new ContentValues();
        v.put(COL_KEYWORD, keyword); v.put(COL_CONTEXT, context); v.put(COL_STATUS, status);
        getWritableDatabase().insert(TABLE_HISTORY, null, v);
    }

    public void updateLastAlertStatus(String status) {
        getWritableDatabase().execSQL("UPDATE " + TABLE_HISTORY + " SET " + COL_STATUS + " = '" + status + "' WHERE " + COL_ID + " = (SELECT MAX(" + COL_ID + ") FROM " + TABLE_HISTORY + ")");
    }

    public List<AlertItem> getAllHistory() {
        List<AlertItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COL_ID + " DESC", null);
        if (c.moveToFirst()) {
            do { list.add(new AlertItem(c.getInt(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4))); } while (c.moveToNext());
        }
        c.close();
        return list;
    }

    public void addGuardian(String name, String phone) {
        ContentValues v = new ContentValues();
        v.put(COL_G_NAME, name); v.put(COL_G_PHONE, phone);
        getWritableDatabase().insert(TABLE_GUARDIANS, null, v);
    }

    public void deleteGuardian(int id) {
        getWritableDatabase().delete(TABLE_GUARDIANS, COL_G_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Guardian> getAllGuardians() {
        List<Guardian> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_GUARDIANS, null);
        if (c.moveToFirst()) {
            do { list.add(new Guardian(c.getInt(0), c.getString(1), c.getString(2))); } while (c.moveToNext());
        }
        c.close();
        return list;
    }
}
