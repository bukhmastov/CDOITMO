package com.bukhmastov.cdoitmo.fragment.presenter;

public interface ScheduleAttestationsFragmentPresenter extends ConnectedFragmentPresenter {

    void invalidate();

    void invalidate(boolean refresh);

    void setQuery(String query);
}
