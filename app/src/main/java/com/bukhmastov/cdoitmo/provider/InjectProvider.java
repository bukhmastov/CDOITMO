package com.bukhmastov.cdoitmo.provider;

import com.bukhmastov.cdoitmo.object.ProtocolTracker;
import com.bukhmastov.cdoitmo.object.SettingsScheduleAttestations;
import com.bukhmastov.cdoitmo.object.SettingsScheduleExams;
import com.bukhmastov.cdoitmo.object.SettingsScheduleLessons;
import com.bukhmastov.cdoitmo.util.Account;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.NotificationMessage;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Theme;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.Time;

public class InjectProvider {

    // future: replace with DI factory
    private static InjectProvider instance = new InjectProvider();
    public static InjectProvider instance() {
        return instance;
    }

    //@Inject
    private Log log = Log.instance();
    //@Inject
    private Thread thread = Thread.instance();
    //@Inject
    private Storage storage = Storage.instance();
    //@Inject
    private StoragePref storagePref = StoragePref.instance();
    //@Inject
    private ProtocolTracker protocolTracker = ProtocolTracker.instance();
    //@Inject
    private SettingsScheduleLessons settingsScheduleLessons = SettingsScheduleLessons.instance();
    //@Inject
    private SettingsScheduleExams settingsScheduleExams = SettingsScheduleExams.instance();
    //@Inject
    private SettingsScheduleAttestations settingsScheduleAttestations = SettingsScheduleAttestations.instance();
    //@Inject
    private Account account = Account.instance();
    //@Inject
    private NotificationMessage notificationMessage = NotificationMessage.instance();
    //@Inject
    private Static staticUtil = Static.instance();
    //@Inject
    private Theme theme = Theme.instance();
    //@Inject
    private Time time = Time.instance();

    public Log getLog() {
        return log;
    }

    public Thread getThread() {
        return thread;
    }

    public Storage getStorage() {
        return storage;
    }

    public StoragePref getStoragePref() {
        return storagePref;
    }

    public ProtocolTracker getProtocolTracker() {
        return protocolTracker;
    }

    public SettingsScheduleLessons getSettingsScheduleLessons() {
        return settingsScheduleLessons;
    }

    public SettingsScheduleExams getSettingsScheduleExams() {
        return settingsScheduleExams;
    }

    public SettingsScheduleAttestations getSettingsScheduleAttestations() {
        return settingsScheduleAttestations;
    }

    public Account getAccount() {
        return account;
    }

    public NotificationMessage getNotificationMessage() {
        return notificationMessage;
    }

    public Static getStaticUtil() {
        return staticUtil;
    }

    public Theme getTheme() {
        return theme;
    }

    public Time getTime() {
        return time;
    }
}
