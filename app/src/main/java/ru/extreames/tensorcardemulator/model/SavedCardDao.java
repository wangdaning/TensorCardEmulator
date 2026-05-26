package ru.extreames.tensorcardemulator.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.room.Ignore;

@Entity(tableName = "saved_cards", indices = {@Index(value = {"uid"}, unique = true)})
public class SavedCard {
    
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    @NonNull
    public String uid;

    public long createdAt;

    public SavedCard(int id, @NonNull String name, @NonNull String uid, long createdAt) {
        this.id = id;
        this.name = name;
        this.uid = uid;
        this.createdAt = createdAt;
    }

    @Ignore
    public SavedCard(@NonNull String name, @NonNull String uid) {
        this.name = name;
        this.uid = uid;
        this.createdAt = System.currentTimeMillis();
    }
}