package redis.embedded;

/**
 * Created by piotrturek on 29/01/15.
 */
public class SequencePortProvider implements PortProvider {
    private volatile int currentPort = 26379;

    public SequencePortProvider(int currentPort) {
        this.currentPort = currentPort;
    }

    public void setCurrentPort(int port) {
        currentPort = port;
    }

    @Override
    public int next() {
        return currentPort++;
    }
}
