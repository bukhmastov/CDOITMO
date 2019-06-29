package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringDef;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bukhmastov.cdoitmo.App;
import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenActivityEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.StoragePref;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.CtxWrapper;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import javax.inject.Inject;

public abstract class ConnectedActivity extends AppCompatActivity {

    public static final String STATE_CONNECTED_ACTIVITY_STACK = "connectedActivityStack";
    public static final String STATE_LAYOUT_WITH_MENU = "isLayoutWithMenu";
    public static final String STATE_STORED_FRAGMENT_NAME = "storedFragmentName";
    public static final String STATE_STORED_FRAGMENT_DATA = "storedFragmentData";
    public static final String STATE_STORED_FRAGMENT_EXTRA = "storedFragmentExtra";
    public static final String ACTIVITY_WITH_MENU = "connected_activity_with_align";

    private final ArrayList<StackElement> stack = new ArrayList<>();
    public static String storedFragmentName = null;
    public static String storedFragmentData = null;
    public static String storedFragmentExtra = null;
    public boolean layoutWithMenu = true;
    public Menu toolbar = null;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    StoragePref storagePref;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE.ROOT, TYPE.STACKABLE})
    public @interface Type {}
    public static class TYPE {
        public static final String ROOT = "root";
        public static final String STACKABLE = "stackable";
    }

    public abstract @IdRes int getRootViewId();
    protected abstract String getLogTag();

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        AppComponentProvider.getComponent().inject(this);
        log.v(getLogTag(), getMethodSignature("onCreate"));
        if (savedInstanceState != null) {
            ArrayList<StackElement> s = savedInstanceState.getParcelableArrayList(STATE_CONNECTED_ACTIVITY_STACK);
            if (s != null) {
                stack.addAll(s);
            }
            layoutWithMenu = savedInstanceState.getBoolean(STATE_LAYOUT_WITH_MENU, layoutWithMenu);
            storedFragmentName = savedInstanceState.getString(STATE_STORED_FRAGMENT_NAME);
            storedFragmentData = savedInstanceState.getString(STATE_STORED_FRAGMENT_DATA);
            storedFragmentExtra = savedInstanceState.getString(STATE_STORED_FRAGMENT_EXTRA);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(STATE_CONNECTED_ACTIVITY_STACK, stack);
        outState.putBoolean(STATE_LAYOUT_WITH_MENU, layoutWithMenu);
        outState.putString(STATE_STORED_FRAGMENT_NAME, storedFragmentName);
        outState.putString(STATE_STORED_FRAGMENT_DATA, storedFragmentData);
        outState.putString(STATE_STORED_FRAGMENT_EXTRA, storedFragmentExtra);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        log.v(getLogTag(), getMethodSignature("onDestroy"));
        super.onDestroy();
    }

    public boolean openActivityOrFragment(Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(TYPE.STACKABLE, connectedFragmentClass, extras);
    }

    public boolean openActivityOrFragment(@Type String type, Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(new StackElement(type, connectedFragmentClass, extras));
    }

    public boolean openActivityOrFragment(StackElement stackElement) {
        log.v(getLogTag(), getMethodSignature("openActivityOrFragment"), " | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass);
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
        log.v(getLogTag(), getMethodSignature("openFragment"), " | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass);
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
            if (fragmentManager.isStateSaved() || fragmentManager.isDestroyed() || isDestroyed()) {
                log.w(getLogTag(), getMethodSignature("openFragment"), " | Fragment not opened | ",
                        "isStateSaved=", fragmentManager.isStateSaved(), " | isDestroyed=", fragmentManager.isDestroyed(), "/", isDestroyed());
                return false;
            }
            log.v(getLogTag(), getMethodSignature("openFragment"), " | containerId=", getRootViewId(), " | fragment=", connectedFragment.toString());
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(getRootViewId(), connectedFragment);
            fragmentTransaction.commit();
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
        log.v(getLogTag(), getMethodSignature("openActivity"), " | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass);
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
        log.v(getLogTag(), getMethodSignature("back"), " | stack.size()=", stack.size());
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

    public int getStackSize() {
        return stack.size();
    }

    public StackElement popFragment() {
        log.v(getLogTag(), getMethodSignature("popFragment"));
        if (stack.isEmpty()) {
            log.v(getLogTag(), getMethodSignature("popFragment"), " | stack is empty");
            return null;
        }
        StackElement stackElement = stack.remove(stack.size() - 1);
        log.v(getLogTag(), getMethodSignature("popFragment"), " | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass, " | stack.size() = ", stack.size());
        return stackElement;
    }

    public void pushFragment(StackElement stackElement) {
        log.v(getLogTag(), getMethodSignature("pushFragment"), " | type=", stackElement.type, " | class=", stackElement.connectedFragmentClass);
        if (stackElement.type.equals(TYPE.ROOT)) {
            stack.clear();
        }
        stack.add(stackElement);
        log.v(getLogTag(), getMethodSignature("pushFragment"), " | stack.size()=", stack.size());
    }

    public void updateToolbar(Context context, String title, Integer image) {
        thread.runOnUI(() -> {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar == null) {
                return;
            }
            actionBar.setTitle(title);
            if (image == null || !App.tablet) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
                actionBar.setLogo(null);
                return;
            }
            actionBar.setDisplayHomeAsUpEnabled(false);
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
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            log.e(getLogTag(), getMethodSignature("inflate"), " | Failed to inflate layout, inflater is null");
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

    private String getMethodSignature(String methodName) {
        return "ConnectedActivity[" + hashCode() + "]#" + methodName;
    }

    @Override
    protected void attachBaseContext(Context context) {
        AppComponentProvider.getComponent().inject(this);
        super.attachBaseContext(CtxWrapper.wrap(context, storagePref));
    }

    public static class StackElement implements Serializable, Parcelable {

        public final Class connectedFragmentClass;
        public final Bundle extras;
        public final @Type String type;

        public StackElement(@Type String type, Class connectedFragmentClass, Bundle extras) {
            this.connectedFragmentClass = connectedFragmentClass;
            this.extras = extras;
            this.type = type;
        }

        public StackElement(Parcel in){
            Object[] data = in.readArray(StackElement.class.getClassLoader());
            this.connectedFragmentClass = (Class) data[0];
            this.extras = (Bundle) data[1];
            this.type = (String) data[2];
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeArray(new Object[] {
                    connectedFragmentClass,
                    extras,
                    type
            });
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public StackElement createFromParcel(Parcel in) {
                return new StackElement(in);
            }
            public StackElement[] newArray(int size) {
                return new StackElement[size];
            }
        };
    }
}
