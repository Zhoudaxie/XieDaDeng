/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.systemui.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateView extends TextView {
    private static final String TAG = "DateView";

    private final Date mCurrentTime = new Date();

    private SimpleDateFormat mDateFormat;
    private String mLastText;
    private String mDatePattern;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    // need to get a fresh date format
                    mDateFormat = null;
                }
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DateView,
                0, 0);

        try {
            mDatePattern = a.getString(R.styleable.DateView_datePattern);
        } finally {
            a.recycle();
        }
        if (mDatePattern == null) {
            mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, null);
        // SPRD: Bug 425041 - Display year.
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.DATE_FORMAT), false,
                mDateFormatObserver);

        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDateFormat = null; // reload the locale next time
        getContext().unregisterReceiver(mIntentReceiver);
        // SPRD: Bug 425041 - Display year.
        getContext().getContentResolver().unregisterContentObserver(mDateFormatObserver);
    }

    /*
     * SPRD: Bug 425041 - Display year. @{
     */
    protected void updateClock() {

        final Context context = getContext();
    	String dateFormat = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
    	String tempFormat = "M/d/yyyy";
    	final Locale l = Locale.getDefault();
    	
    	
          //Log.e("xdd","dateFormat =" +dateFormat);
    	if (dateFormat != null && !dateFormat.equals("")) {
    		if (dateFormat.equals("MM-dd-yyyy") || dateFormat.equals("dd-MM-yyyy") || dateFormat.equals("yyyy-MM-dd")) {
    			tempFormat = dateFormat.replaceAll("-", "/");
    		} else if (dateFormat.equals("EE-MMM-d-yyyy")) {
    			tempFormat = "EE, MMM d yyyy";
    		} else if (dateFormat.equals("EE-d-MMM-yyyy")) {
    			tempFormat = "EE, d MMM yyyy";
    		} else if (dateFormat.equals("yyyy-MMM-d-EE")) {
    			tempFormat = "yyyy MMM d, EE";
    		} else if (dateFormat.equals("yyyy, MMM dd, EEE")){
				tempFormat = "yyyy MMM dd, EEE";
			} else if (dateFormat.equals("EEE, dd MMM, yyyy")){
				tempFormat = "EEE, dd MMM yyyy";
			} else {
                   tempFormat = dateFormat;
                } 
			//Log.e("xdd","tempFormat =" +tempFormat);	
    	}
    	mDateFormat = new SimpleDateFormat(tempFormat, l);
    	
    	mCurrentTime.setTime(System.currentTimeMillis());
    	final String text = mDateFormat.format(mCurrentTime);
    	
      //  if (force || !text.equals(mLastText)) {
            setText(text);
            mLastText = text;
      //  }

    /* @} */
    }
    /*
     * SPRD: Bug 425041 - Display year. @{
     */
    private boolean displayYear = false;
    private boolean change = false;

    /**
     * @hide
     */
    public void displayYear() {
        displayYear = true;
        change = true;
    }

    private ContentObserver mDateFormatObserver = new ContentObserver(getHandler()) {

        @Override
        public void onChange(boolean selfChange) {
            // TODO Auto-generated method stub
            Log.e(TAG, "DATE_FORMAT changes !");
            change = true;
            updateClock();
            super.onChange(selfChange);
        }
    };
    /* @} */
}
