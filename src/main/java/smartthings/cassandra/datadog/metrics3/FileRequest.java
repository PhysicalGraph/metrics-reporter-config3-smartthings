package smartthings.cassandra.datadog.metrics3;


class FileRequest implements MetricRequest {
    private final FileTransport transport;

    FileRequest(FileTransport transport) {
        this.transport = transport;
    }

    @Override
    public void addGauge(DatadogGauge gauge) {
        try {
            Number epoch = gauge.getPoints().get(0).get(0);
            Number count = gauge.getPoints().get(0).get(1);
            transport.fw.append(String.format("%-10s %-10s %-12s %-20s %s\n", gauge.getHost(), gauge.getType(), epoch, count, gauge.getMetric()));
        } catch (Exception e) {
            throw new RuntimeException("Error in adding gauge to File", e);
        }
    }

    @Override
    public void addCounter(DatadogCounter counter) {
        try {
            Number epoch = counter.getPoints().get(0).get(0);
            Number count = counter.getPoints().get(0).get(1);
            transport.fw.append(String.format("%-10s %-10s %-12s %-20s %s\n", counter.getHost(), counter.getType(), epoch, count, counter.getMetric()));
        } catch (Exception e) {
            throw new RuntimeException("Error in adding gauge to File", e);
        }

    }

    @Override
    public void send() {
        try {
            transport.fw.flush();
            transport.fw.close();
            transport.fw = null;
        } catch (Exception e) {
            throw new RuntimeException("Error in sending metric to File", e);
        }
    }
}