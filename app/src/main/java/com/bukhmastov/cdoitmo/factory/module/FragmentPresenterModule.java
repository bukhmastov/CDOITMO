package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.fragment.presenter.AboutFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ERegisterFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.HomeScreenInteractionFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.LinkedAccountsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.LogFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ProtocolFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.RatingListFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.Room101FragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleAttestationsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleExamsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsModifyFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsShareFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.ScheduleLessonsTabHostFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.SubjectShowFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityBuildingsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityEventsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFacultiesFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityNewsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityPersonsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.UniversityUnitsFragmentPresenter;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.AboutFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ERegisterFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.HomeScreenInteractionFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.LinkedAccountsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.LogFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ProtocolFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.RatingFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.RatingListFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.Room101FragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleAttestationsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleExamsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleExamsTabFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleExamsTabHostFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleLessonsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleLessonsModifyFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleLessonsShareFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleLessonsTabFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ScheduleLessonsTabHostFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.SubjectShowFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityBuildingsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityEventsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityFacultiesFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityNewsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityPersonsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityUnitsFragmentPresenterImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentPresenterModule {

    @Provides
    public AboutFragmentPresenter provideAboutFragmentPresenter() {
        return new AboutFragmentPresenterImpl();
    }

    @Provides
    public ERegisterFragmentPresenter provideERegisterFragmentPresenter() {
        return new ERegisterFragmentPresenterImpl();
    }

    @Provides
    public HomeScreenInteractionFragmentPresenter provideHomeScreenInteractionFragmentPresenter() {
        return new HomeScreenInteractionFragmentPresenterImpl();
    }

    @Provides
    public LinkedAccountsFragmentPresenter provideLinkedAccountsFragmentPresenter() {
        return new LinkedAccountsFragmentPresenterImpl();
    }

    @Provides
    public LogFragmentPresenter provideLogFragmentPresenter() {
        return new LogFragmentPresenterImpl();
    }

    @Provides
    public ProtocolFragmentPresenter provideProtocolFragmentPresenter() {
        return new ProtocolFragmentPresenterImpl();
    }

    @Provides
    public RatingFragmentPresenter provideRatingFragmentPresenter() {
        return new RatingFragmentPresenterImpl();
    }

    @Provides
    public RatingListFragmentPresenter provideRatingListFragmentPresenter() {
        return new RatingListFragmentPresenterImpl();
    }

    @Provides
    public Room101FragmentPresenter provideRoom101FragmentPresenter() {
        return new Room101FragmentPresenterImpl();
    }

    @Provides
    public ScheduleAttestationsFragmentPresenter provideScheduleAttestationsFragmentPresenter() {
        return new ScheduleAttestationsFragmentPresenterImpl();
    }

    @Provides
    public ScheduleExamsFragmentPresenter provideScheduleExamsFragmentPresenter() {
        return new ScheduleExamsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ScheduleExamsTabHostFragmentPresenter provideScheduleExamsTabHostFragmentPresenter() {
        return new ScheduleExamsTabHostFragmentPresenterImpl();
    }

    @Provides
    public ScheduleExamsTabFragmentPresenter provideScheduleExamsTabFragmentPresenter() {
        return new ScheduleExamsTabFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ScheduleLessonsTabHostFragmentPresenter provideScheduleLessonsTabHostFragmentPresenter() {
        return new ScheduleLessonsTabHostFragmentPresenterImpl();
    }

    @Provides
    public ScheduleLessonsTabFragmentPresenter provideScheduleLessonsTabFragmentPresenter() {
        return new ScheduleLessonsTabFragmentPresenterImpl();
    }

    @Provides
    public ScheduleLessonsFragmentPresenter provideScheduleLessonsFragmentPresenter() {
        return new ScheduleLessonsFragmentPresenterImpl();
    }

    @Provides
    public ScheduleLessonsModifyFragmentPresenter provideScheduleLessonsModifyFragmentPresenter() {
        return new ScheduleLessonsModifyFragmentPresenterImpl();
    }

    @Provides
    public ScheduleLessonsShareFragmentPresenter provideScheduleLessonsShareFragmentPresenter() {
        return new ScheduleLessonsShareFragmentPresenterImpl();
    }

    @Provides
    public SubjectShowFragmentPresenter provideSubjectShowFragmentPresenter() {
        return new SubjectShowFragmentPresenterImpl();
    }

    @Provides
    public UniversityFragmentPresenter provideUniversityFragmentPresenter() {
        return new UniversityFragmentPresenterImpl();
    }

    @Provides
    public UniversityBuildingsFragmentPresenter provideUniversityBuildingsFragmentPresenter() {
        return new UniversityBuildingsFragmentPresenterImpl();
    }

    @Provides
    public UniversityEventsFragmentPresenter provideUniversityEventsFragmentPresenter() {
        return new UniversityEventsFragmentPresenterImpl();
    }

    @Provides
    public UniversityFacultiesFragmentPresenter provideUniversityFacultiesFragmentPresenter() {
        return new UniversityFacultiesFragmentPresenterImpl();
    }

    @Provides
    public UniversityNewsFragmentPresenter provideUniversityNewsFragmentPresenter() {
        return new UniversityNewsFragmentPresenterImpl();
    }

    @Provides
    public UniversityPersonsFragmentPresenter provideUniversityPersonsFragmentPresenter() {
        return new UniversityPersonsFragmentPresenterImpl();
    }

    @Provides
    public UniversityUnitsFragmentPresenter provideUniversityUnitsFragmentPresenter() {
        return new UniversityUnitsFragmentPresenterImpl();
    }
}
