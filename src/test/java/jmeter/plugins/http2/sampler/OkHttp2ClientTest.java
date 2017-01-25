package jmeter.plugins.http2.sampler;

import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;

public class OkHttp2ClientTest
{
    public static void main(String[] args)
    {
        try {
            HeaderManager hm = new HeaderManager();
            OkHttp2Client client = new OkHttp2Client("get", "https", "apis.netflix.com", 443, "/account/geo", hm);

            int count = 10;
            for (int i=0; i<count; i++) {
                System.out.println("Making request - " + i);
                SampleResult result = client.request();
                System.out.println("Received response: status=" + result.getResponseCode());
                System.out.println("Headers=\n" + result.getResponseHeaders());
                System.out.println("----------------\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
