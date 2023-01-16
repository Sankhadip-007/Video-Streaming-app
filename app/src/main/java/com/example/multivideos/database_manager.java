package com.example.multivideos;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

public class database_manager extends SQLiteOpenHelper {

    public  database_manager(Context context){
        super(context,"android_db",null,1);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS videos(" +
                "id"+" INTEGER PRIMARY KEY, "+
                "name"+" VARCHAR" +")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE videos");
        onCreate(db);
    }

    public void addData(@NonNull Video v) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put("id", v.getId());
        contentValues.put("name",v.getName());
        sqLiteDatabase.insert("videos", null, contentValues);
        sqLiteDatabase.close();
    }
}
