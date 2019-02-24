package com.bukhmastov.cdoitmo.object.schedule;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.model.schedule.ScheduleJsonEntity;
import com.bukhmastov.cdoitmo.network.model.Client;

public interface Schedule<T extends ScheduleJsonEntity> {

    /**
     * Failure states of search
     * Value of each state should be more or equal to 100
     */
    int FAILED_LOAD = 100;
    int FAILED_EMPTY_QUERY = 101;
    int FAILED_INVALID_QUERY = 102;
    int FAILED_NOT_FOUND = 103;
    int FAILED_PERSONAL_NEED_ISU = 104;

    void search(@NonNull String query, @NonNull Handler<T> handler);

    void search(@NonNull String query, int refreshRate, @NonNull Handler<T> handler);

    void search(@NonNull String query, boolean forceToCache, @NonNull Handler<T> handler);

    void search(@NonNull String query, boolean forceToCache, boolean withUserChanges, @NonNull Handler<T> handler);

    void search(@NonNull String query, int refreshRate, boolean forceToCache, @NonNull Handler<T> handler);

    void search(@NonNull String query, int refreshRate, boolean forceToCache, boolean withUserChanges, @NonNull Handler<T> handler);

    String getDefaultScope();

    String getScheduleHeader(String title, String type);

    String getScheduleWeek(int week);

    String getProgressMessage(int state);

    String getFailedMessage(int code, int state);

    interface Handler<J extends ScheduleJsonEntity> {
        void onSuccess(J schedule, boolean fromCache);
        default void onFailure(int state) {
            onFailure(0, null, state);
        }
        void onFailure(int code, Client.Headers headers, int state);
        void onProgress(int state);
        void onNewRequest(Client.Request request);
        void onCancelRequest();
    }

    interface ScheduleSearchProvider<J extends ScheduleJsonEntity> {
        void onSearch(String query, Handler<J> handler);
    }
}
