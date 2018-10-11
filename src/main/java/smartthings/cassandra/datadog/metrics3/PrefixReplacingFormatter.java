package smartthings.cassandra.datadog.metrics3;

public class PrefixReplacingFormatter extends DefaultMetricNameFormatter {

    private final String before;
    private final int beforeLen;
    private final String after;

    public PrefixReplacingFormatter(String before, String after) {
        this.before = before;
        this.beforeLen = before.length();
        this.after = after;
    }

    @Override
    public String format(String name, String... path) {
        String m = super.format(name, path);
        if (m.startsWith(before)) {
            return after + m.substring(beforeLen);
        }
        return m;
    }
}