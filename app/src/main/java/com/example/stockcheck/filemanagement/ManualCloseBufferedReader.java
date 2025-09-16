package com.example.stockcheck.filemanagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Extension of BufferedReader that prevents automatic closing; it can be closed via the ManualClose method.
 */
public class ManualCloseBufferedReader extends BufferedReader {
    public ManualCloseBufferedReader(Reader in) {
        super(in);
    }

    @Override
    public void close() {}

    public void ManualClose() {
        try {
            super.close();
        } catch (IOException ignored) {}
    }
}
