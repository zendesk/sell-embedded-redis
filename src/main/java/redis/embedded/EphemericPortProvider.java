package redis.embedded;

/**
 * Created by piotrturek on 29/01/15.
 */
public class EphemericPortProvider implements PortProvider {
    @Override
    public int next() {
        return 0;
    }
}
