package com.suraksha.shaurya.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = AppEntity.class, version = 1, exportSchema = false)
public abstract class AppDB extends RoomDatabase {
    public abstract AppDao getDao();

    private static final String DATABASE_NAME = "suraksha.db";


    private static AppDB sInstance;

    public static synchronized AppDB createDatabase(Context context) {
        if (sInstance == null) {
            sInstance =
                    createSurakshaDatabase(context);
        }
        return sInstance;
    }


    private static AppDB createSurakshaDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDB.class, DATABASE_NAME).allowMainThreadQueries()
                .build();
    }


}
