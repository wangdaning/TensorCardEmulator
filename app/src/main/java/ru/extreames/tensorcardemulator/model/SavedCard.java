package ru.extreames.tensorcardemulator.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "saved_cards")
public class SavedCard {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    @NonNull
    public String uid;

    public long createdAt;

    public SavedCard(@NonNull String name, @NonNull String uid) {
        this.name = name;
        this.uid = uid;
        this.createdAt = System.currentTimeMillis();
    }
}