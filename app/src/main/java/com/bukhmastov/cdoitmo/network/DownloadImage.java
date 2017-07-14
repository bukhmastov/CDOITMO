package com.bukhmastov.cdoitmo.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.bukhmastov.cdoitmo.utils.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

public class DownloadImage extends AsyncTask<String, Integer, Bitmap> {

    public interface response {
        void finish(Bitmap bitmap);
    }
    private response delegate = null;

    public DownloadImage(response delegate){
        this.delegate = delegate;
    }

    @Override
    protected Bitmap doInBackground(String... arg0) {
        Log.i("qweasdzxzzxc", "opened");
        Bitmap bitmap = null;
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            String path = arg0[0].trim();
            if (!path.isEmpty()) {
                inputStream = new URL(path).openStream();
                bufferedInputStream = new BufferedInputStream(inputStream);
                bitmap = BitmapFactory.decodeStream(bufferedInputStream);
            }
        } catch (Exception e) {
            // do nothing
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ex) {
                // do nothing
            }
            try {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            } catch (Exception ex) {
                // do nothing
            }
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        delegate.finish(bitmap);
    }

}