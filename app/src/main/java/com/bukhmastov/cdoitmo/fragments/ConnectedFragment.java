package com.bukhmastov.cdoitmo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activities.ConnectedActivity;
import com.bukhmastov.cdoitmo.utils.Log;

public abstract class ConnectedFragment extends Fragment {

    private static final String TAG = "ConnectedFragment";
    protected ConnectedActivity activity = null;
    protected Bundle extras = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            activity = (ConnectedActivity) context;
        } catch (ClassCastException e) {
            Log.wtf(TAG, context.toString() + " must implement ConnectedActivity");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extras = getArguments();
    }

    public static Data getData(Context context, Class connectedFragment) {
        if (connectedFragment == ERegisterFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == ProtocolFragment.class) return new Data(connectedFragment, context.getString(R.string.protocol_changes), R.drawable.ic_protocol_changes);
        if (connectedFragment == RatingFragment.class) return new Data(connectedFragment, context.getString(R.string.rating), R.drawable.ic_rating);
        if (connectedFragment == Room101Fragment.class) return new Data(connectedFragment, context.getString(R.string.room101), R.drawable.ic_room101);
        if (connectedFragment == ScheduleExamsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_exams), R.drawable.ic_schedule_exams);
        if (connectedFragment == ScheduleLessonsFragment.class) return new Data(connectedFragment, context.getString(R.string.schedule_lessons), R.drawable.ic_schedule_lessons);
        if (connectedFragment == SubjectShowFragment.class) return new Data(connectedFragment, context.getString(R.string.e_journal), R.drawable.ic_e_journal);
        if (connectedFragment == RatingListFragment.class) return new Data(connectedFragment, context.getString(R.string.top_rating), R.drawable.ic_rating);
        if (connectedFragment == ScheduleLessonsModifyFragment.class) return new Data(connectedFragment, context.getString(R.string.lesson_creation), R.drawable.ic_schedule_lessons);
        if (connectedFragment == ShortcutCreateFragment.class) return new Data(connectedFragment, context.getString(R.string.add_shortcut), R.drawable.ic_shortcut);
        Log.wtf(TAG, "getData | fragment class (" + connectedFragment.toString() + ") does not supported!");
        return null;
    }

    public static class Data {
        public Class connectedFragmentClass;
        public String title;
        public Integer image;
        public Data(Class connectedFragmentClass, String title, Integer image){
            this.connectedFragmentClass = connectedFragmentClass;
            this.title = title;
            this.image = image;
        }
    }

}
