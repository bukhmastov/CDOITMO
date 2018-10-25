package com.bukhmastov.cdoitmo.factory;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.activity.ConnectedActivity;
import com.bukhmastov.cdoitmo.activity.DaysRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activity.FileReceiveActivity;
import com.bukhmastov.cdoitmo.activity.FragmentActivity;
import com.bukhmastov.cdoitmo.activity.IntroducingActivity;
import com.bukhmastov.cdoitmo.activity.LoginActivity;
import com.bukhmastov.cdoitmo.activity.MainActivity;
import com.bukhmastov.cdoitmo.activity.PikaActivity;
import com.bukhmastov.cdoitmo.activity.ScheduleLessonsWidgetConfigureActivity;
import com.bukhmastov.cdoitmo.activity.ShortcutReceiverActivity;
import com.bukhmastov.cdoitmo.activity.TimeRemainingWidgetActivity;
import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.activity.WebViewActivity;
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
import com.bukhmastov.cdoitmo.activity.search.ScheduleAttestationsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.SearchActivity;
import com.bukhmastov.cdoitmo.adapter.PagerExamsAdapter;
import com.bukhmastov.cdoitmo.adapter.PagerLessonsAdapter;
import com.bukhmastov.cdoitmo.adapter.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.adapter.SuggestionsListView;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.adapter.rva.RVABase;
import com.bukhmastov.cdoitmo.adapter.rva.RatingRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleAttestationsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleExamsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleLessonsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.builder.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.dialog.BottomSheetDialog;
import com.bukhmastov.cdoitmo.dialog.CacheClearDialog;
import com.bukhmastov.cdoitmo.dialog.ColorPickerDialog;
import com.bukhmastov.cdoitmo.dialog.Dialog;
import com.bukhmastov.cdoitmo.dialog.ThemeDialog;
import com.bukhmastov.cdoitmo.event.bus.impl.EventBusImpl;
import com.bukhmastov.cdoitmo.factory.module.ActivityPresenterModule;
import com.bukhmastov.cdoitmo.factory.module.AppModule;
import com.bukhmastov.cdoitmo.factory.module.EventBusModule;
import com.bukhmastov.cdoitmo.factory.module.FirebaseModule;
import com.bukhmastov.cdoitmo.factory.module.FragmentPresenterModule;
import com.bukhmastov.cdoitmo.factory.module.NetworkModule;
import com.bukhmastov.cdoitmo.factory.module.ObjectModule;
import com.bukhmastov.cdoitmo.factory.module.ProviderModule;
import com.bukhmastov.cdoitmo.factory.module.UtilsModule;
import com.bukhmastov.cdoitmo.factory.module.WidgetModule;
import com.bukhmastov.cdoitmo.firebase.FirebaseMessagingServiceProvider;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseAnalyticsProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseConfigProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebaseCrashlyticsProviderImpl;
import com.bukhmastov.cdoitmo.firebase.impl.FirebasePerformanceProviderImpl;
import com.bukhmastov.cdoitmo.fragment.AboutFragment;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.fragment.ERegisterFragment;
import com.bukhmastov.cdoitmo.fragment.HomeScreenInteractionFragment;
import com.bukhmastov.cdoitmo.fragment.LinkedAccountsFragment;
import com.bukhmastov.cdoitmo.fragment.LogFragment;
import com.bukhmastov.cdoitmo.fragment.ProtocolFragment;
import com.bukhmastov.cdoitmo.fragment.RatingFragment;
import com.bukhmastov.cdoitmo.fragment.RatingListFragment;
import com.bukhmastov.cdoitmo.fragment.Room101Fragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleAttestationsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleExamsTabHostFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsModifyFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsShareFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsTabFragment;
import com.bukhmastov.cdoitmo.fragment.ScheduleLessonsTabHostFragment;
import com.bukhmastov.cdoitmo.fragment.ERegisterSubjectFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityBuildingsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityEventsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFacultiesFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityNewsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityUnitsFragment;
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
import com.bukhmastov.cdoitmo.fragment.presenter.impl.ERegisterSubjectFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityBuildingsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityEventsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityFacultiesFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityNewsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityPersonsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.presenter.impl.UniversityUnitsFragmentPresenterImpl;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsNotificationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsTemplateHeadersFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsTemplatePreferencesFragment;
import com.bukhmastov.cdoitmo.model.converter.ConverterBase;
import com.bukhmastov.cdoitmo.model.parser.ParserBase;
import com.bukhmastov.cdoitmo.network.impl.DeIfmoClientImpl;
import com.bukhmastov.cdoitmo.network.impl.DeIfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.impl.IfmoClientImpl;
import com.bukhmastov.cdoitmo.network.impl.IfmoRestClientImpl;
import com.bukhmastov.cdoitmo.network.impl.Room101ClientImpl;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.network.model.DeIfmo;
import com.bukhmastov.cdoitmo.network.model.Ifmo;
import com.bukhmastov.cdoitmo.network.provider.impl.NetworkClientProviderImpl;
import com.bukhmastov.cdoitmo.network.provider.impl.NetworkUserAgentProviderImpl;
import com.bukhmastov.cdoitmo.object.ProtocolTrackerJobService;
import com.bukhmastov.cdoitmo.object.impl.DaysRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerImpl;
import com.bukhmastov.cdoitmo.object.impl.ProtocolTrackerServiceImpl;
import com.bukhmastov.cdoitmo.object.impl.Room101AddRequestImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleBase;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.object.impl.TeacherSearchImpl;
import com.bukhmastov.cdoitmo.object.impl.TimeRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleBase;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsHelperImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.provider.InjectProvider;
import com.bukhmastov.cdoitmo.provider.StorageProvider;
import com.bukhmastov.cdoitmo.receiver.ShortcutReceiver;
import com.bukhmastov.cdoitmo.util.impl.AccountImpl;
import com.bukhmastov.cdoitmo.util.impl.AccountsImpl;
import com.bukhmastov.cdoitmo.util.impl.LogImpl;
import com.bukhmastov.cdoitmo.util.impl.NavigationMenuImpl;
import com.bukhmastov.cdoitmo.util.impl.NotificationMessageImpl;
import com.bukhmastov.cdoitmo.util.impl.NotificationsImpl;
import com.bukhmastov.cdoitmo.util.impl.StaticImpl;
import com.bukhmastov.cdoitmo.util.impl.StorageImpl;
import com.bukhmastov.cdoitmo.util.impl.StorageLocalCacheImpl;
import com.bukhmastov.cdoitmo.util.impl.StoragePrefImpl;
import com.bukhmastov.cdoitmo.util.impl.TextUtilsImpl;
import com.bukhmastov.cdoitmo.util.impl.ThemeImpl;
import com.bukhmastov.cdoitmo.util.impl.ThreadImpl;
import com.bukhmastov.cdoitmo.util.impl.TimeImpl;
import com.bukhmastov.cdoitmo.view.OnSwipeTouchListener;
import com.bukhmastov.cdoitmo.view.OutlineTextView;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidget;
import com.bukhmastov.cdoitmo.widget.ScheduleLessonsWidgetFactory;
import com.bukhmastov.cdoitmo.widget.impl.ScheduleLessonsWidgetStorageImpl;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppModule.class, EventBusModule.class,
        ActivityPresenterModule.class, FragmentPresenterModule.class,
        UtilsModule.class, ObjectModule.class,
        NetworkModule.class, WidgetModule.class,
        FirebaseModule.class, ProviderModule.class
})
public interface AppComponent {

    // Application
    void inject(App app);

    // Activities
    void inject(MainActivity mainActivity);
    void inject(ConnectedActivity connectedActivity);
    void inject(LoginActivity loginActivity);
    void inject(FragmentActivity fragmentActivity);
    void inject(IntroducingActivity introducingActivity);
    void inject(FileReceiveActivity fileReceiveActivity);
    void inject(PikaActivity pikaActivity);
    void inject(DaysRemainingWidgetActivity daysRemainingWidgetActivity);
    void inject(TimeRemainingWidgetActivity timeRemainingWidgetActivity);
    void inject(UniversityPersonCardActivity universityPersonCardActivity);
    void inject(WebViewActivity webViewActivity);
    void inject(ScheduleLessonsWidgetConfigureActivity scheduleLessonsWidgetConfigureActivity);
    void inject(ShortcutReceiverActivity shortcutReceiverActivity);
    void inject(SearchActivity searchActivity);
    void inject(ScheduleLessonsSearchActivity scheduleLessonsSearchActivity);
    void inject(ScheduleExamsSearchActivity scheduleExamsSearchActivity);
    void inject(ScheduleAttestationsSearchActivity scheduleAttestationsSearchActivity);
    // Activity presenters
    void inject(MainActivityPresenterImpl mainActivityPresenter);
    void inject(LoginActivityPresenterImpl loginActivityPresenter);
    void inject(FragmentActivityPresenterImpl fragmentActivityPresenter);
    void inject(IntroducingActivityPresenterImpl introducingActivityModel);
    void inject(FileReceiveActivityPresenterImpl fileReceiveActivityPresenter);
    void inject(PikaActivityPresenterImpl pikaActivityPresenter);
    void inject(WebViewActivityPresenterImpl webViewActivityPresenter);
    void inject(UniversityPersonCardActivityPresenterImpl universityPersonCardActivityPresenter);
    void inject(ShortcutReceiverActivityPresenterImpl shortcutReceiverActivityPresenter);
    void inject(ScheduleLessonsWidgetConfigureActivityPresenterImpl scheduleLessonsWidgetConfigureActivityPresenter);
    void inject(DaysRemainingWidgetActivityPresenterImpl daysRemainingWidgetActivityPresenter);
    void inject(TimeRemainingWidgetActivityPresenterImpl timeRemainingWidgetActivityPresenter);

    // Fragments
    void inject(ConnectedFragment connectedFragment);
    void inject(ERegisterFragment eRegisterFragment);
    void inject(AboutFragment aboutFragment);
    void inject(HomeScreenInteractionFragment homeScreenInteractionFragment);
    void inject(LinkedAccountsFragment linkedAccountsFragment);
    void inject(LogFragment logFragment);
    void inject(ProtocolFragment protocolFragment);
    void inject(RatingFragment ratingFragment);
    void inject(RatingListFragment ratingListFragment);
    void inject(Room101Fragment room101Fragment);
    void inject(ScheduleAttestationsFragment scheduleAttestationsFragment);
    void inject(ScheduleExamsFragment scheduleExamsFragment);
    void inject(ScheduleExamsTabFragment scheduleExamsTabFragment);
    void inject(ScheduleExamsTabHostFragment scheduleExamsTabHostFragment);
    void inject(ScheduleLessonsFragment scheduleLessonsFragment);
    void inject(ScheduleLessonsModifyFragment scheduleLessonsModifyFragment);
    void inject(ScheduleLessonsShareFragment scheduleLessonsShareFragment);
    void inject(ScheduleLessonsTabFragment scheduleLessonsTabFragment);
    void inject(ScheduleLessonsTabHostFragment scheduleLessonsTabHostFragment);
    void inject(ERegisterSubjectFragment subjectShowFragment);
    void inject(UniversityBuildingsFragment universityBuildingsFragment);
    void inject(UniversityEventsFragment universityEventsFragment);
    void inject(UniversityFacultiesFragment universityFacultiesFragment);
    void inject(UniversityFragment universityFragment);
    void inject(UniversityNewsFragment universityNewsFragment);
    void inject(UniversityPersonsFragment universityPersonsFragment);
    void inject(UniversityUnitsFragment universityUnitsFragment);
    void inject(SettingsTemplateHeadersFragment settingsTemplateHeadersFragment);
    void inject(SettingsTemplatePreferencesFragment settingsTemplatePreferencesFragment);
    void inject(SettingsFragment settingsFragment);
    void inject(SettingsNotificationsFragment settingsNotificationsFragment);
    // Fragment presenters
    void inject(AboutFragmentPresenterImpl aboutFragmentPresenter);
    void inject(ERegisterFragmentPresenterImpl eRegisterFragmentPresenter);
    void inject(HomeScreenInteractionFragmentPresenterImpl homeScreenInteractionFragmentPresenter);
    void inject(LinkedAccountsFragmentPresenterImpl linkedAccountsFragmentPresenter);
    void inject(LogFragmentPresenterImpl logFragmentPresenter);
    void inject(ProtocolFragmentPresenterImpl protocolFragmentPresenter);
    void inject(RatingFragmentPresenterImpl ratingFragmentPresenter);
    void inject(RatingListFragmentPresenterImpl ratingListFragmentPresenter);
    void inject(Room101FragmentPresenterImpl room101FragmentPresenter);
    void inject(ScheduleAttestationsFragmentPresenterImpl scheduleAttestationsFragmentPresenter);
    void inject(ScheduleExamsFragmentPresenterImpl scheduleExamsFragmentPresenter);
    void inject(ScheduleExamsTabHostFragmentPresenterImpl scheduleExamsTabHostFragmentPresenter);
    void inject(ScheduleExamsTabFragmentPresenterImpl scheduleExamsTabFragmentPresenter);
    void inject(ScheduleLessonsTabHostFragmentPresenterImpl scheduleLessonsTabHostFragmentPresenter);
    void inject(ScheduleLessonsTabFragmentPresenterImpl scheduleLessonsTabFragmentPresenter);
    void inject(ScheduleLessonsFragmentPresenterImpl scheduleLessonsFragmentPresenter);
    void inject(ScheduleLessonsModifyFragmentPresenterImpl scheduleLessonsModifyFragmentPresenter);
    void inject(ScheduleLessonsShareFragmentPresenterImpl scheduleLessonsShareFragmentPresenter);
    void inject(ERegisterSubjectFragmentPresenterImpl subjectShowFragmentPresenter);
    void inject(UniversityFragmentPresenterImpl universityFragmentPresenter);
    void inject(UniversityBuildingsFragmentPresenterImpl universityBuildingsFragmentPresenter);
    void inject(UniversityEventsFragmentPresenterImpl universityEventsFragmentPresenter);
    void inject(UniversityFacultiesFragmentPresenterImpl universityFacultiesFragmentPresenter);
    void inject(UniversityNewsFragmentPresenterImpl universityNewsFragmentPresenter);
    void inject(UniversityPersonsFragmentPresenterImpl universityPersonsFragmentPresenter);
    void inject(UniversityUnitsFragmentPresenterImpl universityUnitsFragmentPresenter);

    // Adapters
    void inject(TeacherPickerAdapter teacherPickerAdapter);
    void inject(SuggestionsListView suggestionsListView);
    void inject(PagerUniversityAdapter pagerUniversityAdapter);
    void inject(PagerLessonsAdapter pagerLessonsAdapter);
    void inject(PagerExamsAdapter pagerExamsAdapter);
    void inject(RVABase rva);
    void inject(RatingRVA ratingRVA);
    void inject(ScheduleAttestationsRVA scheduleAttestationsRVA);
    void inject(ScheduleExamsRVA scheduleExamsRVA);
    void inject(ScheduleLessonsRVA scheduleLessonsRVA);
    void inject(UniversityRVA universityRVA);

    // Dialogs
    void inject(Dialog dialog);
    void inject(ThemeDialog themeDialog);
    void inject(CacheClearDialog cacheClearDialog);
    void inject(ColorPickerDialog colorPickerDialog);
    void inject(BottomSheetDialog bottomSheetDialog);

    // Receivers
    void inject(ShortcutReceiver shortcutReceiver);
    void inject(ScheduleLessonsWidget scheduleLessonsWidget);
    void inject(ScheduleLessonsWidgetFactory scheduleLessonsWidgetFactory);

    // Network
    void inject(Client client);
    void inject(DeIfmo deIfmo);
    void inject(Ifmo ifmo);
    void inject(DeIfmoClientImpl deIfmoClient);
    void inject(DeIfmoRestClientImpl deIfmoRestClient);
    void inject(IfmoClientImpl ifmoClient);
    void inject(IfmoRestClientImpl ifmoRestClient);
    void inject(Room101ClientImpl room101Client);
    void inject(NetworkClientProviderImpl networkClientProvider);
    void inject(NetworkUserAgentProviderImpl networkUserAgentProvider);

    // Firebase
    void inject(FirebaseAnalyticsProviderImpl firebaseAnalyticsProvider);
    void inject(FirebaseConfigProviderImpl firebaseConfigProvider);
    void inject(FirebaseCrashlyticsProviderImpl firebaseCrashlyticsProvider);
    void inject(FirebasePerformanceProviderImpl firebasePerformanceProvider);
    void inject(FirebaseMessagingServiceProvider firebaseMessagingServiceProvider);

    // Objects
    void inject(DaysRemainingWidgetImpl daysRemainingWidget);
    void inject(ProtocolTrackerImpl protocolTracker);
    void inject(ProtocolTrackerServiceImpl protocolTrackerService);
    void inject(ProtocolTrackerJobService protocolTrackerJobService);
    void inject(Room101AddRequestImpl room101AddRequest);
    void inject(SettingsScheduleBase settingsSchedule);
    void inject(SettingsScheduleAttestationsImpl settingsScheduleAttestations);
    void inject(SettingsScheduleExamsImpl settingsScheduleExams);
    void inject(SettingsScheduleLessonsImpl settingsScheduleLessons);
    void inject(TimeRemainingWidgetImpl timeRemainingWidget);
    void inject(Preference preference);
    void inject(PreferenceSwitch preferenceSwitch);
    void inject(ScheduleBase schedule);
    void inject(ScheduleLessonsImpl scheduleLessons);
    void inject(ScheduleLessonsHelperImpl scheduleLessonsHelper);
    void inject(ScheduleExamsImpl scheduleExams);
    void inject(ScheduleAttestationsImpl scheduleAttestations);
    void inject(TeacherSearchImpl teacherSearch);

    // Utils
    void inject(AccountImpl account);
    void inject(AccountsImpl accounts);
    void inject(LogImpl log);
    void inject(NavigationMenuImpl navigationMenu);
    void inject(NotificationMessageImpl notificationMessage);
    void inject(NotificationsImpl notifications);
    void inject(StaticImpl staticUtil);
    void inject(StorageImpl storage);
    void inject(StoragePrefImpl storagePref);
    void inject(StorageLocalCacheImpl storageLocalCache);
    void inject(TextUtilsImpl textUtils);
    void inject(ThemeImpl theme);
    void inject(ThreadImpl thread);
    void inject(TimeImpl time);

    // Providers
    void inject(InjectProvider injectProvider);
    void inject(StorageProvider storageProvider);

    // Converters and Parsers
    void inject(ConverterBase converter);
    void inject(ParserBase parse);

    // Builders and more
    void inject(Room101ReviewBuilder room101ReviewBuilder);
    void inject(ScheduleLessonsWidgetStorageImpl scheduleLessonsWidgetStorage);
    void inject(OutlineTextView outlineTextView);
    void inject(OnSwipeTouchListener onSwipeTouchListener);
    void inject(EventBusImpl eventBus);
}
