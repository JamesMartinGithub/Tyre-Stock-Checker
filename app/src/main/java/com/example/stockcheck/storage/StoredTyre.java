package com.example.stockcheck.storage;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.stockcheck.model.TyreComment;

@Entity
public class StoredTyre {
    @PrimaryKey
    public int id;
    public TyreComment part;
    public TyreComment supplierPartCode;
    public TyreComment description;
    public TyreComment location;
    public String seen;
    public String lastSoldDateString;
    public boolean lastSoldDateChanged;
    public String extraComment;
    public boolean isEdited;
    public boolean isAdded;
    public boolean isDone;
}
