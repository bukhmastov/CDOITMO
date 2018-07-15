package com.bukhmastov.cdoitmo.object.schedule;

import com.bukhmastov.cdoitmo.object.schedule.impl.ScheduleAttestationsImpl;

public interface ScheduleAttestations extends Schedule {

    // future: replace with DI factory
    ScheduleAttestations instance = new ScheduleAttestationsImpl();
    static ScheduleAttestations instance() {
        return instance;
    }

    String TYPE = "attestations";

}
