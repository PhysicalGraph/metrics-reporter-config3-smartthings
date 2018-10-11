package smartthings.cassandra.datadog.metrics3;

interface MetricNameFormatter {
    String format(String name, String... path);
}
