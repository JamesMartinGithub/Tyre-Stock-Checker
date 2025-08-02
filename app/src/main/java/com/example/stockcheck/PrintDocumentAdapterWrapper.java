package com.example.stockcheck;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;

/**
 * PrintDocumentAdapter wrapper that notifies a OnFinishListener when the print window is exited
 */
public class PrintDocumentAdapterWrapper extends PrintDocumentAdapter {

    public interface OnFinishListener {
        void OnFinish();
    }

    private final PrintDocumentAdapter delegate;
    private final OnFinishListener listener;
    public PrintDocumentAdapterWrapper(PrintDocumentAdapter adapter, OnFinishListener listener){
        super();
        this.delegate = adapter;
        this.listener = listener;
    }

    public void onFinish(){
        delegate.onFinish();
        listener.OnFinish();
    }

    @Override
    public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes1, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
        delegate.onLayout(printAttributes, printAttributes1, cancellationSignal, layoutResultCallback, bundle);
    }

    @Override
    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
        delegate.onWrite(pageRanges, parcelFileDescriptor, cancellationSignal, writeResultCallback);
    }
}