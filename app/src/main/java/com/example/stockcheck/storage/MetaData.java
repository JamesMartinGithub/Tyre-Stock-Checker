package com.example.stockcheck.storage;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MetaData {
    @PrimaryKey
    @NonNull
    public String savedTime;
    public String savedFilename;
}
