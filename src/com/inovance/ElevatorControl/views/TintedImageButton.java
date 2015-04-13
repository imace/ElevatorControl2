package com.inovance.elevatorcontrol.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.inovance.elevatorcontrol.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-1-6.
 * Time: AM 10:19.
 */
public class TintedImageButton extends ImageButton {

    public TintedImageButton(Context context) {
        super(context);
    }

    public TintedImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TintedImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        ColorStateList list = getResources().getColorStateList(R.color.button_tint_color);
        int color = list.getColorForState(getDrawableState(), Color.TRANSPARENT);
        setColorFilter(color);
        invalidate();
    }

}
