package jmeter.plugins.http2.sampler;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttp2Client
{
    private static final Logger LOG = LoggingManager.getLoggerForClass();

    private final OkHttpClient client;

    private final int connectTimeout = 2500;
    private final int readTimeout = 10000;
    private final int writeTimeout = 5000;
    

    public OkHttp2Client()
    {
        List<ConnectionSpec> connSpecs = new ArrayList<ConnectionSpec>();
        connSpecs.add(new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS).allEnabledCipherSuites().allEnabledTlsVersions().build());
        connSpecs.add(ConnectionSpec.CLEARTEXT);

        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);

        ConnectionPool connPool = new ConnectionPool(5, 10, TimeUnit.SECONDS);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionSpecs(connSpecs)
                .connectionPool(connPool)
                .protocols(protocols)
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
              ;

        builder = trustAllSslCerts(builder);

        client = builder.build();

        LOG.info("Inititalized new OkHttpClient.");
    }

    private OkHttpClient.Builder trustAllSslCerts(OkHttpClient.Builder builder)
    {
        // Create a trust manager that does not validate certificate chains
        X509TrustManager[] trustAllCerts = new X509TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        builder = builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllCerts[0]);

        // Don't verify hostname matches the ssl cert.
        builder = builder.hostnameVerifier(new HostnameVerifier()
        {
            public boolean verify(String s, SSLSession sslSession)
            {
                return true;
            }
        });

        return builder;
    }

    public void shutdown()
    {
        client.connectionPool().evictAll();
        client.dispatcher().executorService().shutdown();
    }

    public HTTPSampleResult request(String method, String scheme, String host, int port, String path, HeaderManager headerManager)
    {
        LOG.debug(String.format("REQUEST: method=%s, scheme=%s, host=%s, port=%s, path=%s", method, scheme, host, port, path));

        HTTPSampleResult sampleResult = new HTTPSampleResult();
        sampleResult.sampleStart();

        HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
        if (path != null) urlBuilder = urlBuilder.encodedPath(path);
        HttpUrl url = urlBuilder.scheme(scheme).host(host).port(port).build();

        sampleResult.setURL(url.url());
        sampleResult.setHTTPMethod(method);

        // Setup the request headers.
        Headers.Builder headersBuilder = new Headers.Builder();
        for (int i=0; i<headerManager.size(); i++) {
            Header hdr = headerManager.get(i);
            String name = hdr.getName();
            String value = hdr.getValue();
            if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(value)) {
                headersBuilder.add(name, value);
            }
        }
        
        Request request = new Request.Builder()
                .method(method, null)
                .url(url)
                .headers(headersBuilder.build())
                .build();

        // Execute the request.
        try {
            long startTime = System.currentTimeMillis();
            Response response = client.newCall(request).execute();

            long duration = System.currentTimeMillis() - startTime;
            LOG.debug("Received response: status=" + response.code() + ", duration=" + duration + ", response=" + response.toString());

            sampleResult.setSuccessful(true);

            sampleResult.setResponseCode(String.valueOf(response.code()));
            sampleResult.setResponseMessage(response.message());
            sampleResult.setResponseHeaders(getResponseHeadersAsString(response));

            response.close();
        }
        catch (IOException e) {
            LOG.debug("IOEXception executing request. url = " + String.valueOf(url), e);
            sampleResult.setSuccessful(false);
        }

        sampleResult.sampleEnd();
        
        
        return sampleResult;
    }

    /**
     * Convert Response headers set to one String instance
     */
    private String getResponseHeadersAsString(Response response) 
    {
        StringBuilder headerBuf = new StringBuilder();
        
        for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) 
        {
            for (String value : entry.getValue()) {
                headerBuf.append(entry.getKey());
                headerBuf.append(": ");
                headerBuf.append(value);
                headerBuf.append("\n");
            }
        }

        return headerBuf.toString();
    }
}
