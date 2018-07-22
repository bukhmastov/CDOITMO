package com.bukhmastov.cdoitmo.event.events;

import android.view.MenuItem;

public class MainActivityEvent {

    public static class AutoLogoutChangedEvent {

        private final boolean autoLogout;

        public AutoLogoutChangedEvent(boolean autoLogout) {
            this.autoLogout = autoLogout;
        }

        public boolean isAutoLogout() {
            return autoLogout;
        }
    }

    public static class MenuSelectedItemChangedEvent {

        private final MenuItem selectedMenuItem;

        public MenuSelectedItemChangedEvent(MenuItem selectedMenuItem) {
            this.selectedMenuItem = selectedMenuItem;
        }

        public MenuItem getSelectedMenuItem() {
            return selectedMenuItem;
        }
    }

    public static class SwitchToOfflineModeEvent {
        public SwitchToOfflineModeEvent() {}
    }

    public static class UnloadEvent {
        public UnloadEvent() {}
    }
}
