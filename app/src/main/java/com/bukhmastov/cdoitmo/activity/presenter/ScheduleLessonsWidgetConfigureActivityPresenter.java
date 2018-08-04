package com.bukhmastov.cdoitmo.activity.presenter;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.activity.ScheduleLessonsWidgetConfigureActivity;

public interface ScheduleLessonsWidgetConfigureActivityPresenter {

    void setActivity(@NonNull ScheduleLessonsWidgetConfigureActivity activity);

    void onCreate(@Nullable Bundle savedInstanceState);

    void onDestroy();

    class Settings {
        public static class Schedule {
            public static String query = "";
            public static String title = "";
        }
        public static class Theme {
            public static String text = "#FFFFFF";
            public static String background = "#000000";
            public static int opacity = 150;
        }
        public static int updateTime = 168;
        public static boolean useShiftAutomatic = true;
    }

    class Default {
        public static class Schedule {
            public static final String query = "";
            public static final String title = "";
        }
        public static class Theme {
            public static final class Dark {
                public static final String text = "#FFFFFF";
                public static final String background = "#000000";
                public static final int opacity = 150;
            }
            public static final class Light {
                public static final String text = "#000000";
                public static final String background = "#FFFFFF";
                public static final int opacity = 150;
            }
        }
        public static final int updateTime = 168;
        public static boolean useShiftAutomatic = true;
    }
}
