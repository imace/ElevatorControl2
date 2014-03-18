package com.kio.ElevatorControl.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.kio.ElevatorControl.R;
import org.holoeverywhere.LayoutInflater;

/**
 * Created by IntelliJ IDEA.
 * User: keith.
 * Date: 14-3-12.
 * Time: 10:46.
 */
public class DoorAnimationView extends RelativeLayout {

    private static final String TAG = DoorAnimationView.class.getSimpleName();

    private ImageView leftDoor;

    private ImageView rightDoor;

    private int DOOR_OPEN = 1;

    private int DOOR_CLOSE = 2;

    private int doorStatus;

    public boolean animating;

    public DoorAnimationView(Context context) {
        super(context);
    }

    public DoorAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.animation_view, this, true);
        leftDoor = (ImageView) findViewById(R.id.left_door);
        rightDoor = (ImageView) findViewById(R.id.right_door);
        animating = false;
        doorStatus = DOOR_CLOSE;
    }

    public DoorAnimationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
