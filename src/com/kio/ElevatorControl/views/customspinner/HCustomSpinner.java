package com.kio.ElevatorControl.views.customspinner;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.kio.ElevatorControl.R;

public class HCustomSpinner extends Button {

    protected PopupWindow popup = null;

    private HCustomSpinner topButton;

    protected CornerListView mListView;

    private ArrowView arrow;

    private OnItemSelectedListener changListener;

    private Context mContext;

    private HDropListener onPopup;

    /**
     * Button topButton to addView
     *
     * @param context
     * @param attrs
     */
    @SuppressLint({
            "NewApi", "Recycle"
    })
    public HCustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        topButton = this;
        initView(mContext);

        // TypedArray attributes = mContext.obtainStyledAttributes(attrs,
        // R.styleable.spinner);
        // final CharSequence text =
        // attributes.getString(R.styleable.spinner_text);
        // Log.i("text", text.toString());
        // if(text != null){
        // topButton.setText(text);
        // }
        //
        //
        // final int color = attributes.getColor(R.styleable.spinner_textColor,
        // Color.BLACK);
        // topButton.setTextColor(color);
        //
        // final int textSize =
        // attributes.getDimensionPixelSize(R.styleable.spinner_textSize, 0);
        // if(textSize > 0){
        // topButton.setTextScaleX(textSize);
        // }
        // attributes.recycle();
        // android.view.ViewGroup.LayoutParams params =
        // topButton.getLayoutParams();
        // params.width = width;
        // params.height = height;
        // topButton.setLayoutParams(params);
        //
    }

    private void initView(final Context c) {
        arrow = new ArrowView(c, null, topButton);
        topButton.setCompoundDrawables(null, null, arrow.getDrawable(), null);

        // click button text on to popupWindow
        topButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                initPopupWindow(c);
            }
        });

        topButton.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Vibrator vib = (Vibrator) v.getContext().getSystemService(Service.VIBRATOR_SERVICE);
                vib.vibrate(300);
                if (onPopup != null)
                    onPopup.onRefresh();
                return false;
            }

        });

        mListView = new CornerListView(c);
        mListView.setScrollbarFadingEnabled(false);
        mListView.setBackgroundResource(R.drawable.shape_bg_list_view);
        mListView.setCacheColorHint(0);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object obj = parent.getItemAtPosition(position);
                topButton.setText(obj.toString());
                dismiss();
                changListener.onItemSelected(parent, view, position, id);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    protected void initPopupWindow(Context context) {
        if (popup == null) {
            popup = new PopupWindow(mContext);
            popup.setWidth(topButton.getWidth());
            popup.setBackgroundDrawable(new BitmapDrawable());
            popup.setFocusable(true);
            popup.setHeight(500);
            popup.setOutsideTouchable(true);
            popup.setContentView(mListView);
        }
        if (!popup.isShowing()) {
            popup.showAsDropDown(topButton);
        }
        if (onPopup != null)
            onPopup.onPopup();
    }

    protected void dismiss() {
        if (popup.isShowing()) {
            popup.dismiss();
        }
    }

    private void setTopText(ListAdapter adapter) {
        ListAdapter mAdapter = adapter;
        String text = "";
        if (mAdapter.getCount() <= 0) {
            text = this.getResources().getString(R.string.no_selected);
            topButton.setText(text);
            return;
        } else if (topButton.getText().toString().equals("")) {
            text = (String) mAdapter.getItem(0);
            topButton.setText(text);
        }
        text = null;
    }

    public String getSelectedText() {
        return topButton.getText().toString();
    }

    public void setSelectedText(String str) {
        topButton.setText(str);
    }

    public void setAdapter(ListAdapter adapter) {
        if (mListView == null) {
            throw new NullPointerException("Listview null");
        }
        mListView.setAdapter(adapter);
        setTopText(adapter);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.changListener = listener;
    }

    public HDropListener getOnPopup() {
        return onPopup;
    }

    public void setOnPopup(HDropListener onPopup) {
        this.onPopup = onPopup;
    }

    public interface OnItemSelectedListener {

        abstract void onItemSelected(AdapterView<?> parent, View view, int position, long id);

    }

    private final class CornerListView extends ListView {

        public CornerListView(Context context) {
            super(context);
        }

        public CornerListView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public CornerListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        // @Override
        // protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        // {
        //
        // int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >>
        // 2,MeasureSpec.AT_MOST);
        // super.onMeasure(widthMeasureSpec, expandSpec);
        // }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    final int itemNum = pointToPosition(x, y);

                    if (itemNum == AbsListView.INVALID_POSITION) {
                        break;
                    } else {
                        if (itemNum == 0) {
                            if (itemNum == (getAdapter().getCount() - 1)) {
                                setSelector(R.drawable.app_list_corner_round);
                            } else {
                                setSelector(R.drawable.app_list_corner_round_top);
                            }
                        } else if (itemNum == (getAdapter().getCount() - 1)) {
                            setSelector(R.drawable.app_list_corner_round_bottom);
                        } else {
                            setSelector(R.drawable.app_list_corner_shape);
                        }
                    }

                    break;
            }
            return super.onInterceptTouchEvent(ev);
        }

    }

    @SuppressLint("WrongCall")
    protected final class ArrowView extends View {

        private int width;
        private int height;
        protected ShapeDrawable shape;

        public ArrowView(Context context, AttributeSet set, View v) {
            super(context, set);
            // this.mContext = context;
            width = 30;
            height = 20;
            Path p = new Path();
            p.moveTo(0, 0);
            p.lineTo(width, 0);
            p.lineTo(width / 2, height);
            p.lineTo(0, 0);
            shape = new ShapeDrawable(new PathShape(p, width, height));
            shape.getPaint().setColor(Color.BLACK);
            shape.setBounds(0, 0, width, height);

        }

        public void setColor(int color) {
            shape.getPaint().setColor(color);
        }

        protected Drawable getDrawable() {

            Canvas canvas = new Canvas();
            shape.draw(canvas);
            this.onDraw(canvas);
            return shape;
        }

    }
}
