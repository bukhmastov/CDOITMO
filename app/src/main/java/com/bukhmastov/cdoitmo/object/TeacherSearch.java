package com.bukhmastov.cdoitmo.object;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

import com.bukhmastov.cdoitmo.model.schedule.teachers.STeachers;

public interface TeacherSearch {

    void search(String query, TeacherSearchCallback callback);

    interface TeacherSearchCallback {
        void onState(@State int state);
        void onSuccess(STeachers teachers);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REJECTED_EMPTY, REJECTED, ACCEPTED, SEARCHING, NOT_FOUND, FOUND})
    @interface State {}
    int REJECTED_EMPTY = 0;
    int REJECTED = 1;
    int ACCEPTED = 2;
    int SEARCHING = 3;
    int NOT_FOUND = 4;
    int FOUND = 5;
}
