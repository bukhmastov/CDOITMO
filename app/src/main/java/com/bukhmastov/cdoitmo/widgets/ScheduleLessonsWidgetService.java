package com.bukhmastov.cdoitmo.widgets;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ScheduleLessonsWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ScheduleLessonsWidgetFactory(getApplicationContext(), intent);
    }
}
