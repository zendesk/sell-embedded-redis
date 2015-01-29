package redis.embedded;

import redis.embedded.exceptions.RedisBuildingException;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by piotrturek on 29/01/15.
 */
public class PredefinedPortProvider implements PortProvider {
    private final List<Integer> ports = new LinkedList<>();
    private final Iterator<Integer> current = ports.iterator();

    public PredefinedPortProvider(Collection<Integer> ports) {
        this.ports.addAll(ports);
    }

    @Override
    public int next() {
        if (!current.hasNext()) {
            throw new RedisBuildingException("Run out of Redis ports!");
        }
        return current.next();
    }
}
