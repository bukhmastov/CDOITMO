package com.bukhmastov.cdoitmo.network.provider.impl;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.R;
import com.bukhmastov.cdoitmo.factory.AppComponentProvider;
import com.bukhmastov.cdoitmo.network.provider.NetworkClientProvider;
import com.bukhmastov.cdoitmo.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dagger.Lazy;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class NetworkClientProviderImpl implements NetworkClientProvider {

    private static final String TAG = "NetworkClientProvider";
    private static final int CONNECT_TIMEOUT_SEC = 10;
    private static final int READ_TIMEOUT_SEC = 20;
    private OkHttpClient client = null;

    @Inject
    Lazy<Log> log;
    @Inject
    Lazy<Context> context;

    public NetworkClientProviderImpl() {
        AppComponentProvider.getComponent().inject(this);
    }

    @Override
    public OkHttpClient get() {
        if (client == null) {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.addInterceptor(new LoggingInterceptor());
            builder.followRedirects(false);
            builder.connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
            builder.readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS);
            addTrustedCertificates(builder);
            client = builder.build();
        }
        return client;
    }

    private void addTrustedCertificates(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Started from android N (24), trusting to certificates delegated
            // to networkSecurityConfig (at AndroidManifest.xml -> application.networkSecurityConfig)
            return;
        }
        try {
            X509TrustManager trustManager = makeTrustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (Exception e) {
            log.get().w(TAG, "Failed to add trusted certificates");
            log.get().exception(e);
        }
    }

    private InputStream trustedCertificatesInputStream() {
        Resources res = context.get().getResources();
        return res.openRawResource(R.raw.trusted_certificates);
    }

    private X509TrustManager makeTrustManagerForCertificates(InputStream in) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }
        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = makeEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }
        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore makeEmptyKeyStore(char[] password) throws Exception {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null;
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
            okhttp3.Request request = chain.request();
            log.get().v(TAG,
                    "interceptor | request | " +
                            request.method() + " | " +
                            getUrl(request.url())
            );
            long t1 = System.nanoTime();
            Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            log.get().v(TAG,
                    "interceptor | response | " +
                            response.request().method() + " | " +
                            getUrl(response.request().url()) + " | " +
                            ((t2 - t1) / 1e6d) + "ms" + " | " +
                            response.code() + " | " +
                            response.message()
            );
            return response;
        }
        private String getUrl(HttpUrl httpUrl) {
            try {
                if (httpUrl == null) {
                    return "<null>";
                }
                String url = httpUrl.toString();
                if (url.contains("isu.ifmo.ru") && url.contains("api/core")) {
                    url = url.replaceAll("[^/]{64}", "<apikey>");
                    url = url.replaceAll("AT-[^/]*", "<auth-token>");
                }
                if (url.contains("services.ifmo.ru")) {
                    Matcher m = Pattern.compile("^(.*oauth2\\.0/)(.*)$", Pattern.CASE_INSENSITIVE).matcher(url);
                    if (m.find()) {
                        url = m.replaceAll("$1<hidden>");
                    }
                }
                return url;
            } catch (Exception e) {
                return "<error>";
            }
        }
    }
}
