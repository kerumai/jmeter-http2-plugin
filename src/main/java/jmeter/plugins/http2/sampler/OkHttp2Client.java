package jmeter.plugins.http2.sampler;

import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;


import java.io.IOException;
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
        connSpecs.add(ConnectionSpec.MODERN_TLS);

        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);

        client = new OkHttpClient.Builder()
                .connectionSpecs(connSpecs)
                .protocols(protocols)
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .build();
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
            Response response = client.newCall(request).execute();
            sampleResult.setSuccessful(true);

            sampleResult.setResponseCode(String.valueOf(response.code()));
            sampleResult.setResponseMessage(response.message());
            sampleResult.setResponseHeaders(getResponseHeadersAsString(response));

            response.close();
        }
        catch (IOException e) {
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
