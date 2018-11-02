package com.bukhmastov.cdoitmo.activity;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringDef;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.util.TextUtils;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import javax.inject.Inject;

public abstract class ConnectedActivity extends androidx.fragment.app.FragmentActivity {

    private static final String TAG = "ConnectedActivity";
    private final ArrayList<StackElement> stack = new ArrayList<>();
    private static final String STATE_STORED_FRAGMENT_NAME = "storedFragmentName";
    private static final String STATE_STORED_FRAGMENT_DATA = "storedFragmentData";
    private static final String STATE_STORED_FRAGMENT_EXTRA = "storedFragmentExtra";
    public final static String ACTIVITY_WITH_MENU = "connected_activity_with_align";
    public boolean layoutWithMenu = true;
    public Menu toolbar = null;
    public static String storedFragmentName = null;
    public static String storedFragmentData = null;
    public static String storedFragmentExtra = null;
    protected final ConnectedActivity activity = this;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;
    @Inject
    TextUtils textUtils;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE.ROOT, TYPE.STACKABLE})
    public @interface Type {}
    public static class TYPE {
        public static final String ROOT = "root";
        public static final String STACKABLE = "stackable";
    }

    public static class StackElement {
        public final Class connectedFragmentClass;
        public final Bundle extras;
        public final @Type String type;
        public StackElement(@Type String type, Class connectedFragmentClass, Bundle extras) {
            this.connectedFragmentClass = connectedFragmentClass;
            this.extras = extras;
            this.type = type;
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putString(STATE_STORED_FRAGMENT_NAME, storedFragmentName);
        outState.putString(STATE_STORED_FRAGMENT_DATA, storedFragmentData);
        outState.putString(STATE_STORED_FRAGMENT_EXTRA, storedFragmentExtra);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        storedFragmentName = savedInstanceState.getString(STATE_STORED_FRAGMENT_NAME);
        storedFragmentData = savedInstanceState.getString(STATE_STORED_FRAGMENT_DATA);
        storedFragmentExtra = savedInstanceState.getString(STATE_STORED_FRAGMENT_EXTRA);
    }

    public abstract @IdRes int getRootViewId();

    public boolean openActivityOrFragment(Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(TYPE.STACKABLE, connectedFragmentClass, extras);
    }

    public boolean openActivityOrFragment(@Type String type, Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(new StackElement(type, connectedFragmentClass, extras));
    }

    public boolean openActivityOrFragment(StackElement stackElement) {
        log.v(TAG, "openActivityOrFragment | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass.toString());
        if (App.tablet) {
            return openFragment(stackElement);
        } else {
            return openActivity(stackElement);
        }
    }

    public boolean openFragment(Class connectedFragmentClass, Bundle extras) {
        return openFragment(TYPE.STACKABLE, connectedFragmentClass, extras);
    }

    public boolean openFragment(@Type String type, Class connectedFragmentClass, Bundle extras) {
        return openFragment(new StackElement(type, connectedFragmentClass, extras));
    }

    public boolean openFragment(StackElement stackElement) {
        log.v(TAG, "openFragment | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass.toString());
        try {
            ConnectedFragment.Data data = ConnectedFragment.getData(this, stackElement.connectedFragmentClass);
            ViewGroup rootLayout = findViewById(getRootViewId());
            if (rootLayout != null) {
                rootLayout.removeAllViews();
            }
            ConnectedFragment connectedFragment = (ConnectedFragment) data.connectedFragmentClass.newInstance();
            if (stackElement.extras != null) {
                connectedFragment.setArguments(stackElement.extras);
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager == null) {
                return false;
            }
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(getRootViewId(), connectedFragment);
            fragmentTransaction.commitAllowingStateLoss();
            pushFragment(stackElement);
            updateToolbar(this, data.title, layoutWithMenu ? data.image : null);
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    public boolean openActivity(Class connectedFragmentClass, Bundle extras) {
        return openActivity(TYPE.STACKABLE, connectedFragmentClass, extras);
    }

    public boolean openActivity(@Type String type, Class connectedFragmentClass, Bundle extras) {
        return openActivity(new StackElement(type, connectedFragmentClass, extras));
    }

    public boolean openActivity(StackElement stackElement) {
        log.v(TAG, "openActivity | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass.toString());
        try {
            Bundle bundle = new Bundle();
            bundle.putSerializable("class", stackElement.connectedFragmentClass);
            bundle.putBundle("extras", stackElement.extras);
            eventBus.fire(new OpenActivityEvent(FragmentActivity.class, bundle));
            return true;
        } catch (Exception e) {
            log.exception(e);
            return false;
        }
    }

    public boolean back() {
        log.v(TAG, "back | stack.size=", stack.size());
        if (stack.size() > 0) {
            int index = stack.size() - 1;
            if (stack.get(index).type.equals(TYPE.ROOT)) {
                return true;
            } else {
                stack.remove(index);
            }
        }
        if (stack.size() > 0) {
            final int index = stack.size() - 1;
            final StackElement stackElement = stack.get(index);
            stack.remove(index);
            openFragment(stackElement);
            return false;
        } else {
            return true;
        }
    }

    public void pushFragment(StackElement stackElement) {
        log.v(TAG, "pushFragment | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass.toString());
        if (stackElement.type.equals(TYPE.ROOT)) {
            stack.clear();
        }
        stack.add(stackElement);
        log.v(TAG, "stack.size() = ", stack.size());
    }

    public void removeFragment(Class connectedFragmentClass) {
        log.v(TAG, "removeFragment | class=", connectedFragmentClass.toString());
        for (int i = stack.size() - 1; i >= 0; i--) {
            StackElement stackElement = stack.get(i);
            if (stackElement.connectedFragmentClass == connectedFragmentClass) {
                if (!stackElement.type.equals(TYPE.ROOT)) {
                    stack.remove(stackElement);
                } else {
                    log.e(TAG, "removeFragment | prevented root fragment removal from the stack");
                }
                break;
            }
        }
        log.v(TAG, "stack.size() = ", stack.size());
    }

    public void updateToolbar(Context context, String title, Integer image) {
        thread.runOnUI(() -> {
            ActionBar actionBar = getActionBar();
            if (actionBar == null) {
                return;
            }
            actionBar.setTitle(title);
            if (image == null || !App.tablet) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setLogo(null);
                return;
            }
            actionBar.setHomeButtonEnabled(false);
            Drawable drawable = getDrawable(image);
            if (drawable != null) {
                try {
                    drawable.setTint(Color.resolve(context, R.attr.colorToolbarContent));
                } catch (Exception ignore) {
                    // ignore
                }
                actionBar.setLogo(drawable);
            }
        });
    }

    public View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }

    public void draw(int layoutId) {
        try {
            draw(inflate(layoutId));
        } catch (Exception e){
            log.exception(e);
        }
    }

    public void draw(View view) {
        try {
            ViewGroup vg = findViewById(getRootViewId());
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        } catch (Exception e){
            log.exception(e);
        }
    }

    public static void clearStore() {
        storedFragmentName = null;
        storedFragmentData = null;
        storedFragmentExtra = null;
    }

    @Override
    protected void attachBaseContext(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref, log, textUtils));
    }
}
