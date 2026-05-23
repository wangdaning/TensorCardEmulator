package ru.extreames.tensorcardemulator.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SavedCardDao {
    @Query("SELECT * FROM saved_cards ORDER BY createdAt DESC")
    List<SavedCard> getAll();

    @Insert
    long insert(SavedCard card);

    @Update
    void update(SavedCard card);

    @Delete
    void delete(SavedCard card);

    @Query("SELECT * FROM saved_cards WHERE id = :id LIMIT 1")
    SavedCard getById(int id);
}