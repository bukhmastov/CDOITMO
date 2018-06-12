package com.bukhmastov.cdoitmo.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringDef;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.fragment.ConnectedFragment;
import com.bukhmastov.cdoitmo.util.CtxWrapper;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public abstract class ConnectedActivity extends AppCompatActivity {

    private static final String TAG = "ConnectedActivity";
    private final ArrayList<StackElement> stack = new ArrayList<>();
    private static final String STATE_STORED_FRAGMENT_NAME = "storedFragmentName";
    private static final String STATE_STORED_FRAGMENT_DATA = "storedFragmentData";
    private static final String STATE_STORED_FRAGMENT_EXTRA = "storedFragmentExtra";
    public final static String ACTIVITY_WITH_MENU = "connected_activity_with_align";
    protected boolean layout_with_menu = true;
    public Menu toolbar = null;
    public String storedFragmentName = null;
    public String storedFragmentData = null;
    public String storedFragmentExtra = null;
    protected final ConnectedActivity activity = this;

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({TYPE.ROOT, TYPE.STACKABLE})
    public @interface Type {}
    public static class TYPE {
        public static final String ROOT = "root";
        public static final String STACKABLE = "stackable";
    }

    protected class StackElement {
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

    protected abstract @IdRes int getRootViewId();

    public boolean openActivityOrFragment(Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(TYPE.STACKABLE, connectedFragmentClass, extras);
    }
    public boolean openActivityOrFragment(@Type String type, Class connectedFragmentClass, Bundle extras) {
        return openActivityOrFragment(new StackElement(type, connectedFragmentClass, extras));
    }
    public boolean openActivityOrFragment(StackElement stackElement) {
        Log.v(TAG, "openActivityOrFragment | type=" + stackElement.type + " | class=" + stackElement.connectedFragmentClass.toString());
        if (Static.tablet) {
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
        Log.v(TAG, "openFragment | type=" + stackElement.type + " | class=" + stackElement.connectedFragmentClass.toString());
        try {
            ConnectedFragment.Data data = ConnectedFragment.getData(this, stackElement.connectedFragmentClass);
            if (data == null) {
                throw new NullPointerException("data cannot be null");
            }
            ViewGroup root_layout = findViewById(getRootViewId());
            if (root_layout != null) {
                root_layout.removeAllViews();
            }
            ConnectedFragment connectedFragment = (ConnectedFragment) data.connectedFragmentClass.newInstance();
            if (stackElement.extras != null) {
                connectedFragment.setArguments(stackElement.extras);
            }
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (fragmentTransaction != null) {
                    fragmentTransaction.replace(getRootViewId(), connectedFragment);
                    fragmentTransaction.commitAllowingStateLoss();
                    pushFragment(stackElement);
                    updateToolbar(this, data.title, layout_with_menu ? data.image : null);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            Static.error(e);
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
        Log.v(TAG, "openActivity | type=" + stackElement.type + " | class=" + stackElement.connectedFragmentClass.toString());
        /*
         * We don't care about type. This is a harsh life :c
         */
        try {
            Intent intent = new Intent(this, FragmentActivity.class);
            intent.putExtra("class", stackElement.connectedFragmentClass);
            intent.putExtra("extras", stackElement.extras);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            Static.error(e);
            return false;
        }
    }

    public boolean back() {
        Log.v(TAG, "back | stack.size=" + stack.size());
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
        Log.v(TAG, "pushFragment | type=" + stackElement.type + " | class=" + stackElement.connectedFragmentClass.toString());
        if (stackElement.type.equals(TYPE.ROOT)) {
            stack.clear();
        }
        stack.add(stackElement);
        Log.v(TAG, "stack.size() = " + stack.size());
    }
    public void removeFragment(Class connectedFragmentClass) {
        Log.v(TAG, "removeFragment | class=" + connectedFragmentClass.toString());
        for (int i = stack.size() - 1; i >= 0; i--) {
            StackElement stackElement = stack.get(i);
            if (stackElement.connectedFragmentClass == connectedFragmentClass) {
                if (!stackElement.type.equals(TYPE.ROOT)) {
                    stack.remove(stackElement);
                } else {
                    Log.e(TAG, "removeFragment | Root fragment removal from the stack prevented");
                }
                break;
            }
        }
        Log.v(TAG, "stack.size() = " + stack.size());
    }

    public void updateToolbar(final Context context, final String title, final Integer image) {
        Static.T.runOnUiThread(() -> {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
                if (image == null || !Static.tablet) {
                    actionBar.setHomeButtonEnabled(true);
                    actionBar.setLogo(null);
                } else {
                    actionBar.setHomeButtonEnabled(false);
                    Drawable drawable = getDrawable(image);
                    if (drawable != null) {
                        try {
                            drawable.setTint(Static.resolveColor(context, R.attr.colorToolbarContent));
                        } catch (Exception ignore) {
                            // ignore
                        }
                        actionBar.setLogo(drawable);
                    }
                }
            }
        });
    }

    protected View inflate(@LayoutRes int layout) throws InflateException {
        if (activity == null) {
            Log.e(TAG, "Failed to inflate layout, activity is null");
            return null;
        }
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            Log.e(TAG, "Failed to inflate layout, inflater is null");
            return null;
        }
        return inflater.inflate(layout, null);
    }
    protected void draw(int layoutId) {
        try {
            draw(inflate(layoutId));
        } catch (Exception e){
            Static.error(e);
        }
    }
    protected void draw(View view) {
        try {
            ViewGroup vg = findViewById(getRootViewId());
            if (vg != null) {
                vg.removeAllViews();
                vg.addView(view);
            }
        } catch (Exception e){
            Static.error(e);
        }
    }

    protected void clearStore() {
        storedFragmentName = null;
        storedFragmentData = null;
        storedFragmentExtra = null;
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }
}
