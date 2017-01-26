package jmeter.plugins.http2.sampler;

import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.Interruptible;

public class HTTP2Sampler extends HTTPSamplerBase implements Interruptible
{
    private final OkHttp2Client client;

    public HTTP2Sampler() {
        super();
        setName("HTTP2 OkHttp Sampler");
        client = new OkHttp2Client();
    }

    public boolean interrupt()
    {
        if (client != null) {
            client.shutdown();
        }
        return true;
    }
    
    public HTTPSampleResult sample(java.net.URL url, String method, boolean followRedirects, int depth)
    {


        HTTPSampleResult res = client.request(getMethod(), getProtocol(), getDomain(), getPort(), getPath(), getHeaderManager());
        res.setSampleLabel(getName());

        return res;
    }
}

