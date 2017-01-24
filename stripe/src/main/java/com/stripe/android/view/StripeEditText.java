package com.stripe.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

import com.stripe.android.R;

/**
 * Extension of {@link EditText} that listens for users pressing the delete key when there is
 * no text present. Google has actually made this
 * <a href="https://code.google.com/p/android/issues/detail?id=42904">somewhat difficult</a>,
 * but we listen here for hardware key presses, older Android soft keyboard delete presses,
 * and modern Google Keyboard delete key presses.
 */
public class StripeEditText extends EditText {

    @Nullable private DeleteEmptyListener mDeleteEmptyListener;
    @Nullable private ColorStateList mCachedColorStateList;
    private boolean mShouldShowError;
    @ColorRes private int mColorResId;

    public StripeEditText(Context context) {
        super(context);
        listenForDeleteEmpty();
        determineErrorColor();
    }

    public StripeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        listenForDeleteEmpty();
        determineErrorColor();
    }

    public StripeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        listenForDeleteEmpty();
        determineErrorColor();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new SoftDeleteInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    /**
     * Sets a listener that can react to the user attempting to delete the empty string.
     *
     * @param deleteEmptyListener the {@link DeleteEmptyListener} to attach to this view
     */
    public void setDeleteEmptyListener(DeleteEmptyListener deleteEmptyListener) {
        mDeleteEmptyListener = deleteEmptyListener;
    }

    public void setShouldShowError(boolean shouldShowError) {
        if (!mShouldShowError && shouldShowError) {
            mCachedColorStateList = getTextColors();
            setTextColor(getResources().getColor(mColorResId));

        } else if (mShouldShowError && !shouldShowError){
            setTextColor(mCachedColorStateList);
        }

        mShouldShowError = shouldShowError;
        refreshDrawableState();
    }

    public boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114* Color.blue(color))/255;
        if(darkness>0.5){
            return false; // It's a light color
        }else{
            return true; // It's a dark color
        }
    }

    private void determineErrorColor() {
        mCachedColorStateList = getTextColors();
        int color = mCachedColorStateList.getDefaultColor();
        if (isColorDark(color)) {
            mColorResId = R.color.error_text_dark;
        } else {
            mColorResId = R.color.error_text;
        }
    }

    private void listenForDeleteEmpty() {
        // This method works for hard keyboards and older phones.
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && mDeleteEmptyListener != null
                        && length() == 0) {
                    mDeleteEmptyListener.onDeleteEmpty();
                }
                return false;
            }
        });
    }

    /**
     * A class that can listen for when the user attempts to delete the empty string.
     */
    public interface DeleteEmptyListener {
        void onDeleteEmpty();
    }

    private class SoftDeleteInputConnection extends InputConnectionWrapper {

        public SoftDeleteInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // This method works on modern versions of Android with soft keyboard delete.
            if (getTextBeforeCursor(1, 0).length() == 0 && mDeleteEmptyListener != null) {
                mDeleteEmptyListener.onDeleteEmpty();
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
