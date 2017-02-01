package com.github.gmazzo.unicorn;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnimRes;
import android.support.annotation.ArrayRes;
import android.support.annotation.IntRange;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import com.github.gmazzo.ResourceUtils;

public class UnicornView extends View implements Runnable, View.OnClickListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Drawable walkSprite[];
    private int walkSteps;
    private long walkInterval;
    private Drawable staringSprite[];
    private long staringInterval;
    private Drawable dieSprite[];
    private long dieInterval;
    private Animation showAnimation;
    private Animation hideAnimation;
    private int dieClicks;
    private State state;
    private int step;
    private int clickCount;

    public UnicornView(Context context) {
        super(context);

        initialize(context, null, 0, R.style.UnicornView);
    }

    public UnicornView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context, attrs, 0, R.style.UnicornView);
    }

    public UnicornView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initialize(context, attrs, defStyleAttr, R.style.UnicornView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public UnicornView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.UnicornView, defStyleAttr, defStyleRes);
            try {
                setWalkSprite(ta.getResourceId(R.styleable.UnicornView_walkSprite, 0));
                setWalkSteps(ta.getInt(R.styleable.UnicornView_walkSteps, 0));
                setWalkInterval(ta.getInt(R.styleable.UnicornView_walkInterval, 0));
                setStaringSprite(ta.getResourceId(R.styleable.UnicornView_staringSprite, 0));
                setStaringInterval(ta.getInt(R.styleable.UnicornView_staringInterval, 0));
                setDieClicks(ta.getInt(R.styleable.UnicornView_dieClicks, 0));
                setDieSprite(ta.getResourceId(R.styleable.UnicornView_dieSprite, 0));
                setDieInterval(ta.getInt(R.styleable.UnicornView_dieInterval, 0));
                setShowAnimation(ta.getResourceId(R.styleable.UnicornView_showAnimation, 0));
                setHideAnimation(ta.getResourceId(R.styleable.UnicornView_hideAnimation, 0));

            } finally {
                ta.recycle();
            }
        }

        computeSpriteSize();
        setClickable(true);
        setOnClickListener(this);
    }

    private void computeSpriteSize() {
        int size[] = {0, 0};

        computeSpriteSize(size, walkSprite);
        computeSpriteSize(size, staringSprite);
        computeSpriteSize(size, dieSprite);

        setMinimumWidth(size[0]);
        setMinimumHeight(size[1]);
    }

    private void computeSpriteSize(int size[], Drawable drawables[]) {
        if (drawables != null) {
            for (Drawable drawable : drawables) {
                size[0] = Math.max(size[0], drawable.getIntrinsicWidth());
                size[1] = Math.max(size[1], drawable.getIntrinsicHeight());
            }
        }
    }

    private void reset() {
        state = State.WALKING;
        step = -1;
        clickCount = 0;
    }

    public void start() {
        reset();

        setVisibility(VISIBLE);

        Animation anim = getShowAnimation();
        if (anim != null) {
            clearAnimation();
            startAnimation(anim);
        }

        handler.post(this);
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        Drawable[] sprite = state.getSprite(this);
        if (step >= sprite.length - 1) { // end of sprite
            switch (state) {
                case STARING:
                    state = State.WALKING;
                    sprite = state.getSprite(this);
                    break;

                case DYING:
                    stop();
                    setVisibility(GONE);

                    Animation anim = getHideAnimation();
                    if (anim != null) {
                        clearAnimation();
                        startAnimation(anim);
                    }
                    return;
            }
        }

        step = (step + 1) % sprite.length;
        int imgWidth = sprite[step].getBounds().width();
        int width = getWidth() - imgWidth;
        float stepWidth = width / walkSteps;
        float transX = getTranslationX();
        boolean backward = getScaleX() < 0;

        if (state == State.WALKING) {
            if (transX < -width) {
                transX = 0;
                setScaleX(-1);

            } else if (transX > width) {
                transX = 0;
                setScaleX(1);
            }

            setTranslationX(transX + (backward ? stepWidth : -stepWidth));
        }

        invalidate();

        handler.postDelayed(this, state.getInterval(this));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        Drawable drawable = state.getSprite(this)[Math.max(step, 0)];
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        int left = viewWidth - width;
        int top = viewHeight - height;

        drawable.setBounds(left, top, viewWidth, viewHeight);
        drawable.draw(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getVisibility() == VISIBLE) {
            start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stop();
    }

    @Override
    public void onClick(View view) {
        if (state == State.WALKING) {
            handler.removeCallbacks(this);

            setClickCount(getClickCount() + 1);

            state = isDead() ? State.DYING : State.STARING;
            step = -1;

            run();
        }
    }

    /**
     * We require this hack because editor doesn't support {@link Resources#obtainTypedArray(int)}
     */
    private Drawable[] getDrawableArray(@ArrayRes int arrayRes) {
        return isInEditMode() ? new Drawable[]{getResources().getDrawable(R.drawable.unicorn1)} :
                ResourceUtils.getDrawableArray(getResources(), arrayRes);
    }

    public int getClickCount() {
        return clickCount;
    }

    protected void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public boolean isDead() {
        int max = getDieClicks();
        return max > 0 && getClickCount() >= max;
    }

    public Drawable[] getWalkSprite() {
        return walkSprite;
    }

    public void setWalkSprite(Drawable[] walkSprite) {
        if (walkSprite == null || walkSprite.length == 0) {
            throw new IllegalArgumentException("walkSprite must have elements");
        }

        this.walkSprite = walkSprite;
    }

    public void setWalkSprite(@ArrayRes int arrayRes) {
        setWalkSprite(getDrawableArray(arrayRes));
    }

    public int getWalkSteps() {
        return walkSteps;
    }

    public void setWalkSteps(@IntRange(from = 1) int walkSteps) {
        if (walkSteps < 0) {
            throw new IllegalArgumentException("walkSteps must be >= 0");
        }

        this.walkSteps = walkSteps;
    }

    public long getWalkInterval() {
        return walkInterval;
    }

    public void setWalkInterval(@IntRange(from = 1) long walkInterval) {
        if (walkInterval < 0) {
            throw new IllegalArgumentException("walkInterval must be >= 0");
        }

        this.walkInterval = walkInterval;
    }

    public Drawable[] getStaringSprite() {
        return staringSprite;
    }

    public void setStaringSprite(Drawable[] staringSprite) {
        if (staringSprite == null || staringSprite.length == 0) {
            throw new IllegalArgumentException("staringSprite must have elements");
        }

        this.staringSprite = staringSprite;
    }

    public void setStaringSprite(@ArrayRes int arrayRes) {
        setStaringSprite(getDrawableArray(arrayRes));
    }

    public long getStaringInterval() {
        return staringInterval;
    }

    public void setStaringInterval(@IntRange(from = 1) long staringInterval) {
        if (staringInterval < 0) {
            throw new IllegalArgumentException("staringInterval must be >= 0");
        }

        this.staringInterval = staringInterval;
    }

    public Drawable[] getDieSprite() {
        return dieSprite;
    }

    public void setDieSprite(@ArrayRes int arrayRes) {
        setDieSprite(getDrawableArray(arrayRes));
    }

    public void setDieSprite(Drawable[] dieSprite) {
        if (dieSprite == null || dieSprite.length == 0) {
            throw new IllegalArgumentException("dieSprite must have elements");
        }

        this.dieSprite = dieSprite;
    }

    public int getDieClicks() {
        return dieClicks;
    }

    public void setDieClicks(@IntRange(from = 0) int dieClicks) {
        if (dieClicks < 0) {
            throw new IllegalArgumentException("dieClicks must be > 0");
        }

        this.dieClicks = dieClicks;
    }

    public long getDieInterval() {
        return dieInterval;
    }

    public void setDieInterval(@IntRange(from = 1) long dieInterval) {
        if (dieInterval < 0) {
            throw new IllegalArgumentException("dieInterval must be >= 0");
        }

        this.dieInterval = dieInterval;
    }

    public Animation getShowAnimation() {
        return showAnimation;
    }

    public void setShowAnimation(Animation showAnimation) {
        this.showAnimation = showAnimation;
    }

    public void setShowAnimation(@AnimRes int animRes) {
        setShowAnimation(ResourceUtils.getAnimation(getContext(), animRes));
    }

    public Animation getHideAnimation() {
        return hideAnimation;
    }

    public void setHideAnimation(Animation hideAnimation) {
        this.hideAnimation = hideAnimation;
    }

    public void setHideAnimation(@AnimRes int animRes) {
        setHideAnimation(ResourceUtils.getAnimation(getContext(), animRes));
    }

    enum State {
        WALKING,
        STARING,
        DYING;

        Drawable[] getSprite(UnicornView view) {
            switch (this) {
                case WALKING:
                    return view.walkSprite;

                case STARING:
                    return view.staringSprite;

                case DYING:
                    return view.dieSprite;
            }
            throw new IllegalArgumentException(toString());
        }

        long getInterval(UnicornView view) {
            switch (this) {
                case WALKING:
                    return view.walkInterval;

                case STARING:
                    return view.staringInterval;

                case DYING:
                    return view.dieInterval;
            }
            throw new IllegalArgumentException(toString());
        }

    }

}
