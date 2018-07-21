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
import com.bukhmastov.cdoitmo.activity.search.ScheduleAttestationsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleExamsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.ScheduleLessonsSearchActivity;
import com.bukhmastov.cdoitmo.activity.search.SearchActivity;
import com.bukhmastov.cdoitmo.adapter.PagerExamsAdapter;
import com.bukhmastov.cdoitmo.adapter.PagerLessonsAdapter;
import com.bukhmastov.cdoitmo.adapter.PagerUniversityAdapter;
import com.bukhmastov.cdoitmo.adapter.SuggestionsListView;
import com.bukhmastov.cdoitmo.adapter.TeacherPickerAdapter;
import com.bukhmastov.cdoitmo.adapter.rva.ERegisterSubjectsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.RVA;
import com.bukhmastov.cdoitmo.adapter.rva.RatingRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleAttestationsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleExamsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.ScheduleLessonsRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityFacultiesRVA;
import com.bukhmastov.cdoitmo.adapter.rva.university.UniversityRVA;
import com.bukhmastov.cdoitmo.builder.Room101ReviewBuilder;
import com.bukhmastov.cdoitmo.converter.Converter;
import com.bukhmastov.cdoitmo.converter.ProtocolConverter;
import com.bukhmastov.cdoitmo.converter.schedule.exams.ScheduleExamsConverter;
import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleConverter;
import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleLessonsAdditionalConverter;
import com.bukhmastov.cdoitmo.converter.schedule.lessons.ScheduleLessonsConverterIfmo;
import com.bukhmastov.cdoitmo.dialog.BottomSheetDialog;
import com.bukhmastov.cdoitmo.dialog.CacheClearDialog;
import com.bukhmastov.cdoitmo.dialog.ColorPickerDialog;
import com.bukhmastov.cdoitmo.dialog.Dialog;
import com.bukhmastov.cdoitmo.dialog.ThemeDialog;
import com.bukhmastov.cdoitmo.factory.module.AppModule;
import com.bukhmastov.cdoitmo.factory.module.EventBusModule;
import com.bukhmastov.cdoitmo.factory.module.FirebaseModule;
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
import com.bukhmastov.cdoitmo.fragment.SubjectShowFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityBuildingsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityEventsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFacultiesFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityNewsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityPersonsFragment;
import com.bukhmastov.cdoitmo.fragment.UniversityUnitsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsNotificationsFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsTemplateHeadersFragment;
import com.bukhmastov.cdoitmo.fragment.settings.SettingsTemplatePreferencesFragment;
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
import com.bukhmastov.cdoitmo.object.impl.SettingsSchedule;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.impl.SettingsScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.object.impl.TimeRemainingWidgetImpl;
import com.bukhmastov.cdoitmo.object.preference.Preference;
import com.bukhmastov.cdoitmo.object.preference.PreferenceSwitch;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleAttestationsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleExamsImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsHelperImpl;
import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleLessonsImpl;
import com.bukhmastov.cdoitmo.parse.Parse;
import com.bukhmastov.cdoitmo.parse.room101.Room101ViewRequestParse;
import com.bukhmastov.cdoitmo.parse.schedule.ScheduleAttestationsParse;
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
    void inject(SubjectShowFragment subjectShowFragment);
    void inject(UniversityBuildingsFragment universityBuildingsFragment);
    void inject(UniversityEventsFragment universityEventsFragment);
    void inject(UniversityFacultiesFragment universityFacultiesFragment);
    void inject(UniversityFragment universityFragment);
    void inject(UniversityNewsFragment universityNewsFragment);
    void inject(UniversityPersonsFragment universityPersonsFragment);
    void inject(UniversityUnitsFragment universityUnitsFragment);
    void inject(SettingsTemplateHeadersFragment settingsTemplateHeadersFragment);
    void inject(SettingsTemplatePreferencesFragment settingsTemplatePreferencesFragment);
    void inject(SettingsNotificationsFragment settingsNotificationsFragment);

    // Adapters
    void inject(TeacherPickerAdapter teacherPickerAdapter);
    void inject(SuggestionsListView suggestionsListView);
    void inject(PagerUniversityAdapter pagerUniversityAdapter);
    void inject(PagerLessonsAdapter pagerLessonsAdapter);
    void inject(PagerExamsAdapter pagerExamsAdapter);
    void inject(RVA rva);
    void inject(ERegisterSubjectsRVA eRegisterSubjectsRVA);
    void inject(RatingRVA ratingRVA);
    void inject(ScheduleAttestationsRVA scheduleAttestationsRVA);
    void inject(ScheduleExamsRVA scheduleExamsRVA);
    void inject(ScheduleLessonsRVA scheduleLessonsRVA);
    void inject(UniversityRVA universityRVA);
    void inject(UniversityFacultiesRVA universityFacultiesRVA);

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
    void inject(SettingsSchedule settingsSchedule);
    void inject(SettingsScheduleAttestationsImpl settingsScheduleAttestations);
    void inject(SettingsScheduleExamsImpl settingsScheduleExams);
    void inject(SettingsScheduleLessonsImpl settingsScheduleLessons);
    void inject(TimeRemainingWidgetImpl timeRemainingWidget);
    void inject(Preference preference);
    void inject(PreferenceSwitch preferenceSwitch);
    void inject(ScheduleImpl schedule);
    void inject(ScheduleLessonsImpl scheduleLessons);
    void inject(ScheduleLessonsHelperImpl scheduleLessonsHelper);
    void inject(ScheduleExamsImpl scheduleExams);
    void inject(ScheduleAttestationsImpl scheduleAttestations);

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
    void inject(TextUtilsImpl textUtils);
    void inject(ThemeImpl theme);
    void inject(ThreadImpl thread);
    void inject(TimeImpl time);

    // Providers
    void inject(InjectProvider injectProvider);
    void inject(StorageProvider storageProvider);

    // Converters
    void inject(Converter converter);
    void inject(ProtocolConverter protocolConverter);
    void inject(ScheduleConverter scheduleConverter);
    void inject(ScheduleExamsConverter scheduleExamsConverter);
    void inject(ScheduleLessonsAdditionalConverter scheduleLessonsAdditionalConverter);
    void inject(ScheduleLessonsConverterIfmo scheduleLessonsConverterIfmo);

    // Parses
    void inject(Parse parse);
    void inject(Room101ViewRequestParse room101ViewRequestParse);
    void inject(ScheduleAttestationsParse scheduleAttestationsParse);

    // Builders and more
    void inject(Room101ReviewBuilder room101ReviewBuilder);
    void inject(ScheduleLessonsWidgetStorageImpl scheduleLessonsWidgetStorage);
    void inject(OutlineTextView outlineTextView);
    void inject(OnSwipeTouchListener onSwipeTouchListener);
}
