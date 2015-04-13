package com.inovance.elevatorcontrol.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.inovance.elevatorcontrol.R;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-12.
 * Time: 10:46.
 */
public class DoorAnimationView extends LinearLayout {

    private static final String TAG = DoorAnimationView.class.getSimpleName();

    private View leftDoor;

    private View rightDoor;

    private TypefaceTextView currentFloor;

    private ImageView currentDirection;

    private int DOOR_OPEN = 1;

    private int DOOR_CLOSE = 2;

    private int doorStatus;

    public boolean animating;

    public DoorAnimationView(Context context) {
        super(context);
    }

    public DoorAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.door_animation_view, this, true);
        leftDoor = findViewById(R.id.left_door);
        rightDoor = findViewById(R.id.right_door);
        currentFloor = (TypefaceTextView) findViewById(R.id.current_floor);
        currentDirection = (ImageView) findViewById(R.id.current_direction);
        animating = false;
        doorStatus = DOOR_CLOSE;
    }

    public DoorAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    /**
     * Set current floor
     *
     * @param floor Current floor
     */
    public void setCurrentFloor(int floor) {
        currentFloor.setText(String.valueOf(floor));
    }

    /**
     * Set current direction
     * 1 电梯上行
     * 2 电梯下行
     * 3 电梯停止
     *
     * @param direction Current direction
     */
    public void setCurrentDirection(int direction) {
        switch (direction) {
            case 1:
                currentDirection.setImageResource(R.drawable.elevator_upward);
                break;
            case 2:
                currentDirection.setImageResource(R.drawable.elevator_downward);
                break;
            case 3:
                currentDirection.setImageResource(R.drawable.elevator_stop);
                break;
        }
    }

    /**
     * 开门
     */
    public void openDoor() {
        if (doorStatus == DOOR_CLOSE && !animating) {
            Animation openLeftDoor = AnimationUtils
                    .loadAnimation(DoorAnimationView.this.getContext(), R.anim.open_left_door);
            openLeftDoor.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animating = false;
                    doorStatus = DOOR_OPEN;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            Animation openRightDoor = AnimationUtils
                    .loadAnimation(DoorAnimationView.this.getContext(), R.anim.open_right_door);
            openRightDoor.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animating = false;
                    doorStatus = DOOR_OPEN;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            leftDoor.startAnimation(openLeftDoor);
            rightDoor.startAnimation(openRightDoor);
            animating = true;
        }
    }

    /**
     * 关门
     */
    public void closeDoor() {
        if (doorStatus == DOOR_OPEN && !animating) {
            Animation closeLeftDoor = AnimationUtils
                    .loadAnimation(DoorAnimationView.this.getContext(), R.anim.close_left_door);
            closeLeftDoor.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animating = false;
                    doorStatus = DOOR_CLOSE;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            Animation closeRightDoor = AnimationUtils
                    .loadAnimation(DoorAnimationView.this.getContext(), R.anim.close_right_door);
            closeRightDoor.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animating = false;
                    doorStatus = DOOR_CLOSE;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            leftDoor.startAnimation(closeLeftDoor);
            rightDoor.startAnimation(closeRightDoor);
            animating = true;
        }
    }

    /**
     * 更新状态
     *
     * @param willOpen Will Open
     */
    public void setStatus(boolean willOpen) {
        if (willOpen) {
            this.openDoor();
        } else {
            this.closeDoor();
        }
    }

}
