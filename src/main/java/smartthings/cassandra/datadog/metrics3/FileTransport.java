package smartthings.cassandra.datadog.metrics3;

import java.io.FileWriter;

class FileTransport implements MetricTransport {
    private final String fileName;
    FileWriter fw;

    FileTransport(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public MetricRequest prepare() {
        try {
            fw = new FileWriter(fileName, true);
            fw.append("+++++++++++++++++++++ ${new Date().toString()} +++++++++++++++++++++\n");
            return new FileRequest(this);
        } catch (Exception e) {
            throw new RuntimeException("error preparing File", e);
        }
    }

    @Override
    public void close() {
        if (fw != null) {
            try {
                fw.close();
            } catch (Exception e) {
                throw new RuntimeException("error closing File", e);
            }
        }
    }

}