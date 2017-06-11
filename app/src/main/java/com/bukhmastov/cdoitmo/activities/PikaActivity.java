package com.bukhmastov.cdoitmo.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.utils.CtxWrapper;
import com.bukhmastov.cdoitmo.utils.Log;

import java.util.Random;

public class PikaActivity extends AppCompatActivity {

    private static final String TAG = "PikaActivity";
    private final Random random = new Random();
    private boolean dimas = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pika);
        Log.v(TAG, "PIKA is no longer hiding");
        overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
        if (random.nextInt(6) % 6 == 0) {
            Log.v(TAG, "LEGENDARY D1MA$ APPEARS, so pika went away");
            dimas = true;
            ((ImageView) findViewById(R.id.image)).setImageDrawable(getDrawable(R.drawable.wuwari));
        }
        View pika_container = findViewById(R.id.pika_container);
        if (pika_container != null) {
            pika_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    overridePendingTransition(R.anim.zoom_bottom_in, R.anim.zoom_bottom_out);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dimas) {
            Log.v(TAG, "D1MA$ left us to not to be late for some movies");
        } else {
            Log.v(TAG, "PIKA left us to stream some games");
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(CtxWrapper.wrap(context));
    }

}
