package be.keption.todoapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DataBaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "database.db";
    public static final String TABLE_NAME = "data";
    public static final String TASK_COL = "task";

    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + TASK_COL + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public int insert(String task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put(TASK_COL, task);
        long rawId = db.insert(TABLE_NAME, null, value);
        db.close();
        return 1;
    }

    public int delete(String task) {
        SQLiteDatabase db = this.getWritableDatabase();
        long rawId = db.delete(TABLE_NAME, task, new String[]{});
        return 1;
    }

    public ArrayList<String> getData() {
        ArrayList<String> taskList = new ArrayList<String>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
            taskList.add(cursor.getString(0));
        }
        return taskList;
    }
}
