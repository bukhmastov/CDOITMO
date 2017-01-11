package com.bukhmastov.cdoitmo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler oldHandler;
    private Context context;

    MyUncaughtExceptionHandler(Context context) {
        oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("--------Device--------").append("\n");
        stringBuilder.append("DEVICE: ").append(Build.DEVICE).append("\n");
        stringBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
        stringBuilder.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
        stringBuilder.append("DISPLAY: ").append(Build.DISPLAY).append("\n");
        stringBuilder.append("SDK_INT: ").append(Build.VERSION.SDK_INT).append("\n");
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            stringBuilder.append("--------Application--------").append("\n");
            stringBuilder.append(pInfo.versionName).append(" (").append(pInfo.versionCode).append(")").append("\n");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        stringBuilder.append("--------Stack trace--------").append("\n");
        stringBuilder.append(throwable.getMessage()).append("\n");
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for(StackTraceElement element : stackTrace) stringBuilder.append("at ").append(element.toString()).append("\n");
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "bukhmastov-alex@ya.ru" });
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "CDO ITMO - error report");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, stringBuilder.toString());
        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.error_choose_program)));
        if(oldHandler != null) {
            oldHandler.uncaughtException(thread, throwable);
        } else {
            System.exit(1);
        }
    }
}