package smartthings.cassandra.datadog.metrics3;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class HttpRequest implements MetricRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
    private static final int GATEWAY_TIMEOUT_MILLIS = 5000;
    private final HttpTransport transport;
    private static final JsonFactory jsonFactory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper(jsonFactory);
    private JsonGenerator jsonOut;
    private ByteArrayOutputStream outputStream;

    HttpRequest(HttpTransport transport) {
        try {
            this.transport = transport;
            outputStream = new ByteArrayOutputStream(2048);
            jsonOut = jsonFactory.createJsonGenerator(outputStream);
            jsonOut.writeStartObject();
            jsonOut.writeFieldName("series");
            jsonOut.writeStartArray();
        } catch (Exception e) {
            throw new RuntimeException("error in initiating new metric HttpRequest", e);
        }
    }

    // get rid of these
    @Override
    public void addGauge(DatadogGauge gauge) {
        try {
            mapper.writeValue(jsonOut, gauge);
        } catch (Exception e) {
            throw new RuntimeException("error in adding gauge to HttpRequest", e);
        }
    }

    @Override
    public void addCounter(DatadogCounter counter) {
        try {
            mapper.writeValue(jsonOut, counter);
        } catch (Exception e) {
            throw new RuntimeException("error in adding counter to HttpRequest", e);
        }
    }

    @Override
    public void send() {
        try {
            jsonOut.writeEndArray();
            jsonOut.writeEndObject();
            jsonOut.flush();
            outputStream.close();
            postMetric(outputStream.toString("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("error in sending metric via HttpRequest", e);
        }
    }

    private void postMetric(final String messageJson) {
        HttpURLConnection urlConnection = null;
        try {
            log.debug("sending data to the datadog gateway");
            urlConnection = (HttpURLConnection) transport.seriesUrl.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setConnectTimeout(GATEWAY_TIMEOUT_MILLIS);
            urlConnection.setReadTimeout(GATEWAY_TIMEOUT_MILLIS);
            urlConnection.setRequestProperty("content-type", "application/json charset=utf-8");

            OutputStream os = urlConnection.getOutputStream();
            os.write(messageJson.getBytes());
            os.flush();
            os.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode >= 300) {
                log.warn("Datadog returned a non-200 response: {}", responseCode);
            }
        } catch (Exception e) {
            log.error("Error connecting to datadog", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}