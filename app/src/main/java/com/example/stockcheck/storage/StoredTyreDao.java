package com.example.stockcheck.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.ArrayList;
import java.util.List;

@Dao
public interface StoredTyreDao {
    @Insert
    void Insert(ArrayList<StoredTyre> storedTyres);

    @Query("DELETE FROM storedtyre")
    void Clear();

    @Query("SELECT * FROM storedtyre")
    List<StoredTyre> Get();
}
