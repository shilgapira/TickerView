package com.shilgapira.ticker;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;

/**
 * A {@code TickerView} pulls views from an {@code Adapter} and displays them one at a time.
 * The views are scrolled from side to side if they're wider than the {@code TickerView} itself.
 *
 * @author Gil Shapira
 */
public class TickerView extends ViewGroup {

    public static final String TAG = "TickerView";
    
    /** Views will start aligned to the left and scroll to the right */
    public static final int DIRECTION_RIGHT = 0;
    
    /** Views will start aligned to the right and scroll to the left */
    public static final int DIRECTION_LEFT = 1;

    /** Views will switch by scrolling downwards */
    public static final int DIRECTION_DOWN = 0;

    /** Views will switch by scrolling upwards */
    public static final int DIRECTION_UP = 2;

    /** The delay before a view starts scrolling */
    private static final int PRETICK_DELAY = 3000; /* ms */
    
    /** The delay after a view finished scrolling and before switching to the next one */
    private static final int PRESWITCH_DELAY = 3000; /* ms */
    
    /** How much dp to move the views each ticking step */
    private static final float TICK_STEP_SIZE = 1.0f; /* dp */
    
    /** How much dp to move the views each switching step */
    private static final float SWITCH_STEP_SIZE = 1.0f; /* dp */
    
    /** How long to wait between animation steps */
    private static final int STEP_DURATION = 16; /* ms */

    private int mDirection;
    
    private Adapter mAdapter;
    
    private int mCurrent;
    
    private Controller mController;
    
    /**
     * Creates a new {@code TickerView} object.
     */
    public TickerView(Context context) {
        super(context);
        init();
    }

    /**
     * Creates a new {@code TickerView} object.
     */
    public TickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Creates a new {@code TickerView} object.
     */
    public TickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    /**
     * Initialization done by every constructor.
     */
    private void init() {
        mDirection = DIRECTION_RIGHT | DIRECTION_DOWN;
        ensureAdapter();
        restart();
    }
    
    /**
     * @return the scrolling direction of the ticker.
     */
    public int getDirection() {
        return mDirection;
    }
    
    /**
     * Sets the scrolling direction of hte ticker.
     */
    public void setDirection(int direction) {
        mDirection = direction;
        restart();
    }
    
    /**
     * @return the {@code Adapter} used by the ticker. Might not be the same
     * object assigned in {@code setAdapter}.
     */
    public Adapter getAdapter() {
        return mAdapter;
    }
    
    /**
     * Sets an {@code Adapter} for this ticker.
     */
    public void setAdapter(Adapter adapter) {
        if (mAdapter != adapter) {
            mAdapter = adapter;
            ensureAdapter();
            restart();
        }
    }
    
    /**
     * Creates an empty {@code Adapter} object if one hasn't been set yet.
     */
    private void ensureAdapter() {
        if (mAdapter == null) {
            mAdapter = new ArrayAdapter<Object>(getContext(), 0);
        }
    }
    
    private void restart() {
        resetAll();
        
        // show a child view and start ticking
        if (mAdapter.getCount() > 0) {
            Log.i(TAG, "Displaying " + mAdapter.getCount() + " ticker items");
            addChild(mCurrent);
            mController.start();
        }
    }
    
    private void addChild(int position) {
        position %= mAdapter.getCount();
        View child = mAdapter.getView(position, null, null);
        addView(child);
    }
    
    private void resetAll() {
        if (mController != null) {
            mController.mCancelled = true;
        }
        mController = new Controller();
        removeAllViews();
        mCurrent = 0;
    }
    
    private void update() {
        requestLayout();
    }
    
    private void prepareSwitch() {
        addChild(mCurrent + 1);
    }
    
    private void finishSwitch() {
        mCurrent++;
        removeViewAt(0);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int heightMeasure = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.measure(MeasureSpec.UNSPECIFIED, heightMeasure);
            }
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int parentLeft = getPaddingLeft();
        int parentRight = right - left - getPaddingRight();
        int parentTop = getPaddingTop();
        int parentBottom = bottom - top - getPaddingBottom();
        
        int parentWidth = parentRight - parentLeft;
        int parentHeight = parentBottom - parentTop;
        
        int switchOffset = Math.round(mController.mSwitchOffset);
        if (switchOffset >= parentHeight) {
            switchOffset = parentHeight;
            mController.mSwitchFinished = true;
        }
        
        // the switching direction shifts all views up or down
        int currentTop = parentTop;
        if ((mDirection & DIRECTION_UP) == 0) {
            currentTop -= switchOffset;
        } else {
            currentTop += switchOffset;
        }
        
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                
                // children will usually fill the layout, we center them vertically either way
                int childTop = currentTop + (parentHeight - childHeight) / 2;
                
                // default to show children aligned to the appropriate edge
                int tickOffset = 0;
                
                // the first child is always the one being ticked
                if (i == 0) {
                    tickOffset = Math.round(mController.mTickOffset);
                }

                // make sure offset is sane
                int maxOffset = Math.max(0, childWidth - parentWidth);
                if (maxOffset <= tickOffset) {
                    tickOffset = maxOffset;
                    mController.mTickFinished = true;
                }
                
                // placement depends on the ticking direction
                int childLeft;
                if ((mDirection & DIRECTION_LEFT) == 0) {
                    childLeft = parentLeft - tickOffset;
                } else {
                    childLeft = parentRight - childWidth + tickOffset;
                }

                child.layout(childLeft, currentTop, childLeft + childWidth, childTop + childHeight);
                
                // placement depends on switching direction
                if ((mDirection & DIRECTION_UP) == 0) {
                    currentTop += parentHeight;
                } else {
                    currentTop -= parentHeight;
                }
            }
        }
    }
    
    //
    // Controller
    //
    
    /**
     * The state of the ticker.
     */
    private enum State {
        
        /** New view displayed */
        PRETICK,
        
        /** Ticking current view */
        TICK,
        
        /** Finished ticking */
        PRESWITCH,
        
        /** Switching to new view */
        SWITCH,
        
    }
    
    /**
     * Simple state machine to manage the view. 
     */
    private class Controller {
        
        private State mState;
        
        private float mScale;
        
        public boolean mCancelled;
        
        public float mTickOffset;
        
        public boolean mTickFinished;
        
        public float mSwitchOffset;
        
        public boolean mSwitchFinished;

        public Controller() {
            mState = State.PRETICK;
            mScale = getResources().getDisplayMetrics().density;
        }
        
        public void start() {
            step();
        }
        
        private void step() {
            if (mCancelled) {
                return;
            }
            
            State nextState = mState;
            int delay = 0;
            
            Log.v(TAG, "Step: " + mState.name());
            
            switch (mState) {
                case PRETICK:
                    mTickOffset = 0;
                    mTickFinished = false;
                    mSwitchOffset = 0;
                    mSwitchFinished = false;
                    
                    nextState = State.TICK;
                    delay = PRETICK_DELAY;
                    break;
                    
                case TICK:
                    if (mTickFinished) {
                        nextState = State.PRESWITCH;
                    } else {
                        mTickOffset += (TICK_STEP_SIZE * mScale);
                        update();
                        delay = STEP_DURATION;
                    }
                    break;
                    
                case PRESWITCH:
                    prepareSwitch();
                    nextState = State.SWITCH;
                    delay = PRESWITCH_DELAY;
                    break;
                    
                case SWITCH:
                    if (mSwitchFinished) {
                        finishSwitch();
                        nextState = State.PRETICK;
                    } else {
                        mSwitchOffset += (SWITCH_STEP_SIZE * mScale);
                        update();
                        delay = STEP_DURATION;
                    }
                    break;
            }
            
            mState = nextState;
            if (delay == 0) {
                step();
            } else {
                schedule(delay);
            }
        }
        
        private void schedule(int delay) {
            postDelayed(mRunnable, delay);
        }
        
        private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                step();
            }
        };
        
    }
    
    //
    // ViewGroup stuffs
    //

    // @Override omitted to support older SDKs
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);        
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }
    
}
