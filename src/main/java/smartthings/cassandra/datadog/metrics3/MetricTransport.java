package smartthings.cassandra.datadog.metrics3;

import java.io.Closeable;

interface MetricTransport extends Closeable {

    MetricRequest prepare();

}

// criticism: why have gauge/counter/metric classes if we don't have liskov substitutability
