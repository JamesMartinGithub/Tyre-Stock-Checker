package com.example.stockcheck;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Extended ScrollView that notifies a OnScrollCallback when it scrolls.
 */
public class ScrollViewNotifying extends ScrollView {

    public interface OnScrollCallback {
        void OnScrollChanged(int x, int y, int oldx, int oldy);
    }

    private OnScrollCallback onScrollCallback = null;

    public ScrollViewNotifying(Context context) {
        super(context);
    }

    public ScrollViewNotifying(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ScrollViewNotifying(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void SetOnScrollCallback(OnScrollCallback onScrollCallback) {
        this.onScrollCallback = onScrollCallback;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        super.onScrollChanged(x, y, oldx, oldy);
        if(onScrollCallback != null) {
            onScrollCallback.OnScrollChanged(x, y, oldx, oldy);
        }
    }

}
