package jmeter.plugins.http2.sampler;

import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;

public class OkHttp2ClientTest
{
    public static void main(String[] args)
    {
        HeaderManager hm = new HeaderManager();
        OkHttp2Client client = new OkHttp2Client();
        try {
            int count = 25;
            for (int i=0; i<count; i++) {
                System.out.println("Making request - " + i);
                SampleResult result = client.request("get", "https", "100.66.48.151", 7006, "/account/geo", hm);
                System.out.println("Received response: status=" + result.getResponseCode());
                System.out.println("Headers=\n" + result.getResponseHeaders());
                System.out.println("----------------\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            client.shutdown();
        }
    }
}
