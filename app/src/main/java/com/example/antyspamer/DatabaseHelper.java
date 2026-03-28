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
    private static final int DATABASE_VERSION = 2; // Incremented version

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_KEYWORD + " TEXT, " +
                COL_CONTEXT + " TEXT, " +
                COL_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                COL_STATUS + " TEXT)";
        db.execSQL(createHistoryTable);

        String createGuardiansTable = "CREATE TABLE " + TABLE_GUARDIANS + " (" +
                COL_G_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_G_NAME + " TEXT, " +
                COL_G_PHONE + " TEXT)";
        db.execSQL(createGuardiansTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String createGuardiansTable = "CREATE TABLE " + TABLE_GUARDIANS + " (" +
                    COL_G_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_G_NAME + " TEXT, " +
                    COL_G_PHONE + " TEXT)";
            db.execSQL(createGuardiansTable);
        }
    }

    // History methods
    public long addAlert(String keyword, String context, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEYWORD, keyword);
        values.put(COL_CONTEXT, context);
        values.put(COL_STATUS, status);
        return db.insert(TABLE_HISTORY, null, values);
    }

    public void updateLastAlertStatus(String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_HISTORY + " SET " + COL_STATUS + " = '" + status + 
                   "' WHERE " + COL_ID + " = (SELECT MAX(" + COL_ID + ") FROM " + TABLE_HISTORY + ")");
    }

    public List<AlertItem> getAllHistory() {
        List<AlertItem> list = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COL_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new AlertItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Guardian methods
    public long addGuardian(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_G_NAME, name);
        values.put(COL_G_PHONE, phone);
        return db.insert(TABLE_GUARDIANS, null, values);
    }

    public void deleteGuardian(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GUARDIANS, COL_G_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Guardian> getAllGuardians() {
        List<Guardian> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_GUARDIANS, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Guardian(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
