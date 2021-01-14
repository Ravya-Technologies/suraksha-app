package com.suraksha.shaurya.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "message_history")
public class AppEntity {

    @ColumnInfo(name = "name")
    @PrimaryKey
    @NonNull
    private Long time;

    @ColumnInfo(name = "message")
    private String message;

    @ColumnInfo(name = "receiverNumber")
    private String receiverNumber;


    @NonNull
    public Long getTime() {
        return time;
    }

    public void setTime(@NonNull Long time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }

    public void setReceiverNumber(String receiverNumber) {
        this.receiverNumber = receiverNumber;
    }
}
