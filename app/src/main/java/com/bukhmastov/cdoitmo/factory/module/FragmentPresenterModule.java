package com.bukhmastov.cdoitmo.factory.module;

import com.bukhmastov.cdoitmo.fragment.presenter.*;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.*;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentPresenterModule {

    @Provides
    @Singleton
    public AboutFragmentPresenter provideAboutFragmentPresenter() {
        return new AboutFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ERegisterFragmentPresenter provideERegisterFragmentPresenter() {
        return new ERegisterFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public HomeScreenInteractionFragmentPresenter provideHomeScreenInteractionFragmentPresenter() {
        return new HomeScreenInteractionFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public LinkedAccountsFragmentPresenter provideLinkedAccountsFragmentPresenter() {
        return new LinkedAccountsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public LinkAccountFragmentPresenter provideLinkAccountFragmentPresenter() {
        return new LinkAccountFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public LogFragmentPresenter provideLogFragmentPresenter() {
        return new LogFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ProtocolFragmentPresenter provideProtocolFragmentPresenter() {
        return new ProtocolFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public RatingFragmentPresenter provideRatingFragmentPresenter() {
        return new RatingFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public RatingListFragmentPresenter provideRatingListFragmentPresenter() {
        return new RatingListFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public Room101FragmentPresenter provideRoom101FragmentPresenter() {
        return new Room101FragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ScheduleAttestationsFragmentPresenter provideScheduleAttestationsFragmentPresenter() {
        return new ScheduleAttestationsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
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
    @Singleton
    public ScheduleLessonsFragmentPresenter provideScheduleLessonsFragmentPresenter() {
        return new ScheduleLessonsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ScheduleLessonsModifyFragmentPresenter provideScheduleLessonsModifyFragmentPresenter() {
        return new ScheduleLessonsModifyFragmentPresenterImpl();
    }

    @Provides
    public ScheduleLessonsShareFragmentPresenter provideScheduleLessonsShareFragmentPresenter() {
        return new ScheduleLessonsShareFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public ERegisterSubjectFragmentPresenter provideERegisterSubjectFragmentPresenter() {
        return new ERegisterSubjectFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityFragmentPresenter provideUniversityFragmentPresenter() {
        return new UniversityFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityBuildingsFragmentPresenter provideUniversityBuildingsFragmentPresenter() {
        return new UniversityBuildingsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityEventsFragmentPresenter provideUniversityEventsFragmentPresenter() {
        return new UniversityEventsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityFacultiesFragmentPresenter provideUniversityFacultiesFragmentPresenter() {
        return new UniversityFacultiesFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityNewsFragmentPresenter provideUniversityNewsFragmentPresenter() {
        return new UniversityNewsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityPersonsFragmentPresenter provideUniversityPersonsFragmentPresenter() {
        return new UniversityPersonsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public UniversityUnitsFragmentPresenter provideUniversityUnitsFragmentPresenter() {
        return new UniversityUnitsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public IsuGroupInfoFragmentPresenter provideIsuGroupInfoFragmentPresenter() {
        return new IsuGroupInfoFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public IsuScholarshipPaidFragmentPresenter provideIsuScholarshipPaidFragmentPresenter() {
        return new IsuScholarshipPaidFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public IsuScholarshipAssignedFragmentPresenter provideIsuScholarshipAssignedFragmentPresenter() {
        return new IsuScholarshipAssignedFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public IsuScholarshipPaidDetailsFragmentPresenter provideIsuScholarshipPaidDetailsFragmentPresenter() {
        return new IsuScholarshipPaidDetailsFragmentPresenterImpl();
    }

    @Provides
    @Singleton
    public HelpFragmentPresenter provideHelpFragmentPresenter() {
        return new HelpFragmentPresenterImpl();
    }
}
