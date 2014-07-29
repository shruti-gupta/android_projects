/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.scrolltricks;

import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;

public class QuickReturnFragment extends Fragment implements
		ObservableScrollView.Callbacks {
	private static final int STATE_ONSCREEN = 0;
	private static final int STATE_OFFSCREEN = 1;
	private static final int STATE_RETURNING = 2;
	private Button mgoToTop;
	private TextView mQuickReturnView;
	private TextView mQuickReturnViewFooter;
	private View mPlaceholderView;
	private ObservableScrollView mObservableScrollView;
	private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();
	private int mMinRawY = 0;
	private int mMinRawYFooter = 0;
	private int mState = STATE_ONSCREEN;
	private int mStateFooter = STATE_ONSCREEN;
	private int mQuickReturnHeight;
	private int mQuickReturnHeightFooter;
	private int mMaxScrollY;
	private int mScrollY = 0;
	private TranslateAnimation anim;

	public QuickReturnFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(
				R.layout.fragment_content, container, false);

		mObservableScrollView = (ObservableScrollView) rootView
				.findViewById(R.id.scroll_view);
		mObservableScrollView.setCallbacks(this);
		mgoToTop = (Button) rootView.findViewById(R.id.goToTop);
		mQuickReturnView = (TextView) rootView.findViewById(R.id.sticky);
		mQuickReturnViewFooter = (TextView) rootView.findViewById(R.id.footer);
		mPlaceholderView = rootView.findViewById(R.id.placeholder);
		mObservableScrollView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						onScrollChanged(0, mObservableScrollView.getScrollY(),
								0, 0);
						mMaxScrollY = mObservableScrollView
								.computeVerticalScrollRange()
								- mObservableScrollView.getHeight();
						mQuickReturnHeight = mQuickReturnView.getHeight();
						mQuickReturnHeightFooter = mQuickReturnViewFooter
								.getHeight();
					}
				});

		mgoToTop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mObservableScrollView.setScrollY(0);
			}
		});

		return rootView;
	}

	@Override
	public void onScrollChanged(int scrollX, int scrollY, int oldl, int oldt) {
		scrollY = Math.min(mMaxScrollY, scrollY);

		boolean scrollUp = (scrollY > oldt);
		boolean scrollDown = (scrollY < oldt);

		if (scrollDown) {
			Log.d("Check scroll", "Scroll Down");
			if (isWindowScrolled())
				mgoToTop.setVisibility(View.VISIBLE);
			else
				mgoToTop.setVisibility(View.INVISIBLE);
		} else if (scrollUp)
			mgoToTop.setVisibility(View.INVISIBLE);
		mScrollSettleHandler.onScroll(scrollY);
		mScrollY = mObservableScrollView.getScrollY();
		int rawY = mPlaceholderView.getTop() - scrollY;
		int rawYFooter = mScrollY;

		int translationY = 0;
		int translationYFooter = 0;

		switch (mState) {
		case STATE_OFFSCREEN:
			if (rawY <= mMinRawY) {
				mMinRawY = rawY;
			} else {
				mState = STATE_RETURNING;
			}
			translationY = rawY;
			break;

		case STATE_ONSCREEN:
			if (rawY < -mQuickReturnHeight) {
				mState = STATE_OFFSCREEN;
				mMinRawY = rawY;
			}
			translationY = rawY;
			break;

		case STATE_RETURNING:
			translationY = (rawY - mMinRawY) - mQuickReturnHeight;
			if (translationY > 0) {
				translationY = 0;
				mMinRawY = rawY - mQuickReturnHeight;
			}

			if (rawY > 0) {
				mState = STATE_ONSCREEN;
				translationY = rawY;
			}

			if (translationY < -mQuickReturnHeight) {
				mState = STATE_OFFSCREEN;
				mMinRawY = rawY;
			}
			break;
		}
		mQuickReturnView.animate().cancel();
		mQuickReturnView.setTranslationY(translationY + scrollY);

		switch (mStateFooter) {
		case STATE_OFFSCREEN:
			if (rawYFooter >= mMinRawYFooter) {
				mMinRawYFooter = rawYFooter;
			} else {
				mStateFooter = STATE_RETURNING;
			}
			translationYFooter = rawYFooter;
			break;

		case STATE_ONSCREEN:
			if (rawYFooter > mQuickReturnHeightFooter) {
				mStateFooter = STATE_OFFSCREEN;
				mMinRawYFooter = rawYFooter;
			}
			translationYFooter = rawYFooter;
			break;

		case STATE_RETURNING:
			translationYFooter = (rawYFooter - mMinRawYFooter)
					+ mQuickReturnHeightFooter;
			System.out.println(translationYFooter);
			if (translationYFooter < 0) {
				translationYFooter = 0;
				mMinRawYFooter = rawYFooter + mQuickReturnHeightFooter;
			}

			if (rawYFooter == 0) {
				mStateFooter = STATE_ONSCREEN;
				translationYFooter = 0;
			}

			if (translationYFooter > mQuickReturnHeightFooter) {
				mStateFooter = STATE_OFFSCREEN;
				mMinRawYFooter = rawYFooter;
			}
			break;
		}

		/** this can be used if the build is below honeycomb **/
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
			anim = new TranslateAnimation(0, 0, translationYFooter,
					translationYFooter);
			anim.setFillAfter(true);
			anim.setDuration(0);
			mQuickReturnViewFooter.startAnimation(anim);
		} else {
			mQuickReturnViewFooter.animate().cancel();
			mQuickReturnViewFooter.setTranslationY(translationYFooter);
		}
	}

	@Override
	public void onDownMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(false);
	}

	@Override
	public void onCancelMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(true);
		mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
	}

	@Override
	public void onUpMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(true);
		mScrollSettleHandler.onScroll(mObservableScrollView.getScrollY());
	}

	private class ScrollSettleHandler extends Handler {
		private static final int SETTLE_DELAY_MILLIS = 100;
		private int mSettledScrollY = Integer.MIN_VALUE;
		private int mSettledScrollYFooter = Integer.MIN_VALUE;
		private boolean mSettleEnabled;

		public void onScroll(int scrollY) {
			if (mSettledScrollY != scrollY) {
				// Clear any pending messages and post delayed
				removeMessages(0);
				sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
				mSettledScrollY = scrollY;
				mSettledScrollYFooter = scrollY;
			}

		}

		public void setSettleEnabled(boolean settleEnabled) {
			mSettleEnabled = settleEnabled;
		}

		@Override
		public void handleMessage(Message msg) {
			// Handle the scroll settling.

			if (STATE_RETURNING == mState && mSettleEnabled) {
				int mDestTranslationY;
				int mDestTranslationYFooter = 0;

				if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
					mState = STATE_OFFSCREEN;
					mStateFooter = STATE_OFFSCREEN;
					mDestTranslationY = Math.max(mSettledScrollY
							- mQuickReturnHeight, mPlaceholderView.getTop());
					mDestTranslationYFooter = mSettledScrollYFooter
							- mQuickReturnHeightFooter;
				} else {
					mDestTranslationY = mSettledScrollY;
				}

				mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight
						- mDestTranslationY;
				mQuickReturnView.animate().translationY(mDestTranslationY);
				mQuickReturnViewFooter.animate().translationY(
						mDestTranslationYFooter);
			}
			mSettledScrollY = Integer.MIN_VALUE; // reset

		}
	}

	public boolean isWindowScrolled() {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int height = size.y;
		int[] location = {0,0};
		mObservableScrollView.getChildAt(0).getLocationOnScreen(location);
		int scrolled = mObservableScrollView.getTop() - location[1];
		if (scrolled >= height)
			return true;
		return false;

	}
}
