package smartthings.cassandra.datadog.metrics3;

import java.net.MalformedURLException;
import java.net.URL;

public class HttpTransport implements MetricTransport {
    final URL seriesUrl;

    HttpTransport(String apiKey) {
        try {
            this.seriesUrl = new URL(String.format("https://app.datadoghq.com/api/v1/series?api_key=%s", apiKey));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to form URL", e);
        }
    }

    @Override
    public MetricRequest prepare() {
        return new HttpRequest(this);
    }

    @Override
    public void close() { /* nothing to do */ }

}
