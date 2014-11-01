/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package android.support.v4.widget;
package com.usabusi.swiperefreshlayoutupdown.view;
//The reversion 21 of v4 support library has provided a API to set the distance.
//public void setDistanceToTriggerSync (int distance)
//
//http://stackoverflow.com/questions/22856754/how-to-adjust-the-swipe-down-distance-in-swiperefreshlayout/26580181#26580181


//http://stackoverflow.com/questions/22856754/how-to-adjust-the-swipe-down-distance-in-swiperefreshlayout
//http://stackoverflow.com/questions/24801963/android-swipe-refresh-layout-scroll-up-to-refresh
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 * </p>
 */
public class SwipeRefreshLayoutUpDown extends ViewGroup {
    // Maps to ProgressBar.Large style
    public static final int LARGE = MaterialProgressDrawableUpDown.LARGE;
    // Maps to ProgressBar default style
    public static final int DEFAULT = MaterialProgressDrawableUpDown.DEFAULT;

    private static final String LOG_TAG = SwipeRefreshLayoutUpDown.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mRefreshing = false;
    private int mTouchSlop;
    private float mTotalDragDistance = -1;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    // Whether or not the starting offset has been determined.
    private boolean mOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    private boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    //decelerate_interpolator
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private CircleImageViewUpDown mCircleView;
    private int mCircleViewIndex = -1;

    protected int mFrom;

    private float mStartingScale;

    protected int mOriginalOffsetTop;

    private MaterialProgressDrawableUpDown mProgress;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;
//上下运动动画
    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    private float mSpinnerFinalOffset;

    private boolean mNotify;

    private int mCircleWidth;

    private int mCircleHeight;

    // Whether the client has set a custom starting position;
    private boolean mUsingCustomStart;

    //v21 updown
    public static enum PullMode {
        /**
         * Disable all Pull-to-Refresh gesture and Refreshing handling
         */
        DISABLED(0x0),

        /**
         * Only allow the user to Pull from the start of the Refreshable View to
         * refresh. The start is either the Top or Left, depending on the
         * scrolling direction.
         */
        PULL_FROM_START(0x1),

        /**
         * Only allow the user to Pull from the end of the Refreshable View to
         * refresh. The start is either the Bottom or Right, depending on the
         * scrolling direction.
         */
        PULL_FROM_END(0x2),

        /**
         * Allow the user to both Pull from the start, from the end to refresh.
         */
        BOTH(0x3);

        static PullMode getDefault() {
            return BOTH;
        }

        boolean permitsPullToRefresh() {
            return !(this == DISABLED);
        }

        boolean permitsPullFromStart() {
            return (this == PullMode.BOTH || this == PullMode.PULL_FROM_START);
        }

        boolean permitsPullFromEnd() {
            return (this == PullMode.BOTH || this == PullMode.PULL_FROM_END);
        }

        boolean permitsPullFromStartOnly() {
            return (this == PullMode.PULL_FROM_START);
        }

        boolean permitsPullFromEndOnly() {
            return (this == PullMode.PULL_FROM_END);
        }

        boolean permitsPullFromBoth() {
            return (this == PullMode.BOTH );
        }

        private int mIntValue;

        // The modeInt values need to match those from attrs.xml
        PullMode(int modeInt) {
            mIntValue = modeInt;
        }

        int getIntValue() {
            return mIntValue;
        }

    }
    //v21 updown more
    private int mHeight = -1;
    private int mCurrentTargetOffsetBottom;
    private boolean mPullDown = false;
    private PullMode mMode = PullMode.getDefault();
    private PullMode mCurrentMode;


    public PullMode getPullMode() {
        return mMode;
    }
    public void setPullMode(PullMode mMode) {
        this.mMode = mMode;
    }
    //v21 updown csdn version
    public void setLoadNoFull(boolean load)
    {
        this.loadNoFull = load;
    }
    public PullMode getCurrentPullMode() {
        return mCurrentMode;
    }

    protected int mOriginalOffsetBottom;

	//v20 updown 下拉上推更新
    //private OnRefreshListener mListener;
	//private OnRefreshListener mOnRefreshListener;
    //private boolean mRefreshing = false;
    //private OnLoadListener mloadListener;
	private boolean mLoading = false;


    //之前手势的方向，为了解决同一个触点前后移动方向不同导致后�?个方向会刷新的问题，
    //这里Mode.DISABLED无意义，只是�?个初始�?�，和上�?/下拉方向进行区分
    private PullMode mLastDirection = PullMode.DISABLED;
    private int mDirection = 0;
    //当子控件移动到尽头时才开始计算初始点的位�?
    private float mStartPoint;
    private boolean up;
    private boolean down;
    //数据不足�?屏时是否打开上拉加载模式
    private boolean loadNoFull = false;

    private float mLastMotionY;
    //之前手势的方向，为了解决同一个触点前后移动方向不同导致后�?个方向会刷新的问题，
    //这里Mode.DISABLED无意义，只是�?个初始�?�，和上�?/下拉方向进行区分
    //private PullMode mLastDirection = PullMode.DISABLED;
   // private int mDirection = 0;
    //当子控件移动到尽头时才开始计算初始点的位�?
    //private float mStartPoint;
    //private boolean up;
    //private boolean down;
    //数据不足�?屏时是否打开上拉加载模式
    //private boolean loadNoFull = false;

    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            } else {
                mProgress.stop();
                mCircleView.setVisibility(View.GONE);
                setColorViewAlpha(MAX_ALPHA);
                // Return the circle to its start position
                if (mScale) {
                    setAnimationProgress(0 /* animation complete and view is hidden */);
                } else {
                    setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                            true /* requires update */);
                }
            }
            mCurrentTargetOffsetTop = mCircleView.getTop();
            //v21 updown loadmore
            //mCurrentTargetOffsetBottom = mCircleView.getTop();//mCircleView.getBottom();
        }
    };

/*	private Animation.AnimationListener mLoadListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();
                if (mNotify) {
                    if (mloadListener != null) {
                        mloadListener.onLoad();
                    }
                }
            } else {
                mProgress.stop();
                mCircleView.setVisibility(View.GONE);
                setColorViewAlpha(MAX_ALPHA);
                // Return the circle to its start position
                if (mScale) {
                    setAnimationProgress(0);
                } else {
                    setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                            true );
                }
            }
            mCurrentTargetOffsetTop = mCircleView.getTop();
        }
    };
	*/
    private void setColorViewAlpha(int targetAlpha) {
        mCircleView.getBackground().setAlpha(targetAlpha);
        mProgress.setAlpha(targetAlpha);
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *              progress spinner should appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mCircleView.setVisibility(View.GONE);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
        //v21 updown
        mOriginalOffsetBottom = mCurrentTargetOffsetBottom = start;
        mSpinnerFinalOffset = end;
        mUsingCustomStart = true;
        mCircleView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *              where the progress spinner is set to appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != MaterialProgressDrawableUpDown.LARGE && size != MaterialProgressDrawableUpDown.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawableUpDown.LARGE) {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
        mProgress.updateSizes(size);
        mCircleView.setImageDrawable(mProgress);
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public SwipeRefreshLayoutUpDown(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public SwipeRefreshLayoutUpDown(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mTotalDragDistance = mSpinnerFinalOffset;
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mCircleView = new CircleImageViewUpDown(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER / 2);
        mProgress = new MaterialProgressDrawableUpDown(getContext(), this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);
        mCircleView.setVisibility(View.GONE);
        addView(mCircleView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }
    //v21 updown
    //onLoadMore
	/*
    public void setOnLoadListener(OnLoadListener loadlistener) {
        mloadListener = loadlistener;
    }*/
    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            //v21 updown
            mCurrentMode = PullMode.DISABLED;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

 /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param loading Whether or not the view should show load progress.
     */
    /*
	public void setLoading(boolean loading) {
        if (loading && mRefreshing != loading) {
            // scale and show
            mLoading = loading;
            //v21 updown
            mCurrentMode = PullMode.DISABLED;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true );
            mNotify = false;
            startScaleUpAnimation(mLoadListener);
        } else {
            setLoading(loading, false );
        }
    }
	*/
    private void startScaleUpAnimation(AnimationListener listener) {
        mCircleView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mProgress.setAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mCircleView, progress);
            ViewCompat.setScaleY(mCircleView, progress);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            }
            else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }
/*
    private void setLoading(boolean loading, final boolean notify) {
        if (mLoading != loading) {
            mNotify = notify;
            ensureTarget();
            mLoading = loading;
            if (mLoading) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mLoadListener);
            } else {
                startScaleDownAnimation(mLoadListener);
            }
        }
    }
	*/
    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mCircleView.setAnimationListener(listener);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownAnimation);
    }

    private void startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    private void startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
    }

    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mProgress
                        .setAlpha((int) (startingAlpha + ((endingAlpha - startingAlpha)
                                * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mCircleView.setAnimationListener(null);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(alpha);
        return alpha;
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColor(int colorRes) {
        mCircleView.setBackgroundColor(colorRes);
        mProgress.setBackgroundColor(getResources().getColor(colorRes));
    }

    /**
     * @deprecated Use {@link #setColorSchemeResources(int...)}
     */
    @Deprecated
    public void setColorScheme(int... colors) {
        setColorSchemeResources(colors);
    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    public void setColorSchemeColors(int... colors) {
        ensureTarget();
        mProgress.setColorSchemeColors(colors);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }
/*
    public boolean isLoading() {
        return mLoading;
    }
	*/
    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mCircleView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
		//v21 updown
		//final int childTop = mCurrentTargetOffsetTop + getPaddingTop() + getPaddingBottom() + mCurrentTargetOffsetBottom;
        final int childTop = getPaddingTop() ;//+ getPaddingBottom();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        mCircleView.layout((width / 2 - circleWidth / 2), mCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentTargetOffsetTop + circleHeight);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mCircleView.getMeasuredHeight();
        }
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    //v21 updown
    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                View lastChild = absListView.getChildAt(absListView.getChildCount() - 1);
                if (lastChild != null) {
                    return (absListView.getLastVisiblePosition() == (absListView.getCount() - 1))
                            && lastChild.getBottom() > absListView.getPaddingBottom();
                } else {
                    return false;
                }
            } else {
                //return mTarget.getHeight() - mTarget.getScrollY() > 0;
                return mTarget.getScrollY() < 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

       if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
           mReturningToStart = false;
		   Log.e(LOG_TAG, "mReturningToStart = false in onInterceptTouchEvent");
        }


        if ( !isEnabled() || mReturningToStart || mRefreshing || mLoading)
        {
            boolean bisEnabled;
            bisEnabled=!isEnabled();
            String strLogout= String.format( "Got return false event in onInterceptTouchEvent.!isEnabled()= %b,mReturningToStart= %b,mRefreshing =%b,mRefreshing=%b",bisEnabled,mReturningToStart,mRefreshing,mLoading);
            Log.e(LOG_TAG, strLogout);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        if ((mMode.permitsPullFromStartOnly() && canChildScrollUp())
           || (mMode.permitsPullFromEndOnly() &&  canChildScrollDown() ) ) {
            boolean bcanChildScrollDown,bcanChildScrollUp;
            bcanChildScrollDown=canChildScrollDown();
            bcanChildScrollUp=canChildScrollUp();
            boolean bpermitsPullFromStartOnly=mMode.permitsPullFromStartOnly();
            String strLogout2= String.format( "Got return false permitsPullFromStart/End Only event in onInterceptTouchEvent.canChildScrollDown()= %b,canChildScrollUp() =%b,bpermitsPullFromStartOnly=%b",bcanChildScrollDown,bcanChildScrollUp,bpermitsPullFromStartOnly);
            Log.e(LOG_TAG, strLogout2);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        if (mMode.permitsPullFromBoth() &&
                (canChildScrollUp() &&  canChildScrollDown() ) ) {
            boolean bcanChildScrollDown,bcanChildScrollUp;
            bcanChildScrollDown=canChildScrollDown();
            bcanChildScrollUp=canChildScrollUp();
            String strLogout3= String.format( "Got return false permitsPullFromBoth event in onInterceptTouchEvent.canChildScrollDown()= %b,canChildScrollUp() =%b",bcanChildScrollDown,bcanChildScrollUp);
            Log.e(LOG_TAG, strLogout3);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //v21 updown
                mLastMotionY = mInitialMotionY = ev.getY();
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCircleView.getTop(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                final float initialMotionY = getMotionEventY(ev, mActivePointerId);
                if (initialMotionY == -1) {
                    return false;
                }
                Log.e(LOG_TAG, "Got ACTION_DOWN event in onInterceptTouchEvent.");
                mInitialMotionY = initialMotionY;

                //v21 updown csdn version
			    mStartPoint = mInitialMotionY;

                //这里用up/down记录子控件能否下拉，如果当前子控件不能上下滑动，但当手指按下并移动子控件时，控件就会变得可滑�?
                //后面的一些处理不能直接使用canChildScrollUp/canChildScrollDown
                //但仍存在问题：当数据不满�?屏且设置可以上拉模式后，多次快�?�上拉会�?发上拉加�?
                up = canChildScrollUp();
                down = canChildScrollDown();	

				break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id in onInterceptTouchEvent.");
                    return false;
                }
                else
                    Log.e(LOG_TAG, "Got ACTION_MOVE event in onInterceptTouchEvent.");
				/*
                //v20 updown
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id in onInterceptTouchEvent.");
                    return false;
                }
				*/
                final float y = getMotionEventY(ev, mActivePointerId);

                if (y == -1) {
                    return false;
                }
                //v21 updown
                //float yDiff = y - mStartPoint;
                float yDiff = y - mInitialMotionY;

                //若上个手势的方向和当前手势方向不�?致，返回
                if((mLastDirection == PullMode.PULL_FROM_START && yDiff < 0) ||
                        (mLastDirection == PullMode.PULL_FROM_END && yDiff > 0))
                {
					Log.e(LOG_TAG, "Got yDiff > 0 or <0 when ACTION_MOVE event in onInterceptTouchEvent.");
                    return false;
                }
                //下拉或上拉时，子控件本身能够滑动时，记录当前手指位置，当其滑动到尽头时，
                //mStartPoint作为下拉刷新或上拉加载的手势起点
                if ((canChildScrollUp() && yDiff > 0) || (canChildScrollDown() && yDiff < 0))
                {
                    mStartPoint = y;
                }

                //下拉
                if (yDiff > mTouchSlop)
                {
                    //若当前子控件能向下滑动，或�?�上个手势为上拉，则返回
                    if (canChildScrollUp() || mLastDirection == PullMode.PULL_FROM_END)
                    {
                        mIsBeingDragged = false;
                        return false;
                    }
                    if (mMode.permitsPullFromStart())
                    {
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                        mLastDirection = PullMode.PULL_FROM_START;
                        mCurrentMode = PullMode.PULL_FROM_START;
                        Log.e(LOG_TAG, "Got yDiff > 0 when ACTION_MOVE event in onInterceptTouchEvent.");
                        mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
                        Log.e(LOG_TAG, "Got mProgress.setAlpha(STARTING_PROGRESS_ALPHA) in ACTION_MOVE event in onInterceptTouchEvent.");
                    }
                }
                //上拉
                else if (-yDiff > mTouchSlop) {
                    //若当前子控件能向上滑动，或�?�上个手势为下拉，则返回
                    if (canChildScrollDown() || mLastDirection == PullMode.PULL_FROM_START)
                    {
                        mIsBeingDragged = false;
                        return false;
                    }
                    //若子控件不能上下滑动，说明数据不足一屏，若不满屏不加载，返回
                    if (!up && !down && !loadNoFull)
                    {
                        mIsBeingDragged = false;
                        return false;
                    }
                    if (mMode.permitsPullFromEnd())
                    {
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                        mLastDirection = PullMode.PULL_FROM_END;
                        mCurrentMode = PullMode.PULL_FROM_END;
                        yDiff = -yDiff;
                        mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
                        Log.e(LOG_TAG, "Got mProgress.setAlpha(STARTING_PROGRESS_ALPHA) in ACTION_MOVE event in onInterceptTouchEvent.");
                        Log.e(LOG_TAG, "Got yDiff < 0 when ACTION_MOVE event in onInterceptTouchEvent.");
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.e(LOG_TAG, "Got ACTION_CANCEL event in onInterceptTouchEvent.");
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
				//v21 updown csdn version
				mLastDirection = PullMode.DISABLED;
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
            Log.e(LOG_TAG, "mReturningToStart = false in onTouchEvent");
        }

        if ( !isEnabled() || mReturningToStart || mRefreshing || mLoading)
        {
            boolean bisEnabled;
            bisEnabled=!isEnabled();
            String strLogout= String.format( "Got return false event in onTouchEvent.!isEnabled()= %b,mReturningToStart= %b,mRefreshing =%b,mRefreshing=%b",bisEnabled,mReturningToStart,mRefreshing,mLoading);
            Log.e(LOG_TAG, strLogout);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        if ((mMode.permitsPullFromStartOnly() && canChildScrollUp())
                || (mMode.permitsPullFromEndOnly() &&  canChildScrollDown() ) ) {
            boolean bcanChildScrollDown,bcanChildScrollUp;
            bcanChildScrollDown=canChildScrollDown();
            bcanChildScrollUp=canChildScrollUp();
            boolean bpermitsPullFromStartOnly=mMode.permitsPullFromStartOnly();
            String strLogout2= String.format( "Got return false permitsPullFromStart/End Only event in onTouchEvent.canChildScrollDown()= %b,canChildScrollUp() =%b,bpermitsPullFromStartOnly=%b",bcanChildScrollDown,bcanChildScrollUp,bpermitsPullFromStartOnly);
            Log.e(LOG_TAG, strLogout2);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }
        /*
        if (mMode.permitsPullFromBoth() &&
                (canChildScrollUp() &&  canChildScrollDown() ) ) {
            boolean bcanChildScrollDown,bcanChildScrollUp;
            bcanChildScrollDown=canChildScrollDown();
            bcanChildScrollUp=canChildScrollUp();
            String strLogout3= String.format( "Got return false permitsPullFromBoth event in onTouchEvent.canChildScrollDown()= %b,canChildScrollUp() =%b",bcanChildScrollDown,bcanChildScrollUp);
            Log.e(LOG_TAG, strLogout3);
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }*/
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Log.e(LOG_TAG, "Got ACTION_DOWN event in onTouchEvent.");
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
				//v21 updown csdn version
                mLastMotionY = mInitialMotionY = ev.getY();

                mStartPoint = mInitialMotionY;

                up = canChildScrollUp();
                down = canChildScrollDown();
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id in in onTouchEvent.");
                    return false;
                }
                else
                    Log.e(LOG_TAG, "Got ACTION_MOVE event in onTouchEvent.");
                if (mIsBeingDragged) {
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    //v21 updown
                    //float yDiff = y - mInitialMotionY;
                    float yDiff = y - mStartPoint;
                    float overscrollTopValue = yDiff * DRAG_RATE;//< mTotalDragDistance
                    // User velocity passed min velocity; trigger a refresh
                    if (overscrollTopValue > mTotalDragDistance) {
                        // User movement passed distance; trigger a refresh
                        Log.e(LOG_TAG, "Got overscrollTopValue > mTotalDragDistance when ACTION_MOVE event in onTouchEvent.");
                        if (mLastDirection == PullMode.PULL_FROM_END) {
                            return true;
                        }
                        if (mMode.permitsPullFromStart()) {
                            mLastDirection = PullMode.PULL_FROM_START;
                            mCurrentMode = PullMode.PULL_FROM_START;
                            //startRefresh();
                        }
                    } else if (-overscrollTopValue > mTotalDragDistance) {
                        Log.e(LOG_TAG, "Got -overscrollTopValue > mTotalDragDistance when ACTION_MOVE event in onTouchEvent.");
                        if ((!up && !down && !loadNoFull) || mLastDirection == PullMode.PULL_FROM_START) {
                            return true;
                        }
                        if (mMode.permitsPullFromEnd()) {
                            mLastDirection = PullMode.PULL_FROM_END;
                            mCurrentMode = PullMode.PULL_FROM_END;
                            yDiff = -yDiff;
                            //startLoad();
                        }
                    } else {
                        Log.e(LOG_TAG, "Got overscrollTopValue between -mTotalDragDistance and mTotalDragDistancewhen ACTION_MOVE event in onTouchEvent.");
                        if (!up && !down && yDiff < 0 && !loadNoFull) {
                            return true;
                        }
                        // Just track the user's movement
                        //根据手指移动距离设置进度条显示的百分�?
//                        setTriggerPercentage(
//                                mAccelerateInterpolator.getInterpolation(
//                                        Math.abs(yDiff) / mDistanceToTriggerSync));
//                        updateContentOffsetTop((int) yDiff);
//                        if (mTarget.getTop() == getPaddingTop()) {
//                            // If the user puts the view back at the top, we
//                            // don't need to. This shouldn't be considered
//                            // cancelling the gesture as the user can restart from the top.
//                            removeCallbacks(mCancel);
//                            mLastDirection = PullMode.DISABLED;
//                        } else {
//                            mDirection = (yDiff > 0 ? 1 : -1);
//                            updatePositionTimeout();
//                        }
                    }

                    final float overscrollTop = yDiff * DRAG_RATE;
                    mProgress.showArrow(true);
                    float originalDragPercent = overscrollTop / mTotalDragDistance;
                    if (originalDragPercent < 0) {
                        Log.e(LOG_TAG, "Got originalDragPercent <  0 when ACTION_MOVE event in onTouchEvent.");
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
                    float extraOS = Math.abs(overscrollTop) - mTotalDragDistance;
                    float slingshotDist = mUsingCustomStart ? mSpinnerFinalOffset
                            - mOriginalOffsetTop : mSpinnerFinalOffset;
                    float tensionSlingshotPercent = Math.max(0,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                            (tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;

                    int targetY = mOriginalOffsetTop
                            + (int) ((slingshotDist * dragPercent) + extraMove);
                    // where 1.0f is a full circle
                    if (mCircleView.getVisibility() != View.VISIBLE) {
                        mCircleView.setVisibility(View.VISIBLE);
                    }
                    if (!mScale) {
                        ViewCompat.setScaleX(mCircleView, 1f);
                        ViewCompat.setScaleY(mCircleView, 1f);
                    }
                    if (overscrollTop < mTotalDragDistance) {
                        if (mScale) {
                            setAnimationProgress(overscrollTop / mTotalDragDistance);
                        }
                        if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                                && !isAnimationRunning(mAlphaStartAnimation)) {
                            // Animate the alpha
                            startProgressAlphaStartAnimation();
                        }
                        float strokeStart = (float) (adjustedPercent * .8f);
                        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
                        mProgress.setArrowScale(Math.min(1f, adjustedPercent));
                    } else {
                        if (mProgress.getAlpha() < MAX_ALPHA
                                && !isAnimationRunning(mAlphaMaxAnimation)) {
//								//V21 updown csdn version
//                            if (mLastDirection ==PullMode.PULL_FROM_END) {
//                                int progressBarHeight = getHeight();
//                                int progressBarBottom = getBottom();
//                                int progressBarTop = getTop();
//                                int top = 40;
//                                //http://stackoverflow.com/questions/26484907/setrefreshingtrue-does-not-show-indicator
//                                //?
//                                //https://code.google.com/p/android/issues/detail?id=77712
//                                //?
//                                //https://github.com/google/iosched/blob/master/android/src/main/java/com/google/samples/apps/iosched/ui/BaseActivity.java
////http://stackoverflow.com/questions/26484907/setrefreshingtrue-does-not-show-indicator
//                            import android.view.Display;
//                            import android.graphics.Point;
////                                Display display = getDefaultDisplay();
////                                Point size = new Point();
////                                display.getSize(size);
////                                int height = size.y;
//                                setProgressViewOffset(false, 200, 500);
//
//                                //上下移动时初始位置，高度
//                                //setProgressViewOffset(false, 300, 900);
//                                //top - progressBarBottom+500 , top+300);
//                                Log.e(LOG_TAG, "setProgressViewOffset top - progressBarBottom when ACTION_MOVE event in onTouchEvent.");
//                                invalidate();
////                            http://stackoverflow.com/questions/26493213/android-swiperefreshlayout-no-animation-on-fragment-creation/26640352#26640352
////                            It depends on which API level you're building under - if you're using up to API 20 then you can just turn on setRefreshing(true), this will run the animation in the ActionBar, but in API 21 (Material Design) they changed the progress to be a spinner than is "pulled into view" before it spins
////
////                            You have 2 ways of getting around this in API 21: 1) shift the spinner down with setProgressViewOffset(), but remember to shift it back up afterwords (note that this works in px, while setDistanceToTriggerSync() uses dp) 2) make a duplicate spinner that is displayed when you're loading the data
////
////                            The more code-efficient solution is to use the existing spinner, but you have to be careful that you do reset its position
////
////                            If you need to calculate the pixel density, you can grab it from:
////
////                            DisplayMetrics metrics = new DisplayMetrics();
////                            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
////                            float scale = metrics.density;
//                            }
                            // Animate the alpha
                            startProgressAlphaMaxAnimation();
                        }
                    }
                    float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
                    mProgress.setProgressRotation(rotation);
                    //mCurrentTargetOffsetTop-=800;
                    setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop,
                            true /* requires update */);
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                //v21 updown
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {

                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id in onTouchEvent.");
                    }
                    return false;
                }
                if ( action == MotionEvent.ACTION_UP) {
                    Log.e(LOG_TAG, "ACTION_UP in onTouchEvent");
                }
                else    if ( action == MotionEvent.ACTION_CANCEL) {
                    Log.e(LOG_TAG, "ACTION_CANCEL in onTouchEvent");
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                //float yDiff = y - mInitialMotionY;

                float yDiff = y - mStartPoint;
                //float overscrollTopValue = yDiff * DRAG_RATE;//< mTotalDragDistance
                float overscrollTop = yDiff * DRAG_RATE;
                mIsBeingDragged = false;
                // User velocity passed min velocity; trigger a refresh
                if (overscrollTop > mTotalDragDistance) {
                    // User movement passed distance; trigger a refresh
                    Log.e(LOG_TAG, "Got overscrollTopValue > mTotalDragDistance when ACTION_MOVE event in onTouchEvent.");
                    if (mLastDirection == PullMode.PULL_FROM_END) {
                        String strLogout= String.format( "Got return false  in overscrollTop= %f,mTotalDragDistance=%f",overscrollTop,mTotalDragDistance);
                        Log.e(LOG_TAG, strLogout);
                        animateStop();//return true;
                    }
                    if (mMode.permitsPullFromStart()) {
                        mLastDirection = PullMode.PULL_FROM_START;
                        mCurrentMode = PullMode.PULL_FROM_START;
                        //startRefresh();
                        setRefreshing(true, true /* notify */);
                        Log.e(LOG_TAG, "setRefreshing(true, true) ACTION_UP in onTouchEvent");
                    }
                } else if (-overscrollTop > mTotalDragDistance) {
                    Log.e(LOG_TAG, "Got -overscrollTopValue > mTotalDragDistance when ACTION_MOVE event in onTouchEvent.");
                    if ((!up && !down && !loadNoFull) || mLastDirection == PullMode.PULL_FROM_START) {
                        String strLogout= String.format( "Got return false  in overscrollTop= %f,mTotalDragDistance=%f",overscrollTop,mTotalDragDistance);
                        Log.e(LOG_TAG, strLogout);
                        animateStop();//return true;
                    }
                    if (mMode.permitsPullFromEnd()) {
                        mLastDirection = PullMode.PULL_FROM_END;
                        mCurrentMode = PullMode.PULL_FROM_END;
                        yDiff = -yDiff;
                        setRefreshing(true, true /* notify */);
                        Log.e(LOG_TAG, "setRefreshing(true, true) ACTION_UP in onTouchEvent");
                        //startLoad();
                    }
                } else {
                    Log.e(LOG_TAG, "Got overscrollTopValue between -mTotalDragDistance and mTotalDragDistancewhen ACTION_MOVE event in onTouchEvent.");
                    if (!up && !down && yDiff < 0 && !loadNoFull) {
                        String strLogout= String.format( "Got return false  in overscrollTop= %f,mTotalDragDistance=%f",overscrollTop,mTotalDragDistance);
                        Log.e(LOG_TAG, strLogout);
                        animateStop();//return true;
                    }
                    // Just track the user's movement
                    //根据手指移动距离设置进度条显示的百分�?
//                        setTriggerPercentage(
//                                mAccelerateInterpolator.getInterpolation(
//                                        Math.abs(yDiff) / mDistanceToTriggerSync));
//                        updateContentOffsetTop((int) yDiff);
//                        if (mTarget.getTop() == getPaddingTop()) {
//                            // If the user puts the view back at the top, we
//                            // don't need to. This shouldn't be considered
//                            // cancelling the gesture as the user can restart from the top.
//                            removeCallbacks(mCancel);
//                            mLastDirection = PullMode.DISABLED;
//                        } else {
//                            mDirection = (yDiff > 0 ? 1 : -1);
//                            updatePositionTimeout();
//                        }
                }

                mActivePointerId = INVALID_POINTER;
				//v21 updown csdn version
				mLastDirection = PullMode.DISABLED;
                return false;
            }
        }

        return true;
    }

    private void animateStop(){


        // cancel refresh
        mRefreshing = false;
        mProgress.setStartEndTrim(0f, 0f);
        Animation.AnimationListener listener = null;
        if (!mScale) {
            listener = new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!mScale) {
                        startScaleDownAnimation(null);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

            };
        }
        animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
        mProgress.showArrow(false);

    }
    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
            }
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mAnimateToStartPosition);
        }
    }
    //animaton circle to end
    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mCircleView.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
        }
    };

    private void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mCircleView.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = mProgress.getAlpha();
        } else {
            mStartingScale = ViewCompat.getScaleX(mCircleView);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mCircleView.bringToFront();
        mCircleView.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mCircleView.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
	 /*
    public interface OnLoadListener {
        public void onLoad();
    }
	*/
    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
        //public void onLoadMore();
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
	 /*
    private class BaseAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
	*/
}