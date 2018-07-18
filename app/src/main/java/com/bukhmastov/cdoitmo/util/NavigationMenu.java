package com.bukhmastov.cdoitmo.util;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.firebase.FirebaseConfigProvider;
import com.bukhmastov.cdoitmo.util.impl.NavigationMenuImpl;
import com.bukhmastov.cdoitmo.view.Message;

import java.util.ArrayList;
import java.util.List;

public interface NavigationMenu {

    // future: replace with DI factory
    NavigationMenu instance = new NavigationMenuImpl();
    static NavigationMenu instance() {
        return instance;
    }

    void displayEnableDisableOfflineButton(final NavigationView navigationView);

    void displayUserData(final Context context, final Storage storage, final NavigationView navigationView);

    void toggleOfflineIcon(final Menu menu);

    void displayRemoteMessage(final Activity activity, final FirebaseConfigProvider firebaseConfigProvider, final Storage storage);

    void hideIfUnauthorizedMode(final NavigationView navigationView);
}
