package jmeter.plugins.http2.sampler;

import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.samplers.SampleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class OkHttp2ClientTest
{
    public static void main(String[] args)
    {
        OkHttp2Client client = new OkHttp2Client();
        ExecutorService exec = Executors.newFixedThreadPool(5);
        List<Callable<String>> tasks = new ArrayList<>();
        try {
            int count = 200;
            for (int i = 0; i < count; i++) {
                tasks.add(new RequestTask(client, i));
            }

            List<Future<String>> futures = exec.invokeAll(tasks, 60, TimeUnit.SECONDS);
            for (Future f : futures) {
                if (! f.isDone()) {
                    System.out.println("Task not complete!");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            client.shutdown();
        }
    }

    private static class RequestTask implements Callable<String>
    {
        private OkHttp2Client client;
        private int index;

        public RequestTask(OkHttp2Client client, int index)
        {
            this.client = client;
            this.index = index;
        }

        @Override
        public String call() throws Exception
        {
            String path = "/req-" + (index + 1);
            try {
                System.out.println("Making request - " + index);
                HeaderManager headers = new HeaderManager();
                headers.add(new Header("Simulator-Options", "status=200, duration=100"));
                SampleResult result = client.request("get", "https", "localhost", 7006, path, headers);
                System.out.println("Received response: status=" + result.getResponseCode());
                System.out.println("Headers=\n" + result.getResponseHeaders());
                System.out.println("----------------\n");

                if (! result.isSuccessful()) {
                    System.err.println("Unsuccessful request - " + path);
                }

                return result.getResponseCode();
            }
            catch (Exception e) {
                System.err.println("Error for request - " + path);
                e.printStackTrace();
                return e.getMessage();
            }
        }
    }
}
