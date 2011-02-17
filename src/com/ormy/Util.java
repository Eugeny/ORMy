package com.ormy;

import android.util.Log;

public final class Util {
    protected static void Log(Throwable e) {
	String TAG = "ORMy";
	Log.e(TAG, e.toString());
	for (StackTraceElement st : e.getStackTrace())
	    Log.e(TAG, st.toString());
    }
}
