package com.bukhmastov.cdoitmo.network.provider.impl;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import android.support.annotation.NonNull;

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

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import dagger.Lazy;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class NetworkClientProviderImpl implements NetworkClientProvider {

    private static final String TAG = "NetworkClientProvider";
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
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
            client = trustDefinedCertificates(
                    new OkHttpClient().newBuilder()
                            .addInterceptor(new LoggingInterceptor())
                            .followRedirects(false)
                            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            ).build();
        }
        return client;
    }

    private OkHttpClient.Builder trustDefinedCertificates(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Started from android N (24), trusting to certificates delegated to networkSecurityConfig
            return builder;
        }
        try {
            X509TrustManager trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (Exception e) {
            log.get().w(TAG, "Failed to add trusted certificates");
            log.get().exception(e);
        }
        return builder;
    }

    private InputStream trustedCertificatesInputStream() {
        Resources res = context.get().getResources();
        return res.openRawResource(R.raw.trusted_certificates);
    }

    private X509TrustManager trustManagerForCertificates(InputStream in) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }
        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
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

    private KeyStore newEmptyKeyStore(char[] password) throws Exception {
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
                            request.url()
            );
            long t1 = System.nanoTime();
            Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            log.get().v(TAG,
                    "interceptor | response | " +
                            response.request().method() + " | " +
                            response.request().url() + " | " +
                            ((t2 - t1) / 1e6d) + "ms" + " | " +
                            response.code() + " | " +
                            response.message()
            );
            return response;
        }
    }
}
