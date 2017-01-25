package jmeter.plugins.http2.sampler;

import okhttp3.*;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OkHttp2Client
{
    private final String method;
    private final String scheme;
    private final String host;
    private final int port;
    private final String path;
    private final HeaderManager headerManager;

    private final OkHttpClient client;
    

    public OkHttp2Client(String method, String scheme, String host, int port, String path, HeaderManager headerManager) 
    {
        this.method = method;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
        this.headerManager = headerManager;

        List<ConnectionSpec> connSpecs = new ArrayList<ConnectionSpec>();
        connSpecs.add(ConnectionSpec.MODERN_TLS);
        
        List<Protocol> protocols = new ArrayList<Protocol>();
        protocols.add(Protocol.HTTP_2);
        protocols.add(Protocol.HTTP_1_1);
        
        client = new OkHttpClient.Builder()
                .connectionSpecs(connSpecs)
                .protocols(protocols)
                .build();
    }

    public SampleResult request()
    {
        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();

        HttpUrl url = new HttpUrl.Builder().scheme(scheme).host(host).port(port).encodedPath(path).build();
        sampleResult.setURL(url.url());

        Headers.Builder headersBuilder = new Headers.Builder();

        PropertyIterator headerItr = headerManager.getHeaders().iterator();
        while (headerItr.hasNext()) {
            JMeterProperty prop = headerItr.next();
            String name = prop.getName();
            String value = prop.getStringValue();
            headersBuilder.add(name, value);
        }
        
        Request request = new Request.Builder()
                .method(method, null)
                .url(url)
                .headers(headersBuilder.build())
                .build();

        try {
            Response response = client.newCall(request).execute();
            sampleResult.setSuccessful(true);
            
            sampleResult.setResponseCode(String.valueOf(response.code()));
            sampleResult.setResponseMessage(response.message());
            sampleResult.setResponseHeaders(getResponseHeadersAsString(response));
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
