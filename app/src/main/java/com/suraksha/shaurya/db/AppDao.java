package com.suraksha.shaurya.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(AppEntity chat);

    @Query("SELECT * FROM message_history")
    List<AppEntity> fetchChatRooms();

}