package com.bukhmastov.cdoitmo.object;

import android.app.Activity;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Room101AddRequest {

    interface Callback {
        void onProgress(@Stage int stage);
        void onDraw(View view);
        void onClose();
        void onDone();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            STAGE_PICK_DATE_LOAD, STAGE_PICK_DATE,
            STAGE_PICK_TIME_START_LOAD, STAGE_PICK_TIME_START,
            STAGE_PICK_TIME_END_LOAD, STAGE_PICK_TIME_END,
            STAGE_PICK_CONFIRMATION_LOAD, STAGE_PICK_CONFIRMATION,
            STAGE_PICK_CREATE, STAGE_PICK_DONE
    })
    @interface Stage {}
    int STAGE_PICK_DATE_LOAD = 0;
    int STAGE_PICK_DATE = 1;
    int STAGE_PICK_TIME_START_LOAD = 2;
    int STAGE_PICK_TIME_START = 3;
    int STAGE_PICK_TIME_END_LOAD = 4;
    int STAGE_PICK_TIME_END = 5;
    int STAGE_PICK_CONFIRMATION_LOAD = 6;
    int STAGE_PICK_CONFIRMATION = 7;
    int STAGE_PICK_CREATE = 8;
    int STAGE_PICK_DONE = 9;
    int STAGE_TOTAL = 9;

    void start(@NonNull Activity activity, @NonNull Callback callback);

    void back();

    void forward();

    void close(boolean done);

    void reset();
}
