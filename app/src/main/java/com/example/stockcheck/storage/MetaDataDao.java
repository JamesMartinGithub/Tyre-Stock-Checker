package com.example.stockcheck.storage;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface MetaDataDao {
    @Insert
    void Insert(MetaData metaData);

    @Query("DELETE FROM metadata")
    void Clear();

    @Query("SELECT * FROM metadata LIMIT 1")
    MetaData Get();
}
