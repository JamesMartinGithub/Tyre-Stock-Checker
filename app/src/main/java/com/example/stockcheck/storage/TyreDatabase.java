package com.example.stockcheck.storage;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.example.stockcheck.model.TyreComment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {StoredTyre.class, MetaData.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class TyreDatabase extends RoomDatabase {
    public abstract StoredTyreDao storedTyreDao();
    public abstract MetaDataDao metaDataDao();
    private static volatile TyreDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static TyreDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TyreDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), TyreDatabase.class, "tyre_database").build();
                }
            }
        }
        return INSTANCE;
    }
}

class Converters {
    @TypeConverter
    public static TyreComment deserialiseTyreComment(String representation) {
        if (representation == null) return null;
        TyreComment newComment = new TyreComment("");
        newComment.Deserialise(representation);
        return newComment;
    }

    @TypeConverter
    public static String serialiseTyreComment(TyreComment comment) {
        return comment == null ? null : comment.GetSerialised();
    }
}
