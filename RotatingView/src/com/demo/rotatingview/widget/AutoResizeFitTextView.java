package com.demo.rotatingview.widget;

import android.content.Context;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Created by luanqian on 14-4-13.
 * Text Size will auto change to fit the textView
 * Auto smaller if has no enough space,but has a min size
 * Auto bigger if the space is to much,but has a max size
 */
public class AutoResizeFitTextView extends TextView {

    private static final String TAG = AutoResizeFitTextView.class.getSimpleName();

    private static final float MAX_TEXT_SIZE = 24f;// 24sp
    private static final float MIN_TEXT_SIZE = 1f; //1sp

    private float mTextSize = MAX_TEXT_SIZE;
    private float mSpacingMul = 1.0f;
    private float mSpacingAdd = 0.0f;
    private boolean mAddEllipsis = true;

    private int mLastTextLength =1;
    private boolean mNeedsResize = false;

    /**
     * The max lines the textView can show
     */
    private int mMaxLineForShown = 1;

    /**
     * Whether the textView has changed
     */
    private boolean mSizeChange = false;

    public AutoResizeFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTextSize = getTextSize();
    }

    /**
     * When text changes, we should re-compute the best textSize and requestLayout
     */
    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        mNeedsResize = true;
        requestLayout();
    }

    /**
     * When the textView's size changed, we should also re-compute the best textSize and requestLayout
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            mNeedsResize = true;
            mMaxLineForShown = 1;
            requestLayout();
        }
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        mTextSize = getTextSize();
    }
    /**
     * Resize text after measuring
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed || mNeedsResize) {
            int widthLimit = (right - left) - getCompoundPaddingLeft() - getCompoundPaddingRight();
            int heightLimit = (bottom - top) - getCompoundPaddingBottom() - getCompoundPaddingTop();
            resizeText(widthLimit, heightLimit);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    /**
     * Resize the text size with specified width and height
     * @param width
     * @param height
     */
    public void resizeText(int width, int height) {
        CharSequence text = getText();
        if (text == null || text.length() == 0 || height <= 0 || width <= 0 || mTextSize == 0) {
            return;
        }

        TextPaint textPaint = getPaint();

        float oldTextSize = textPaint.getTextSize();
        float targetTextSize = MAX_TEXT_SIZE;

        int textHeight = getTextHeight(text, textPaint, width, targetTextSize);

        if (textHeight > height){
            mMaxLineForShown = computeTextCount(text,textPaint,width) -1;
        }
        // Until we either fit within our text view or we had reached our min text size, incrementally try smaller sizes
        while (textHeight > height && targetTextSize > MIN_TEXT_SIZE) {
            targetTextSize = Math.max(targetTextSize - 0.5f, MIN_TEXT_SIZE);
            textHeight = getTextHeight(text, textPaint, width, targetTextSize);
        }

        int newCount = computeTextCount(text,textPaint,width);
        if ((mLastTextLength > newCount && newCount > mMaxLineForShown) || takeSizeChange()){
            targetTextSize = oldTextSize;
            while (textHeight < height && targetTextSize < MAX_TEXT_SIZE){
                targetTextSize = Math.min(targetTextSize + 0.5f, MAX_TEXT_SIZE);
                textHeight = getTextHeight(text, textPaint, width, targetTextSize);
            }
            targetTextSize = Math.min(targetTextSize - 0.5f,MAX_TEXT_SIZE);
        }
        mLastTextLength = newCount;
        // If we had reached our minimum text size and still don't fit, append an ellipsis
        if (mAddEllipsis && targetTextSize == MIN_TEXT_SIZE && textHeight > height) {
            // Draw using a static layout
            // modified: use a copy of TextPaint for measuring
            TextPaint paint = new TextPaint(textPaint);
            // Draw using a static layout
            StaticLayout layout = new StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, mSpacingMul, mSpacingAdd, false);
            // Check that we have a least one line of rendered text
            if (layout.getLineCount() > 0) {
                // Since the line at the specific vertical position would be cut off,
                // we must trim up to the previous line
                int lastLine = layout.getLineForVertical(height) - 1;
                // If the text would not even fit on a single line, clear it
                if (lastLine < 0) {
                    setText("");
                }
                // Otherwise, trim to the previous line and add an ellipsis
                else {
                    int start = layout.getLineStart(lastLine);
                    int end = layout.getLineEnd(lastLine);
                    float lineWidth = layout.getLineWidth(lastLine);
                    float ellipseWidth = textPaint.measureText("…");

                    // Trim characters off until we have enough room to draw the ellipsis
                    while (width < lineWidth + ellipseWidth) {
                        lineWidth = textPaint.measureText(text.subSequence(start, --end + 1).toString());
                    }
                    setText(text.subSequence(0, end) + "…");
                }
            }
        }

        setTextSize(TypedValue.COMPLEX_UNIT_PX, (targetTextSize == MAX_TEXT_SIZE)? oldTextSize : targetTextSize);
        setLineSpacing(mSpacingAdd, mSpacingMul);

        mNeedsResize = false;
    }

    // Set the text size of the text paint object and use a static layout to render text off screen before measuring
    private int getTextHeight(CharSequence source, TextPaint paint, int width, float textSize) {
        // modified: make a copy of the original TextPaint object for measuring
        // (apparently the object gets modified while measuring, see also the
        // docs for TextView.getPaint() (which states to access it read-only)
        TextPaint paintCopy = new TextPaint(paint);
        // Update the text paint object
        paintCopy.setTextSize(textSize);
        // Measure using a static layout
        StaticLayout layout = new StaticLayout(source, paintCopy, width, Layout.Alignment.ALIGN_NORMAL, mSpacingMul, mSpacingAdd, true);
        return layout.getHeight();
    }

    private int computeTextCount(CharSequence source, TextPaint paint, int width){
        // Draw using a static layout
        StaticLayout layout = new StaticLayout(source, paint, width, Layout.Alignment.ALIGN_NORMAL, mSpacingMul, mSpacingAdd, false);
        return layout.getLineCount();
    }

    private boolean takeSizeChange(){
        if (mSizeChange){
            mSizeChange = false;
            return true;
        }
        return false;
    }
}
