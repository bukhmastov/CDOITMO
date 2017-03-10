package com.bukhmastov.cdoitmo.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.bukhmastov.cdoitmo.R;

import java.util.ArrayList;

public class ErrorTracker {
    private ArrayList<Throwable> errorList = new ArrayList<>();
    public void add(Throwable throwable){
        throwable.printStackTrace();
        errorList.add(throwable);
    }
    public int count(){
        return errorList.size();
    }
    public boolean send(Context context){
        if (count() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--------Device--------").append("\n");
            stringBuilder.append("DEVICE: ").append(Build.DEVICE).append("\n");
            stringBuilder.append("MODEL: ").append(Build.MODEL).append("\n");
            stringBuilder.append("PRODUCT: ").append(Build.PRODUCT).append("\n");
            stringBuilder.append("DISPLAY: ").append(Build.DISPLAY).append("\n");
            stringBuilder.append("SDK_INT: ").append(Build.VERSION.SDK_INT).append("\n");
            stringBuilder.append("--------Application--------").append("\n");
            stringBuilder.append(Static.versionName).append(" (").append(Static.versionCode).append(")").append("\n");
            for (Throwable throwable : errorList) {
                stringBuilder.append("--------Stack trace--------").append("\n");
                stringBuilder.append(throwable.getMessage()).append("\n");
                StackTraceElement[] stackTrace = throwable.getStackTrace();
                for (StackTraceElement element : stackTrace) stringBuilder.append("at ").append(element.toString()).append("\n");
            }
            errorList.clear();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"bukhmastov-alex@ya.ru"});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "CDO ITMO - report");
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, stringBuilder.toString());
            try {
                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.error_choose_program)));
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }
}