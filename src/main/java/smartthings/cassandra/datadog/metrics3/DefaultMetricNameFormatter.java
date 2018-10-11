package smartthings.cassandra.datadog.metrics3;

public class DefaultMetricNameFormatter implements MetricNameFormatter {
    public String format(String name, String... path) {
        final StringBuilder sb = new StringBuilder(name);
        for (String part : path) {
            sb.append('.').append(part);
        }
        return sb.toString();
    }
}
