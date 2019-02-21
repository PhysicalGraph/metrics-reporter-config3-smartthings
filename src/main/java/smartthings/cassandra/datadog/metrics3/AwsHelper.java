package smartthings.cassandra.datadog.metrics3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class AwsHelper {
    private static final Logger log = LoggerFactory.getLogger(AwsHelper.class);
    private static final int TIMEOUT_MILLIS = 5000;
    private static final int RETRY_COUNT = 3;

    static String getEc2InstanceId() {
        int errCnt = 0;
        while (true) {
            try {
                String instanceId = callAWS();
                log.debug("Discovered instanceId of {}", instanceId);
                return instanceId;
            } catch (Exception e) {
                errCnt++;
                if (errCnt >= RETRY_COUNT) {
                    throw e;
                }
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private static String callAWS() {
        HttpURLConnection urlConnection = null;
        URL instanceUrl;
        try {
            log.debug("Requesting instance id from aws meta data endpoints");
            instanceUrl = new URL("http://169.254.169.254/latest/meta-data/instance-id");

            urlConnection = (HttpURLConnection) instanceUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(TIMEOUT_MILLIS);
            urlConnection.setReadTimeout(TIMEOUT_MILLIS);

            InputStream response = urlConnection.getInputStream();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode >= 300) {
                throw new IOException("Invalid status code $responseCode found when trying to get aws instance-id");
            }
            String instanceId = inputStreamToString(response);
            log.debug("Found aws instance id of {}", instanceId);
            return instanceId;
        } catch (Exception e) {
            throw new RuntimeException("Error in aws instanceid resolution", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static String inputStreamToString(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString().trim();
    }

}