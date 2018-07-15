package com.bukhmastov.cdoitmo.network.provider.impl;

import android.support.annotation.NonNull;

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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.Buffer;

public class NetworkClientProviderImpl implements NetworkClientProvider {

    private static final String TAG = "NetworkClientProvider";
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 30;
    private OkHttpClient client = null;

    //@Inject
    private Log log = Log.instance();

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
        try {
            X509TrustManager trustManager = trustManagerForCertificates(trustedCertificatesInputStream());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (Exception e) {
            log.w(TAG, "Failed to add trusted certificates");
            log.exception(e);
        }
        return builder;
    }

    private InputStream trustedCertificatesInputStream() {
        /*
         * Certificate for: ITMO UNIVERSITY (*.ifmo.ru)
         * Issued by: GeoTrust TLS RSA CA G1 (www.digicert.com)
         * Valid: ‎21 dec ‎2017 - 19 feb ‎2020
         * Certificate at pem format:
         */
        String deifmoRsaCertificationAuthority = "-----BEGIN CERTIFICATE-----\n" +
                "MIIGaDCCBVCgAwIBAgIQBNf0DeMvYXonJ8XhQ5nwBDANBgkqhkiG9w0BAQsFADBg\n" +
                "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
                "d3cuZGlnaWNlcnQuY29tMR8wHQYDVQQDExZHZW9UcnVzdCBUTFMgUlNBIENBIEcx\n" +
                "MB4XDTE3MTIyMTAwMDAwMFoXDTIwMDIxOTEyMDAwMFowVjELMAkGA1UEBhMCUlUx\n" +
                "GTAXBgNVBAcTEFNhaW50IFBldGVyc2J1cmcxGDAWBgNVBAoTD0lUTU8gVU5JVkVS\n" +
                "U0lUWTESMBAGA1UEAwwJKi5pZm1vLnJ1MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
                "MIIBCgKCAQEA1vEpSSKivybXfZgDxBPQksD6z6IdJrMRjD08XiKkZcCrmFfj7rnO\n" +
                "tNcuQghiI2BpM6G2PgXGaFrhpLIeK11LYRcRES02zVXhftvGvdrzsKU31f2JeDQI\n" +
                "XDdG8PWgdbDkGcwaoh0tu8wCjH3DsZJfqjqcwPRU5zum1qEUXAeQJe1hy1s/C7nb\n" +
                "LQOfss/CgwMlHnsktgVGKo/OXkH0Vx+QcsJLLWYksA2oJZng571KLSkWrng8YSNj\n" +
                "ZaiA0achBVJaV7pVMTZ2exxY2vdPpM9/goJdBjnNydQi4I3Li+TNns6Xh5geMPgh\n" +
                "dj/Vro+KMWhl5GkVtaWnhp7QUv4NVHNHYQIDAQABo4IDJjCCAyIwHwYDVR0jBBgw\n" +
                "FoAUlE/UXYvkpOKmgP792PkA76O+AlcwHQYDVR0OBBYEFPdM63+7+oL0VeDSfujZ\n" +
                "dfhSUEBlMB0GA1UdEQQWMBSCCSouaWZtby5ydYIHaWZtby5ydTAOBgNVHQ8BAf8E\n" +
                "BAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMD8GA1UdHwQ4MDYw\n" +
                "NKAyoDCGLmh0dHA6Ly9jZHAuZ2VvdHJ1c3QuY29tL0dlb1RydXN0VExTUlNBQ0FH\n" +
                "MS5jcmwwTAYDVR0gBEUwQzA3BglghkgBhv1sAQEwKjAoBggrBgEFBQcCARYcaHR0\n" +
                "cHM6Ly93d3cuZGlnaWNlcnQuY29tL0NQUzAIBgZngQwBAgIwdgYIKwYBBQUHAQEE\n" +
                "ajBoMCYGCCsGAQUFBzABhhpodHRwOi8vc3RhdHVzLmdlb3RydXN0LmNvbTA+Bggr\n" +
                "BgEFBQcwAoYyaHR0cDovL2NhY2VydHMuZ2VvdHJ1c3QuY29tL0dlb1RydXN0VExT\n" +
                "UlNBQ0FHMS5jcnQwCQYDVR0TBAIwADCCAX4GCisGAQQB1nkCBAIEggFuBIIBagFo\n" +
                "AHcApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFgeIc1WgAABAMA\n" +
                "SDBGAiEAx828SBz2uAVD4Esva7QIzstUbunWgtWKd7R+62yKSGECIQCNC1KgDmkJ\n" +
                "DQAX3mDl285U89YIg+w6FQGQs18QGlByggB1AId1v+dZfPiMQ5lfvfNu/1aNR1Y2\n" +
                "/0q1YMG06v9eoIMPAAABYHiHNqcAAAQDAEYwRAIgRixqDDO8p6uObzrDKC1o5u21\n" +
                "7jUjQ8sG0gMEjv4NScACIFbVNPE1P77C60WwfbxTEz622vWjsg1FkCOzQ/siVNmQ\n" +
                "AHYAu9nfvB+KcbWTlCOXqpJ7RzhXlQqrUugakJZkNo4e0YUAAAFgeIc2KgAABAMA\n" +
                "RzBFAiEAuZMvRaGzl2jPNWiehhso1byGO46tjxChGjZk8B683W0CIBOSg+DDeaLN\n" +
                "s22cpe5VkXolQTwhHww3GV5PTEZxUttoMA0GCSqGSIb3DQEBCwUAA4IBAQAeb0Np\n" +
                "Y9MahNWXFfQIntW8Q6Cd1unOs8mt3G5G8y5GhuU+DZj5Iw4xAricAB/QUGGbrGtf\n" +
                "Q3R9BA6S6elAcpUUaA1wuGUjh/yfZYx+VBTVhRhCgjO8J5KdkGzMSc6cArCbrkv6\n" +
                "hNm8xAFtcp3/zOG+UaBaGd/dn6NCduoGSmE5YvOrf/HIvZB8REnlGhlG1JePd4WN\n" +
                "lgsLC1rX8l65KiP/66j/S0krnDuFl7d/T8m1uAiUKGmGGPjM+gXnrVlcsnSJtkN9\n" +
                "Li0qjL7Ei2eH7gHaxyzJ7bA6wodR3S3u/+MA+G/4jprDQjJv/z42hcERdw+d1uku\n" +
                "VOepcc82nrVd0OOQ\n" +
                "-----END CERTIFICATE-----";
        return new Buffer()
                .writeUtf8(deifmoRsaCertificationAuthority)
                .inputStream();
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
            log.v(TAG,
                    "interceptor | request | " +
                            request.method() + " | " +
                            request.url()
            );
            long t1 = System.nanoTime();
            Response response = chain.proceed(request);
            long t2 = System.nanoTime();
            log.v(TAG,
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
