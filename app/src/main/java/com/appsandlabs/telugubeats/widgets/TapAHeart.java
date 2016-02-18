package com.appsandlabs.telugubeats.widgets;

import android.content.Context;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.appsandlabs.telugubeats.helpers.App;
import com.appsandlabs.telugubeats.R;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by abhinav on 2/8/16.
 */
public class TapAHeart extends ImageView implements View.OnClickListener {


    private ArrayList<ImageView> floaterHearts = new ArrayList<>();
    private View rootView;
    private float parentY;
    private float parentX;
    private long lastClick = 0;
    private Runnable pendingHeartsSender;


    private String tapId = getClass().getPackage().getName();
    private int heartsFloated=0;
    private Random random = new Random();
    private App app;

    public TapAHeart(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        this.setImageResource(R.drawable.heart_icon);
        this.app = new App(getContext());
    }


    public static class FloaterHeart extends ImageView implements Animation.AnimationListener {

        private final ViewGroup rootView;
        private Random random = new Random();
        private FloatingAnimation floatingAnimation;
        private int animationsCount = 0;

        public FloaterHeart(ViewGroup rootView , Context context, final float x , final float y ) {
            super(context);
            this.rootView = rootView;
            this.setImageResource(R.drawable.heart_icon);
            this.post(new Runnable() {
                @Override
                public void run() {
                    setX(x);
                    setY(y - getHeight() / 2);
                    bringToFront();
                    ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 1.0f,
                            0,
                            Animation.RELATIVE_TO_SELF, Animation.RELATIVE_TO_SELF, 0f
                    );
                    scaleAnimation.setDuration(1000);
                    scaleAnimation.setInterpolator(new LinearInterpolator());

                    floatingAnimation = new FloatingAnimation(true, FloaterHeart.this, random);
                    floatingAnimation.setFillEnabled(true);
                    floatingAnimation.setFillAfter(true);

                    AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                    fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
                    fadeOut.setFillAfter(true);
                    fadeOut.setStartOffset(1000);
                    fadeOut.setDuration(2000);


                    AnimationSet animationSet = new AnimationSet(false);

                    animationSet.addAnimation(scaleAnimation);
                    scaleAnimation.setAnimationListener(FloaterHeart.this);
                    animationSet.addAnimation(floatingAnimation);
                    floatingAnimation.setAnimationListener(FloaterHeart.this);

                    fadeOut.setAnimationListener(FloaterHeart.this);
                    animationSet.addAnimation(fadeOut);

                    startAnimation(animationSet);
                }
            });
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

            if(++animationsCount>=3){
                //all three animations ended
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rootView.removeView(FloaterHeart.this);
                    }
                }, 100);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }



    private static class FloatingAnimation extends Animation {
        private final FloaterHeart floaterHeart;
        private PathMeasure pm;
        float[] pos = new float[2];

        public FloatingAnimation(boolean shareInterpolator, FloaterHeart floaterHeart, Random random) {
            this.floaterHeart = floaterHeart;
            setFillAfter(true);
            setInterpolator(new DecelerateInterpolator());
            Path p = new Path();
            p.moveTo(0f, 0f);
            int distance = 300+random.nextInt(200);
            int numSteps = 3+random.nextInt(3);
            for(int i=0;i<numSteps;i++){
                p.rLineTo(-50-random.nextInt(100),-distance/numSteps);
                p.rLineTo(50+random.nextInt(10), -distance/numSteps);
            }
            pm = new PathMeasure(p, false);
            setDuration(5000);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float distance = pm.getLength() * interpolatedTime;
            pm.getPosTan(distance, pos, null);
            t.getMatrix().postTranslate(pos[0], pos[1]);
            floaterHeart.invalidate();
        }
    }




    @Override
    public void onClick(View v) {
            floatHeart(1, true);
    }

    public void floatHeart(int count, final boolean isUserClick) {

        long currentNanoTime = System.nanoTime();
        if(currentNanoTime - lastClick < 200000000 ){
            return;
        }
        lastClick = currentNanoTime;
        if(rootView==null){
            View parent = (View) getParent();
            int[] pos;
            getLocationInWindow(pos = new int[2]);
            parentX = pos[0];
            parentY = pos[1];


            while(parent!=null){
                try {
                    if (parent instanceof RelativeLayout) {
                        rootView = parent;
                    }
                    parent = (View) parent.getParent();
                }
                catch (Exception ex){
                    break;
                }
            }
        }

        if(rootView==null){
            return;
        }
        if(rootView instanceof RelativeLayout) {
            if(isUserClick) {
                floatHeart((RelativeLayout) rootView, parentX, parentY);
                heartsFloated++;
                if (pendingHeartsSender != null) {
                    getHandler().removeCallbacks(pendingHeartsSender);
                }
                getHandler().postDelayed(pendingHeartsSender = new Runnable() {
                    @Override
                    public void run() {
                        app.getServerCalls().sendHearts(heartsFloated);
                        heartsFloated = 0;
                        pendingHeartsSender = null;
                    }
                }, 1000);
            }
            else{
                int time = 0;
                for(int i=0;i<count;i++){
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            floatHeart((RelativeLayout) rootView, parentX, parentY);
                        }
                    }, time);
                    time+=random.nextInt(800);
                }
            }
        }
        else{
            Toast.makeText(getContext(), "The root must be a relative layout to display floating hearts", Toast.LENGTH_SHORT).show();
        }


        return;
    }


    private void floatHeart(RelativeLayout rootView , float x, float y) {
        FloaterHeart floatingHeart = new FloaterHeart(rootView, getContext(), x, y);
        floatingHeart.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.floaterHearts.add(floatingHeart);
        rootView.addView(floatingHeart);
    }

}
