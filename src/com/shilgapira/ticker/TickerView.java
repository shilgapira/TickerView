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
    
    /** The delay before a view starts scrolling */
    public static final int PRETICK_DELAY = 3000; /* ms */
    
    /** The delay after a view finished scrolling and before switching to the next one */
    public static final int PRESWITCH_DELAY = 3000; /* ms */
    
    // Animation parameters
    public static final float TICK_STEP_SIZE = 1.0f; /* dp */
    public static final float SWITCH_STEP_SIZE = 1.0f; /* dp */
    public static final int STEP_DURATION = 16; /* ms */

    /** Views will start aligned to the left and scroll to the right */
    public static final int DIRECTION_RIGHT = 0;
    
    /** Views will start aligned to the right and scroll to the left */
    public static final int DIRECTION_LEFT = 1;

    /** Views will switch by scrolling downwards */
    public static final int DIRECTION_DOWN = 0;

    /** Views will switch by scrolling upwards */
    public static final int DIRECTION_UP = 2;

    private Adapter mAdapter;
    
    private int mCurrent;
    
    private int mDirection;
    
    private Controller mController;
    
    private float mTickOffset;
    
    private float mTickSize;
    
    private boolean mTickFinished;
    
    private float mSwitchOffset;
    
    private float mSwitchSize;
    
    private boolean mSwitchFinished;

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
        float scale = getResources().getDisplayMetrics().density;
        mTickSize = TICK_STEP_SIZE * scale;
        mSwitchSize = SWITCH_STEP_SIZE * scale;
        ensureAdapter();
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
            start();
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
    
    private void start() {
        // reset state first
        if (mController != null) {
            mController.cancel();
        }
        removeAllViews();
        resetTick();
        mCurrent = 0;
        
        // show a child view and start ticking
        if (mAdapter.getCount() > 0) {
            Log.i(TAG, "Displaying " + mAdapter.getCount() + " ticker items");
            addChild(mCurrent);
            mController = new Controller();
            mController.start();
        }
    }
    
    private void addChild(int position) {
        position %= mAdapter.getCount();
        View child = mAdapter.getView(position, null, null);
        addView(child);
    }
    
    private void resetTick() {
        mTickOffset = 0;
        mTickFinished = false;
        mSwitchOffset = 0;
        mSwitchFinished = false;
    }
    
    private void performTickStep() {
        mTickOffset += mTickSize;
        requestLayout();
    }
    
    private void prepareSwitch() {
        addChild(mCurrent + 1);
    }
    
    private void performSwitchStep() {
        mSwitchOffset += mSwitchSize;
        requestLayout();
    }
    
    private void finishSwitch() {
        mCurrent++;
        removeViewAt(0);
        resetTick();
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
        
        int switchOffset = Math.round(mSwitchOffset);
        if (switchOffset >= parentHeight) {
            switchOffset = parentHeight;
            mSwitchFinished = true;
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
                    tickOffset = Math.round(mTickOffset);
                }

                // make sure offset is sane
                int maxOffset = Math.max(0, childWidth - parentWidth);
                if (maxOffset <= tickOffset) {
                    tickOffset = maxOffset;
                    mTickFinished = true;
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
        
        private boolean mCancelled;
        
        public Controller() {
            mState = State.PRETICK;
        }
        
        public void start() {
            step();
        }
        
        public void cancel() {
            mCancelled = true;
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
                    nextState = State.TICK;
                    delay = PRETICK_DELAY;
                    break;
                    
                case TICK:
                    if (mTickFinished) {
                        nextState = State.PRESWITCH;
                    } else {
                        performTickStep();
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
                        performSwitchStep();
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
