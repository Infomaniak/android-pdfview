package com.infomaniak.lib.pdfview.scroll;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.infomaniak.lib.pdfview.PDFView;
import com.infomaniak.lib.pdfview.R;
import com.infomaniak.lib.pdfview.util.Util;

public class DefaultScrollHandle extends RelativeLayout implements ScrollHandle {

    private final static int HANDLE_WIDTH = 65;
    private final static int HANDLE_HEIGHT = 40;
    private final static int DEFAULT_TEXT_SIZE = 12;

    private final Handler handler = new Handler();
    private final boolean inverted;
    private final Runnable hidePageScrollerRunnable = this::hide;

    private float relativeHandlerMiddle = 0f;
    private PDFView pdfView;
    private float currentPos;
    private Drawable handleBackgroundDrawable;
    private View handleBackgroundView;
    private int handleAlign;
    private int handleWidth = HANDLE_WIDTH;
    private int handleHeight = HANDLE_HEIGHT;
    private int hideHandleDelayMillis = 1000;

    protected TextView textView;
    protected Context context;

    public DefaultScrollHandle(Context context) {
        this(context, false);
    }

    public DefaultScrollHandle(Context context, boolean inverted) {
        super(context);
        this.context = context;
        this.inverted = inverted;
        textView = new TextView(context);
        setVisibility(INVISIBLE);
        setTextColor(Color.BLACK);
        setTextSize(DEFAULT_TEXT_SIZE);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
        if (pdfView.isSwipeVertical()) {
            if (inverted) { // left
                handleAlign = ALIGN_PARENT_LEFT;
            } else { // right
                handleAlign = ALIGN_PARENT_RIGHT;
            }
        } else {
            int tempWidth = handleWidth;
            handleWidth = handleHeight;
            handleHeight =  tempWidth;
            if (inverted) { // top
                handleAlign = ALIGN_PARENT_TOP;
            } else { // bottom
                handleAlign = ALIGN_PARENT_BOTTOM;
            }
        }

        setHandleBackground();

        LayoutParams lp = new LayoutParams(Util.getDP(context, handleWidth), Util.getDP(context,
                handleHeight));
        lp.setMargins(0, 0, 0, 0);

        LayoutParams tvlp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvlp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        addView(textView, tvlp);

        lp.addRule(handleAlign);
        pdfView.addView(this, lp);

        this.pdfView = pdfView;
    }

    private void setHandleBackground() {
        if (handleBackgroundDrawable != null) {
            setBackground(handleBackgroundDrawable);
        } else if (handleBackgroundView != null) {
            addView(handleBackgroundView);
        } else {
            int handleBackground;
            switch (handleAlign) {
                case ALIGN_PARENT_LEFT: {
                    handleBackground = R.drawable.default_scroll_handle_left;
                    break;
                }
                case ALIGN_PARENT_RIGHT:{
                    handleBackground = R.drawable.default_scroll_handle_right;
                    break;
                }
                case ALIGN_PARENT_TOP:{
                    handleBackground = R.drawable.default_scroll_handle_top;
                    break;
                }
                default:{
                    handleBackground = R.drawable.default_scroll_handle_bottom;
                    break;
                }
            }
            setBackground(getDrawable(handleBackground));
        }
    }

    private Drawable getDrawable(int resDrawable) {
        return ContextCompat.getDrawable(context, resDrawable);
    }

    @Override
    public void destroyLayout() {
        pdfView.removeView(this);
    }

    @Override
    public void setScroll(float position) {
        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }
        if (pdfView != null) {
            setPosition((pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
        }
    }

    private void setPosition(float pos) {
        if (Float.isInfinite(pos) || Float.isNaN(pos)) {
            return;
        }
        float pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pdfViewSize = pdfView.getHeight();
        } else {
            pdfViewSize = pdfView.getWidth();
        }
        pos -= relativeHandlerMiddle;

        if (pos < 0) {
            pos = 0;
        } else if (pos > pdfViewSize - Util.getDP(context, HANDLE_HEIGHT)) {
            pos = pdfViewSize - Util.getDP(context, HANDLE_HEIGHT);
        }

        if (pdfView.isSwipeVertical()) {
            setY(pos);
        } else {
            setX(pos);
        }

        calculateMiddle();
        invalidate();
    }

    private void calculateMiddle() {
        float pos, viewSize, pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pos = getY();
            viewSize = getHeight();
            pdfViewSize = pdfView.getHeight();
        } else {
            pos = getX();
            viewSize = getWidth();
            pdfViewSize = pdfView.getWidth();
        }
        relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize;
    }

    @Override
    public void hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, hideHandleDelayMillis);
    }

    @Override
    public void setPageNum(int pageNum) {
        String text = String.valueOf(pageNum);
        if (!textView.getText().equals(text)) {
            textView.setText(text);
        }
    }

    @Override
    public boolean shown() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void show() {
        setVisibility(VISIBLE);
    }

    @Override
    public void hide() {
        setVisibility(INVISIBLE);
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    /**
     * @param size text size in dp
     */
    public void setTextSize(int size) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    public void setPageHandleBackground(Drawable handleBackgroundDrawable) {
        this.handleBackgroundDrawable = handleBackgroundDrawable;
    }

    public void setPageHandleBackgroundView(View handleBackgroundView) {
        this.handleBackgroundView = handleBackgroundView;
    }

    public void setHandleWidth(int handleWidth) {
        this.handleWidth = handleWidth;
    }

    public void setHandleHeight(int handleHeight) {
        this.handleHeight = handleHeight;
    }

    public void setHideHandleDelay(int hideHandleDelayMillis) {
        this.hideHandleDelayMillis = hideHandleDelayMillis;
    }

    private boolean isPDFViewReady() {
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPDFViewReady()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pdfView.stopFling();
                handler.removeCallbacks(hidePageScrollerRunnable);
                if (pdfView.isSwipeVertical()) {
                    currentPos = event.getRawY() - getY();
                } else {
                    currentPos = event.getRawX() - getX();
                }
            case MotionEvent.ACTION_MOVE:
                if (pdfView.isSwipeVertical()) {
                    setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getHeight(), false);
                } else {
                    setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getWidth(), false);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                hideDelayed();
                pdfView.performPageSnap();
                return true;
        }

        return super.onTouchEvent(event);
    }
}
