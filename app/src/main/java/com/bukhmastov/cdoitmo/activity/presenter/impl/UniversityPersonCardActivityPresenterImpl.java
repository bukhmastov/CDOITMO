package com.bukhmastov.cdoitmo.activity.presenter.impl;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.activity.UniversityPersonCardActivity;
import com.bukhmastov.cdoitmo.activity.presenter.UniversityPersonCardActivityPresenter;
import com.bukhmastov.cdoitmo.event.bus.EventBus;
import com.bukhmastov.cdoitmo.event.events.OpenIntentEvent;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.firebase.FirebaseAnalyticsProvider;
import com.bukhmastov.cdoitmo.model.university.persons.UPerson;
import com.bukhmastov.cdoitmo.network.IfmoRestClient;
import com.bukhmastov.cdoitmo.network.handlers.RestResponseHandler;
import com.bukhmastov.cdoitmo.network.model.Client;
import com.bukhmastov.cdoitmo.util.Log;
import com.bukhmastov.cdoitmo.util.Static;
import com.bukhmastov.cdoitmo.util.Thread;
import com.bukhmastov.cdoitmo.util.singleton.Color;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;
import com.bukhmastov.cdoitmo.view.CircularTransformation;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.bukhmastov.cdoitmo.util.Thread.UPC;

public class UniversityPersonCardActivityPresenterImpl implements UniversityPersonCardActivityPresenter, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "UniversityPersonCard";
    private UniversityPersonCardActivity activity = null;
    private Client.Request requestHandle = null;
    private boolean loaded = false;
    private boolean firstLoad = true;
    private UPerson person = null;
    private int pid = -1;

    @Inject
    Log log;
    @Inject
    Thread thread;
    @Inject
    EventBus eventBus;
    @Inject
    IfmoRestClient ifmoRestClient;
    @Inject
    Static staticUtil;
    @Inject
    FirebaseAnalyticsProvider firebaseAnalyticsProvider;

    public UniversityPersonCardActivityPresenterImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public void setActivity(@NonNull UniversityPersonCardActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        thread.initialize(UPC);
        thread.runOnUI(UPC, () -> {
            log.i(TAG, "Activity created");
            Intent intent = activity.getIntent();
            if (intent == null) {
                activity.finish();
                return;
            }
            Bundle extras = intent.getExtras();
            if (extras == null) {
                activity.finish();
                return;
            }
            boolean ok = false;
            if (extras.containsKey("person")) {
                try {
                    person = (UPerson) extras.getSerializable("person");
                    pid = person.getId();
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok && extras.containsKey("pid")) {
                try {
                    person = null;
                    pid = extras.getInt("pid");
                    ok = true;
                } catch (Exception e) {
                    ok = false;
                }
            }
            if (!ok || pid < 0) {
                activity.finish();
                return;
            }
            firebaseAnalyticsProvider.logCurrentScreen(activity);
        }, throwable -> {
            activity.finish();
        });
    }

    @Override
    public void onResume() {
        thread.run(UPC, () -> {
            log.v(TAG, "resumed");
            firebaseAnalyticsProvider.setCurrentScreen(activity);
            if (!loaded) {
                loaded = true;
                load();
            }
        });
    }

    @Override
    public void onPause() {
        log.v(TAG, "paused");
        thread.standalone(() -> {
            if (requestHandle != null && requestHandle.cancel()) {
                loaded = false;
            }
        });
    }

    @Override
    public void onDestroy() {
        log.v(TAG, "Fragment destroyed");
        loaded = false;
        thread.interrupt(UPC);
    }

    @Override
    public void onRefresh() {
        thread.run(UPC, () -> {
            log.v(TAG, "refreshing");
            person = null;
            load();
        });
    }

    private void load() {
        thread.run(UPC, () -> {
            if (person != null) {
                display();
                return;
            }
            loadProvider(new RestResponseHandler<UPerson>() {
                @Override
                public void onSuccess(int code, Client.Headers headers, UPerson response) {
                    thread.runOnUI(UPC, () -> {
                        SwipeRefreshLayout swipe = activity.findViewById(R.id.person_swipe);
                        if (swipe != null) {
                            swipe.setRefreshing(false);
                        }
                    });
                    thread.run(UPC, () -> {
                        if (code == 200 && response != null) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                response.setPost(android.text.Html.fromHtml(response.getPost(), android.text.Html.FROM_HTML_MODE_LEGACY).toString());
                            } else {
                                response.setPost(android.text.Html.fromHtml(response.getPost()).toString());
                            }
                            person = response;
                            display();
                            return;
                        }
                        loadFailed();
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onFailure(int code, Client.Headers headers, int state) {
                    thread.runOnUI(UPC, () -> {
                        log.v(TAG, "load | code = ", code, " | failure ", state);
                        SwipeRefreshLayout swipe = activity.findViewById(R.id.person_swipe);
                        if (swipe != null) {
                            swipe.setRefreshing(false);
                        }
                        if (code == 404) {
                            loadNotFound();
                            return;
                        }
                        if (state == Client.FAILED_OFFLINE) {
                            activity.draw(R.layout.state_offline_text);
                            View reload = activity.findViewById(R.id.offline_reload);
                            if (reload != null) {
                                reload.setOnClickListener(v -> load());
                            }
                            return;
                        }
                        activity.draw(R.layout.state_failed_button);
                        TextView message = activity.findViewById(R.id.try_again_message);
                        if (message != null) {
                            message.setText(ifmoRestClient.getFailedMessage(activity, code, state));
                        }
                        View reload = activity.findViewById(R.id.try_again_reload);
                        if (reload != null) {
                            reload.setOnClickListener(v -> load());
                        }
                    }, throwable -> {
                        loadFailed();
                    });
                }
                @Override
                public void onProgress(int state) {
                    thread.runOnUI(UPC, () -> {
                        log.v(TAG, "load | progress ", state);
                        if (!firstLoad) {
                            return;
                        }
                        activity.draw(R.layout.state_loading_text);
                        TextView message = activity.findViewById(R.id.loading_message);
                        if (message != null) {
                            message.setText(ifmoRestClient.getProgressMessage(activity, state));
                        }
                    });
                }
                @Override
                public void onNewRequest(Client.Request request) {
                    requestHandle = request;
                }
                @Override
                public UPerson newInstance() {
                    return new UPerson();
                }
            });
        }, throwable -> {
            loadFailed();
        });
    }

    private void loadProvider(RestResponseHandler<UPerson> handler) {
        log.v(TAG, "loadProvider");
        ifmoRestClient.get(activity, "person/" + pid, null, handler);
    }

    private void loadFailed() {
        thread.runOnUI(UPC, () -> {
            log.v(TAG, "loadFailed");
            activity.draw(R.layout.state_failed_button);
            TextView message = activity.findViewById(R.id.try_again_message);
            if (message != null) {
                message.setText(R.string.load_failed);
            }
            View reload = activity.findViewById(R.id.try_again_reload);
            if (reload != null) {
                reload.setOnClickListener(v -> load());
            }
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void loadNotFound() {
        thread.runOnUI(UPC, () -> {
            log.v(TAG, "loadNotFound");
            activity.draw(R.layout.state_nothing_to_display_person);
            activity.findViewById(R.id.web).setOnClickListener(view -> thread.run(UPC, () -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + pid + "/"));
                eventBus.fire(new OpenIntentEvent(intent));
            }));
        }, throwable -> {
            log.exception(throwable);
        });
    }

    private void display() {
        thread.runOnUI(UPC, () -> {
            if (person == null) {
                loadFailed();
                return;
            }
            firstLoad = false;
            activity.draw(R.layout.layout_university_person_card);
            activity.findViewById(R.id.person_header).setPadding(0, getStatusBarHeight(), 0, 0);
            // кнопка назад
            activity.findViewById(R.id.back).setOnClickListener(v -> activity.finish());
            // кнопка сайта
            activity.findViewById(R.id.web).setOnClickListener(view -> thread.run(UPC, () -> {
                if (person == null) {
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.ifmo.ru/ru/viewperson/" + person.getId() + "/"));
                eventBus.fire(new OpenIntentEvent(intent));
            }));
            // заголовок
            String name = (getStringIfExists(person.getLastName()) + " " + getStringIfExists(person.getFirstName()) + " " + getStringIfExists(person.getMiddleName())).trim();
            ((TextView) activity.findViewById(R.id.name)).setText(name);
            if (StringUtils.isNotBlank(person.getDegree())) {
                ((TextView) activity.findViewById(R.id.degree)).setText(StringUtils.capitalizeFirstLetter(person.getDegree()));
            } else {
                staticUtil.removeView(activity.findViewById(R.id.degree));
            }
            new Picasso.Builder(activity).build()
                    .load(person.getImage())
                    .error(R.drawable.ic_sentiment_very_satisfied_white)
                    .transform(new CircularTransformation())
                    .into((ImageView) activity.findViewById(R.id.avatar));
            // контент
            ViewGroup infoConnectContainer = activity.findViewById(R.id.info_connect_container);
            if (infoConnectContainer != null) {
                boolean exists = false;
                if (StringUtils.isNotBlank(person.getPhone())) {
                    String[] phones = person.getPhone().trim().split("[;,]");
                    for (String phone : phones) {
                        if (StringUtils.isNotBlank(phone)) {
                            infoConnectContainer.addView(getConnectContainer(R.drawable.ic_phone, phone.trim(), exists, v -> thread.run(UPC, () -> {
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                eventBus.fire(new OpenIntentEvent(intent));
                            })));
                            exists = true;
                        }
                    }
                }
                if (StringUtils.isNotBlank(person.getEmail())) {
                    String[] emails = person.getEmail().trim().split("[;,]");
                    for (String email : emails) {
                        if (StringUtils.isNotBlank(email)) {
                            infoConnectContainer.addView(getConnectContainer(R.drawable.ic_email, email.trim(), exists, v -> thread.run(UPC, () -> {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("message/rfc822");
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email.trim()});
                                Intent chooser = Intent.createChooser(intent, activity.getString(R.string.send_mail) + "...");
                                eventBus.fire(new OpenIntentEvent(chooser));
                            })));
                            exists = true;
                        }
                    }
                }
                if (StringUtils.isNotBlank(person.getWww())) {
                    String[] webs = person.getWww().trim().split("[;,]");
                    for (String web : webs) {
                        if (StringUtils.isNotBlank(web)) {
                            infoConnectContainer.addView(getConnectContainer(R.drawable.ic_web, web.trim(), exists, v -> thread.run(UPC, () -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(web.trim()));
                                eventBus.fire(new OpenIntentEvent(intent));
                            })));
                            exists = true;
                        }
                    }
                }
            }
            ViewGroup infoAboutContainer = activity.findViewById(R.id.info_about_container);
            if (infoAboutContainer != null) {
                if (StringUtils.isNotBlank(person.getRank())) {
                    infoAboutContainer.addView(getAboutContainer(activity.getString(R.string.person_rank), StringUtils.capitalizeFirstLetter(person.getRank())));
                }
                if (StringUtils.isNotBlank(person.getPost())) {
                    infoAboutContainer.addView(getAboutContainer(activity.getString(R.string.person_post), StringUtils.capitalizeFirstLetter(person.getPost())));
                }
                if (StringUtils.isNotBlank(person.getText())) {
                    infoAboutContainer.addView(getAboutContainer(activity.getString(R.string.person_bio), person.getText()));
                }
            }
            // работаем со свайпом
            SwipeRefreshLayout swipe = activity.findViewById(R.id.person_swipe);
            if (swipe != null) {
                swipe.setColorSchemeColors(Color.resolve(activity, R.attr.colorAccent));
                swipe.setProgressBackgroundColorSchemeColor(Color.resolve(activity, R.attr.colorBackgroundRefresh));
                swipe.setOnRefreshListener(this);
            }
        }, throwable -> {
            log.exception(throwable);
            loadFailed();
        });
    }

    private View getConnectContainer(@DrawableRes int icon, String text, boolean isRemoveSeparator, View.OnClickListener listener) {
        View activity_university_person_card_connect = activity.inflate(R.layout.layout_university_connect);
        ((ImageView) activity_university_person_card_connect.findViewById(R.id.connect_image)).setImageResource(icon);
        ((TextView) activity_university_person_card_connect.findViewById(R.id.connect_text)).setText(text.trim());
        if (listener != null) {
            activity_university_person_card_connect.setOnClickListener(listener);
        }
        if (!isRemoveSeparator) {
            staticUtil.removeView(activity_university_person_card_connect.findViewById(R.id.separator));
        }
        return activity_university_person_card_connect;
    }

    private View getAboutContainer(String title, String text) {
        View activity_university_person_card_about = activity.inflate(R.layout.layout_university_person_card_about);
        ((TextView) activity_university_person_card_about.findViewById(R.id.title)).setText(title);
        TextView textView = activity_university_person_card_about.findViewById(R.id.text);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textView.setText(android.text.Html.fromHtml(text, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim());
        } else {
            textView.setText(android.text.Html.fromHtml(text).toString().trim());
        }
        return activity_university_person_card_about;
    }

    private String getStringIfExists(String value) {
        return StringUtils.isNotBlank(value) ? value : "";
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
