package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.activity.presenter.DaysRemainingWidgetActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.FileReceiveActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.FragmentActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.IntroducingActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.LoginActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.MainActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.PikaActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.ScheduleLessonsWidgetConfigureActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.ShortcutReceiverActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.TimeRemainingWidgetActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.UniversityPersonCardActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.WebViewActivityPresenter;
import com.bukhmastov.cdoitmo.activity.presenter.impl.DaysRemainingWidgetActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.FileReceiveActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.FragmentActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.IntroducingActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.LoginActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.MainActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.PikaActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.ScheduleLessonsWidgetConfigureActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.ShortcutReceiverActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.TimeRemainingWidgetActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.UniversityPersonCardActivityPresenterImpl;
import com.bukhmastov.cdoitmo.activity.presenter.impl.WebViewActivityPresenterImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ActivityPresenterModule {

    @Provides
    @Singleton
    public MainActivityPresenter provideMainActivityPresenter() {
        return new MainActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public LoginActivityPresenter provideLoginActivityPresenter() {
        return new LoginActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public FragmentActivityPresenter provideFragmentActivityPresenter() {
        return new FragmentActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public IntroducingActivityPresenter provideIntroducingActivityPresenter() {
        return new IntroducingActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public FileReceiveActivityPresenter provideFileReceiveActivityPresenter() {
        return new FileReceiveActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public PikaActivityPresenter providePikaActivityPresenter() {
        return new PikaActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public WebViewActivityPresenter provideWebViewActivityPresenter() {
        return new WebViewActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityPersonCardActivityPresenter provideUniversityPersonCardActivityPresenter() {
        return new UniversityPersonCardActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public ShortcutReceiverActivityPresenter provideShortcutReceiverActivityPresenter() {
        return new ShortcutReceiverActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public ScheduleLessonsWidgetConfigureActivityPresenter provideScheduleLessonsWidgetConfigureActivityPresenter() {
        return new ScheduleLessonsWidgetConfigureActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public DaysRemainingWidgetActivityPresenter provideDaysRemainingWidgetActivityPresenter() {
        return new DaysRemainingWidgetActivityPresenterImpl();
    }

    @Provides
    @Singleton
    public TimeRemainingWidgetActivityPresenter provideTimeRemainingWidgetActivityPresenter() {
        return new TimeRemainingWidgetActivityPresenterImpl();
    }
}
