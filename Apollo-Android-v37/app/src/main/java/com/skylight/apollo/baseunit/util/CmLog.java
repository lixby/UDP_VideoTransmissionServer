package com.skylight.apollo.baseunit.util;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

/**
 */

public class CmLog {

    public static final String TAG = "cm";
    public static final String[] FORMAT_STRING = {"%d", "%b", "%f", "%l"};

    private static boolean LOGGING_ENABLED = true;

    private static final int STACK_TRACE_LEVELS_UP = 5;
    

    public static void logEnable(boolean open) {
		LOGGING_ENABLED = open;
	}

	public static void verbose(String message) {
        if (LOGGING_ENABLED) {
            Log.v(TAG, getMethodName() + message + getClassNameMethodNameAndLineNumber());
        }
    }

    public static void debug(String message) {
        if (LOGGING_ENABLED) {
            Log.d(TAG, getMethodName() + message + getClassNameMethodNameAndLineNumber());
        }
    }

    public static void info(String message) {
        if (LOGGING_ENABLED) {
            Log.i(TAG, getMethodName() + message + getClassNameMethodNameAndLineNumber());
        }
    }

    public static void error(String message) {
        if (LOGGING_ENABLED) {
            Log.e(TAG, getMethodName() + message + getClassNameMethodNameAndLineNumber());
        }
    }

    private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getLineNumber();
    }

    private static String getClassName() {
        String fileName = Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getFileName();
        return fileName.substring(0, fileName.length() - 5);
    }

    private static String getMethodName() {
        return Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getMethodName() + " ";
    }

    public static String getClassNameMethodNameAndLineNumber() {
        return " (" + getClassName() + ".java:" + getLineNumber() + ")";
    }

    public static void d(String msg, Object... objects) {
        if (!LOGGING_ENABLED)
            return;
        d(String.format(wrapSMS(msg), wrapParams(objects)));
    }


    public static void i(String msg, Object... objects) {
        if (!LOGGING_ENABLED)
            return;
        i(String.format(wrapSMS(msg), wrapParams(objects)));
    }

    public static void e(String msg, Object... objects) {
        if (!LOGGING_ENABLED)
            return;
        e(String.format(wrapSMS(msg), wrapParams(objects)));
    }

    private static Object[] wrapParams(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            objects[i] = String.valueOf(objects[i]);
        }
        return objects;
    }

    public static void w(String msg) {
        if (!LOGGING_ENABLED)
            return;
        Log.w(TAG, formatSMS(msg));
    }

    public static void d(String msg) {
        if (!LOGGING_ENABLED)
            return;
        Log.d(TAG, formatSMS(msg));
    }

    public static void d(int code) {
        if (!LOGGING_ENABLED)
            return;
        String sb = "";
        if(code>=0){
            sb = "command  is success";
        }else{
            sb = "command  is failed";
        }
        error(sb);
    }

    public static void d(int code,String value) {
        if (!LOGGING_ENABLED)
            return;
        StringBuffer sb = new StringBuffer();
        if(code>=0){
            sb.append("command  is success");
            sb.append(" value is "+value);
        }else{
            sb.append( "command  is failed");
        }
        error(sb.toString());
    }

    public static void i(String msg) {
        if (!LOGGING_ENABLED)
            return;

        Log.i(TAG, formatSMS(msg));
    }

    public static void isMainThread() {
        if (!LOGGING_ENABLED)
            return;

        Log.i(TAG, formatSMS("isMainThread: " + (Looper.myLooper() == Looper.getMainLooper())));
    }

    public static void e(String msg) {
        if (!LOGGING_ENABLED)
            return;

        Log.e(TAG, formatSMS(msg));
    }

    public static void e(Throwable e) {
        if (!LOGGING_ENABLED)
            return;

        Log.e(TAG, formatSMS(e != null ? e.getMessage() : "Un-know Exception"));
    }

    private static String wrapSMS(String msg) {
        for (String temp : FORMAT_STRING)
            msg = msg.replaceAll(temp, "%s");
        return msg;
    }

    private static String formatSMS(String sms) {
        String className = null;
        int lineNumber = 0;
        String methodName = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        try {
            for (int i = 0; i < stackTrace.length; i++) {

                if (TextUtils.equals(CmLog.class.getName(), stackTrace[i].getClassName()) && !TextUtils.equals(CmLog.class.getName(), stackTrace[i + 1].getClassName())) {
                    className = stackTrace[i + 1].getFileName();
                    lineNumber = stackTrace[i + 1].getLineNumber();
                    methodName = stackTrace[i + 1].getMethodName();
                    break;
                }
            }
        } catch (Exception e) {
        }
        return methodName + " " + sms + " - (" + className + ":" + lineNumber + ")";

    }
}