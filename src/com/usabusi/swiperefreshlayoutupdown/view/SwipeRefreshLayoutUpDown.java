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

package com.usabusi.swiperefreshlayoutupdown.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
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
 * animation, it should call setRefreshing(true). To disable the gesture and progress
 * animation, call setEnabled(false) on the view.
 *
 * <p> This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.</p>
 */
public class SwipeRefreshLayoutUpDown extends ViewGroup {
    private static final String LOG_TAG = SwipeRefreshLayoutUpDown.class.getSimpleName();

    private static final long RETURN_TO_ORIGINAL_POSITION_TIMEOUT = 300;
    private static final float ACCELERATE_INTERPOLATION_FACTOR = 1.5f;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final float PROGRESS_BAR_HEIGHT = 4;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;
    private static final int INVALID_POINTER = -1;

    private SwipeProgressBarUpDown mProgressBar; //the thing that shows progress is going
    private SwipeProgressBarUpDown mProgressBarBottom;
    private View mTarget; //the content that gets pulled down
    private int mOriginalOffsetTop;
    private OnRefreshListener mRefreshListener;
    private OnLoadListener mLoadListener;
    private int mFrom;
    private boolean mRefreshing = false;
    private boolean mLoading = false;
    private int mTouchSlop;
    private float mDistanceToTriggerSync = -1;
    private int mMediumAnimationDuration;
    private float mFromPercentage = 0;
    private float mCurrPercentage = 0;
    private int mProgressBarHeight;
    private int mCurrentTargetOffsetTop;

    private float mInitialMotionY;
    private float mLastMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private final AccelerateInterpolator mAccelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
        android.R.attr.enabled
    };
    private Mode mMode = Mode.getDefault();
    //之前手势的方向，为了解决同一个触点前后移动方向不同导致后�?个方向会刷新的问题，
    //这里Mode.DISABLED无意义，只是�?个初始�?�，和上�?/下拉方向进行区分
    private Mode mLastDirection = Mode.DISABLED;
    private int mDirection = 0;
    //当子控件移动到尽头时才开始计算初始点的位�?
    private float mStartPoint;
    private boolean up;
    private boolean down;
    //数据不足�?屏时是否打开上拉加载模式
    private boolean loadNoFull = false;

    //对下拉或上拉进行复位
    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            if (mFrom != mOriginalOffsetTop) {
                targetTop = (mFrom + (int)((mOriginalOffsetTop - mFrom) * interpolatedTime));
            }
            int offset = targetTop - mTarget.getTop();
            //注释掉这里，不然上拉后回复原位置会很快，不平�?
//            final int currentTop = mTarget.getTop();
//            if (offset + currentTop < 0) {
//                offset = 0 - currentTop;
//            }
            setTargetOffsetTopAndBottom(offset);
        }
    };

    //设置上方进度条的完成度百分比
    private Animation mShrinkTrigger = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float percent = mFromPercentage + ((0 - mFromPercentage) * interpolatedTime);
        	mProgressBar.setTriggerPercentage(percent);
        }
    };

    //设置下方进度条的完成度百分比
    private Animation mShrinkTriggerBottom = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float percent = mFromPercentage + ((0 - mFromPercentage) * interpolatedTime);
            mProgressBarBottom.setTriggerPercentage(percent);
        }
    };

    //监听，回复初始位�?
    private final AnimationListener mReturnToStartPositionListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mCurrentTargetOffsetTop = 0;
            mLastDirection = Mode.DISABLED;
        }
    };

    //回复进度条百分比
    private final AnimationListener mShrinkAnimationListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mCurrPercentage = 0;
        }
    };

    //回复初始位置
    private final Runnable mReturnToStartPosition = new Runnable() {

        @Override
        public void run() {
            mReturningToStart = true;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(),
                    mReturnToStartPositionListener);
        }

    };

    // Cancel the refresh gesture and animate everything back to its original state.
    private final Runnable mCancel = new Runnable() {

        @Override
        public void run() {
            mReturningToStart = true;
            // Timeout fired since the user last moved their finger; animate the
            // trigger to 0 and put the target back at its original position
            if (mProgressBar != null || mProgressBarBottom != null) {
                mFromPercentage = mCurrPercentage;
                if(mDirection > 0 && ((mMode == Mode.PULL_FROM_START) || (mMode == Mode.BOTH)))
                {
	                mShrinkTrigger.setDuration(mMediumAnimationDuration);
	                mShrinkTrigger.setAnimationListener(mShrinkAnimationListener);
	                mShrinkTrigger.reset();
	                mShrinkTrigger.setInterpolator(mDecelerateInterpolator);
	                startAnimation(mShrinkTrigger);
                }
                else if(mDirection < 0 && ((mMode == Mode.PULL_FROM_END) || (mMode == Mode.BOTH)))
                {
	                mShrinkTriggerBottom.setDuration(mMediumAnimationDuration);
	                mShrinkTriggerBottom.setAnimationListener(mShrinkAnimationListener);
	                mShrinkTriggerBottom.reset();
	                mShrinkTriggerBottom.setInterpolator(mDecelerateInterpolator);
	                startAnimation(mShrinkTriggerBottom);                	
                }
            }
            mDirection = 0;
            animateOffsetToStartPosition(mCurrentTargetOffsetTop + getPaddingTop(),
                    mReturnToStartPositionListener);
        }

    };

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     * @param context
     */
    public SwipeRefreshLayoutUpDown(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     * @param context
     * @param attrs
     */
    public SwipeRefreshLayoutUpDown(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mProgressBar = new SwipeProgressBarUpDown(this);
        mProgressBarBottom = new SwipeProgressBarUpDown(this);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mAccelerateInterpolator = new AccelerateInterpolator(ACCELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mCancel);
        removeCallbacks(mReturnToStartPosition);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mReturnToStartPosition);
        removeCallbacks(mCancel);
    }

    //对子控件进行移动
    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(mMediumAnimationDuration);
        mAnimateToStartPosition.setAnimationListener(listener);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateToStartPosition);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public void setOnLoadListener(OnLoadListener listener) {
        mLoadListener = listener;
    }

    //设置进度条的显示百分�?
    private void setTriggerPercentage(float percent) {
        if (percent == 0f) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-percent
            // means we're trying to reset state, so there's nothing to reset in this case.
            mCurrPercentage = 0;
            return;
        }
        mCurrPercentage = percent;
        if (((mMode == Mode.PULL_FROM_START) || (mMode == Mode.BOTH))
        		&& mLastDirection != Mode.PULL_FROM_END && !mLoading)
		{
            mProgressBar.setTriggerPercentage(percent);	
		}
        else if(((mMode == Mode.PULL_FROM_END) || (mMode == Mode.BOTH))
        		&& mLastDirection != Mode.PULL_FROM_START && !mRefreshing)
        {
            mProgressBarBottom.setTriggerPercentage(percent);
		}
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (mRefreshing != refreshing) {
            ensureTarget();
            mCurrPercentage = 0;
            mRefreshing = refreshing;
            if (mRefreshing) {
                mProgressBar.start();
            } else {
            	mLastDirection = Mode.DISABLED;
                mProgressBar.stop();
            }
        }
    }

    public void setLoading(boolean loading) {
        if (mLoading != loading) {
            ensureTarget();
            mCurrPercentage = 0;
            mLoading = loading;
            if (mLoading) {
                mProgressBarBottom.start();
            } else {
            	mLastDirection = Mode.DISABLED;
                mProgressBarBottom.stop();
            }
        }
    }

    /**
     * @deprecated Use {@link #setColorSchemeResources(int, int, int, int)}
     */
    @Deprecated
    private void setColorScheme(int colorRes1, int colorRes2, int colorRes3, int colorRes4) {
        setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4);
    }

    /**
     * Set the four colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     */
    public void setTopColor(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4)
    {
    	setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4);
    }
    
    public void setBottomColor(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4)
    {
    	setColorSchemeResourcesBottom(colorRes1, colorRes2, colorRes3, colorRes4);
    }

    public void setColor(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4){
    	setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4);
    	setColorSchemeResourcesBottom(colorRes1, colorRes2, colorRes3, colorRes4);
    }
    
    private void setColorSchemeResources(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4) {
        final Resources res = getResources();
        setColorSchemeColors(res.getColor(colorRes1), res.getColor(colorRes2),
                res.getColor(colorRes3), res.getColor(colorRes4));
    }

    private void setColorSchemeResourcesBottom(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4) {
        final Resources res = getResources();
        setColorSchemeColorsBottom(res.getColor(colorRes1), res.getColor(colorRes2),
                res.getColor(colorRes3), res.getColor(colorRes4));
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     */
    private void setColorSchemeColors(int color1, int color2, int color3, int color4) {
        ensureTarget();
        mProgressBar.setColorScheme(color1, color2, color3, color4);
    }

    private void setColorSchemeColorsBottom(int color1, int color2, int color3, int color4) {
        ensureTarget();
        mProgressBarBottom.setColorScheme(color1, color2, color3, color4);
    }
    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    public boolean isLoading() {
        return mLoading;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid out yet.
        if (mTarget == null) {
            if (getChildCount() > 1 && !isInEditMode()) {
                throw new IllegalStateException(
                        "SwipeRefreshLayout can host only one direct child");
            }
            mTarget = getChildAt(0);
            mOriginalOffsetTop = mTarget.getTop() + getPaddingTop();
        }
        if (mDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View)getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDistanceToTriggerSync = (int) Math.min(
                        ((View) getParent()) .getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
                                REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mProgressBar.draw(canvas);
        mProgressBarBottom.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width =  getMeasuredWidth();
        final int height = getMeasuredHeight();
        mProgressBar.setBounds(0, 0, width, mProgressBarHeight);
        if (getChildCount() == 0) {
            return;
        }
        final View child = getChildAt(0);
        final int childLeft = getPaddingLeft();
        final int childTop = mCurrentTargetOffsetTop + getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        mProgressBarBottom.setBounds(0, height-mProgressBarHeight, width, height);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() > 1 && !isInEditMode()) {
            throw new IllegalStateException("SwipeRefreshLayout can host only one direct child");
        }
        if (getChildCount() > 0) {
            getChildAt(0).measure(
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
                            MeasureSpec.EXACTLY));
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
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

    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
        		View lastChild = absListView.getChildAt(absListView.getChildCount() - 1);
        		if (lastChild != null) {
        			return (absListView.getLastVisiblePosition() == (absListView.getCount() - 1))
        					&& lastChild.getBottom() > absListView.getPaddingBottom();
        		}
        		else
        		{
        			return false;
        		}
            } else {
                return mTarget.getHeight() - mTarget.getScrollY() > 0;
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
        }

        if (!isEnabled() || mReturningToStart) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mStartPoint = mInitialMotionY;

                //这里用up/down记录子控件能否下拉，如果当前子控件不能上下滑动，但当手指按下并移动子控件时，控件就会变得可滑�?
                //后面的一些处理不能直接使用canChildScrollUp/canChildScrollDown
                //但仍存在问题：当数据不满�?屏且设置可以上拉模式后，多次快�?�上拉会�?发上拉加�?
                up = canChildScrollUp();
                down = canChildScrollDown();					
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
//                final float yDiff = y - mInitialMotionY;
                final float yDiff = y - mStartPoint;
                //若上个手势的方向和当前手势方向不�?致，返回
                if((mLastDirection == Mode.PULL_FROM_START && yDiff < 0) ||
                		(mLastDirection == Mode.PULL_FROM_END && yDiff > 0))
                {
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
                	if (canChildScrollUp() || mLastDirection == Mode.PULL_FROM_END)
					{
                		mIsBeingDragged = false;
                		return false;
					}
                	if ((mMode == Mode.PULL_FROM_START) || (mMode == Mode.BOTH))
					{
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                        mLastDirection = Mode.PULL_FROM_START;
					}
                }
                //上拉
                else if (-yDiff > mTouchSlop) {
                	//若当前子控件能向上滑动，或�?�上个手势为下拉，则返回
                	if (canChildScrollDown() || mLastDirection == Mode.PULL_FROM_START)
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
                	if ((mMode == Mode.PULL_FROM_END) || (mMode == Mode.BOTH))
					{
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                        mLastDirection = Mode.PULL_FROM_END;
					}
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                mLastDirection = Mode.DISABLED;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled() || mReturningToStart) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mStartPoint = mInitialMotionY;

                up = canChildScrollUp();
                down = canChildScrollDown();					
			    break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
//                final float yDiff = y - mInitialMotionY;
                final float yDiff = y - mStartPoint;

                if((mLastDirection == Mode.PULL_FROM_START && yDiff < 0) ||
                		(mLastDirection == Mode.PULL_FROM_END && yDiff > 0))
                {
                	return true;
                }
                
                if (!mIsBeingDragged && (yDiff > 0 && mLastDirection == Mode.PULL_FROM_START)
                		|| (yDiff < 0 && mLastDirection == Mode.PULL_FROM_END)) {
                    mIsBeingDragged = true;
                }

                if (mIsBeingDragged) {
                    // User velocity passed min velocity; trigger a refresh
                    if (yDiff > mDistanceToTriggerSync) {
                        // User movement passed distance; trigger a refresh
                    	if(mLastDirection == Mode.PULL_FROM_END)
                    	{
                    		return true;
                    		
                    	}
                    	if ((mMode == Mode.PULL_FROM_START) || (mMode == Mode.BOTH))
                    	{
                    		mLastDirection = Mode.PULL_FROM_START;
                    		startRefresh();
                    	}
                    }
                    else if (-yDiff > mDistanceToTriggerSync) {
                    	if((!up && !down && !loadNoFull)  || mLastDirection == Mode.PULL_FROM_START)
                    	{
                    		return true;
                    	}
                    	if ((mMode == Mode.PULL_FROM_END) || (mMode == Mode.BOTH))
                    	{
                    		mLastDirection = Mode.PULL_FROM_END;
                    		startLoad();
                    	}
					}else {
						if (!up && !down && yDiff < 0 && !loadNoFull)
						{
							return true;
						}
						// Just track the user's movement
						//根据手指移动距离设置进度条显示的百分�?
                        setTriggerPercentage(
                                mAccelerateInterpolator.getInterpolation(
                                        Math.abs(yDiff) / mDistanceToTriggerSync));
                        updateContentOffsetTop((int)yDiff);
                        if (mTarget.getTop() == getPaddingTop()) {
                            // If the user puts the view back at the top, we
                            // don't need to. This shouldn't be considered
                            // cancelling the gesture as the user can restart from the top.
                            removeCallbacks(mCancel);
                            mLastDirection = Mode.DISABLED;
                        } else {
                        	mDirection = (yDiff > 0 ? 1 : -1);
                            updatePositionTimeout();
                        }
                    }
                    mLastMotionY = y;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                mLastDirection = Mode.DISABLED;
                return false;
        }

        return true;
    }

    private void startRefresh() {
    	if (!mLoading && !mRefreshing)
		{
            removeCallbacks(mCancel);
            mReturnToStartPosition.run();
            setRefreshing(true);
            mRefreshListener.onRefresh();			
		}
    }

    private void startLoad() {
    	if (!mLoading && !mRefreshing)
		{
            removeCallbacks(mCancel);
            mReturnToStartPosition.run();
            setLoading(true);
            mLoadListener.onLoad();	
		}
    }

    //手指移动时更新子控件的位�?
    private void updateContentOffsetTop(int targetTop) {
        final int currentTop = mTarget.getTop();
        if (targetTop > mDistanceToTriggerSync) {
            targetTop = (int) mDistanceToTriggerSync;
        }
        //注释掉，否则上拉的时候子控件会向下移�?
//        else if (targetTop < 0) {
//            targetTop = 0;
//        }
        setTargetOffsetTopAndBottom(targetTop - currentTop);
    }

    //根据偏移量对子控件进行移�?
    private void setTargetOffsetTopAndBottom(int offset) {
        mTarget.offsetTopAndBottom(offset);
        mCurrentTargetOffsetTop = mTarget.getTop();
    }

    private void updatePositionTimeout() {
        removeCallbacks(mCancel);
        postDelayed(mCancel, RETURN_TO_ORIGINAL_POSITION_TIMEOUT);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    public interface OnLoadListener {
        public void onLoad();
    }
    
    public void setMode(Mode mode)
    {
    	this.mMode = mode;
    }
    
    public void setLoadNoFull(boolean load)
    {
    	this.loadNoFull = load;
    }
    
    public static enum Mode {
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

        static Mode getDefault() {
            return BOTH;
        }

        boolean permitsPullToRefresh() {
            return !(this == DISABLED);
        }
        boolean permitsPullFromStart() {
            return (this == Mode.BOTH || this == Mode.PULL_FROM_START);
        }
        boolean permitsPullFromEnd() {
            return (this == Mode.BOTH || this == Mode.PULL_FROM_END);
        }

        private int mIntValue;

        // The modeInt values need to match those from attrs.xml
        Mode(int modeInt) {
            mIntValue = modeInt;
        }

        int getIntValue() {
            return mIntValue;
        }

    }
    
    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private class BaseAnimationListener implements AnimationListener {
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
}
