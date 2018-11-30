package com.bukhmastov.cdoitmo.event.events;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

public class ClearCacheEvent {

    private final @NonNull @Identity String identity;

    public ClearCacheEvent() {
        this(ALL);
    }

    public ClearCacheEvent(@NonNull @Identity String identity) {
        this.identity = identity;
    }

    public @NonNull @Identity String getIdentity() {
        return identity;
    }

    public boolean is(@NonNull @Identity String identity) {
        if (ALL.equals(getIdentity())) {
            return true;
        }
        if (NOT_VALUABLE.equals(getIdentity()) && !VALUABLE_IDENTITIES.contains(getIdentity())) {
            return true;
        }
        return getIdentity().equals(identity);
    }

    public boolean isNot(@NonNull @Identity String identity) {
        return !is(identity);
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ALL, NOT_VALUABLE,
            EREGISTER, PROTOCOL, RATING, ROOM101,
            SCHEDULE_LESSONS, SCHEDULE_EXAMS, SCHEDULE_ATTESTATIONS,
            UNIVERSITY, GROUPS, SCHOLARSHIP
    })
    public @interface Identity {}
    public static final String ALL = "_all_";
    public static final String NOT_VALUABLE = "_not_valuable_";
    public static final String EREGISTER = "eregister";
    public static final String PROTOCOL = "protocol";
    public static final String RATING = "rating";
    public static final String ROOM101 = "room101";
    public static final String SCHEDULE_LESSONS = "schedule_lessons";
    public static final String SCHEDULE_EXAMS = "schedule_exams";
    public static final String SCHEDULE_ATTESTATIONS = "schedule_attestations";
    public static final String UNIVERSITY = "university";
    public static final String GROUPS = "groups";
    public static final String SCHOLARSHIP = "scholarship";

    private static final Collection<String> VALUABLE_IDENTITIES = Arrays.asList(EREGISTER, SCHEDULE_LESSONS, SCHEDULE_EXAMS);
}
