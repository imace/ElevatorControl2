package com.inovance.elevatorcontrol.views.component;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.inovance.elevatorcontrol.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-6-6.
 * Time: 14:00.
 */
public class SegmentControl extends LinearLayout {

    private String[] mItems;

    private int currentItem = -1;

    private static final int separatorWidthDimension = 1;

    public static interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public SegmentControl(Context context) {
        super(context);
        init();
    }

    public SegmentControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SegmentControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.setOrientation(LinearLayout.HORIZONTAL);
    }

    public void setItems(String[] items) {
        mItems = items;
        reLayout();
    }

    public void setCurrentItem(int index) {
        if (currentItem != index) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof TextView) {
                    child.setSelected(false);
                    int tag = Integer.parseInt(child.getTag().toString());
                    if (tag == index) {
                        child.setSelected(true);
                        if (mListener != null) {
                            mListener.onItemClick(child, index);
                        }
                    }
                }
            }
        }
    }

    public int getCurrentItem() {
        return currentItem;
    }

    private void reLayout() {
        removeAllViews();
        if (mItems != null && mItems.length > 0) {
            LayoutParams itemLayoutParams = new LayoutParams(0, LayoutParams.MATCH_PARENT);
            itemLayoutParams.weight = 1;
            itemLayoutParams.gravity = Gravity.CENTER;

            XmlResourceParser parser = getResources().getXml(R.color.button_text_color);
            ColorStateList colors = null;
            try {
                colors = ColorStateList.createFromXml(getResources(), parser);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            LayoutParams separatorLayoutParams = new LayoutParams(getPixelFromDimension(separatorWidthDimension),
                    LayoutParams.MATCH_PARENT);
            separatorLayoutParams.gravity = Gravity.CENTER;
            int count = mItems.length;
            for (int index = 0; index < count; index++) {
                TextView button = new TextView(getContext());
                button.setTextAppearance(getContext(), android.R.style.TextAppearance);
                button.setLayoutParams(itemLayoutParams);
                button.setText(mItems[index]);
                button.setGravity(Gravity.CENTER);
                if (colors != null) {
                    button.setTextColor(colors);
                }
                button.setTag(index);
                final int position = index;
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            if (currentItem != position) {
                                mListener.onItemClick(view, position);
                            }
                            for (int i = 0; i < getChildCount(); i++) {
                                View child = getChildAt(i);
                                if (child instanceof TextView) {
                                    child.setSelected(false);
                                }
                            }
                            view.setSelected(true);
                            currentItem = position;
                        }
                    }
                });
                addView(button);

                if (count > 1 && index != count - 1) {
                    View separator = new View(getContext());
                    separator.setBackgroundColor(0xffdfdfdf);
                    separator.setLayoutParams(separatorLayoutParams);
                    addView(separator);
                }
            }
        }
    }

    /**
     * Get Pixel From Dimension
     *
     * @param dp Dimension
     * @return Pixel
     */
    private int getPixelFromDimension(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }

}
