package redis.embedded;

import redis.embedded.ports.EphemeralPortProvider;
import redis.embedded.ports.PredefinedPortProvider;
import redis.embedded.ports.SequencePortProvider;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by piotrturek on 22/01/15.
 */
public class RedisClusterBuilder {
    private RedisSentinelBuilder sentinelBuilder = new RedisSentinelBuilder();
    private RedisServerBuilder serverBuilder = new RedisServerBuilder();
    private int sentinelCount = 1;
    private int quorumSize = 1;
    private PortProvider sentinelPortProvider = new SequencePortProvider(26379);
    private PortProvider replicationGroupPortProvider = new SequencePortProvider(6379);
    private final List<ReplicationGroup> groups = new LinkedList<>();

    public RedisClusterBuilder withSentinelBuilder(RedisSentinelBuilder sentinelBuilder) {
        this.sentinelBuilder = sentinelBuilder;
        return this;
    }

    public RedisClusterBuilder withServerBuilder(RedisServerBuilder serverBuilder) {
        this.serverBuilder = serverBuilder;
        return this;
    }

    public RedisClusterBuilder sentinelPorts(Collection<Integer> ports) {
        this.sentinelPortProvider = new PredefinedPortProvider(ports);
        this.sentinelCount = ports.size();
        return this;
    }

    public RedisClusterBuilder serverPorts(Collection<Integer> ports) {
        this.replicationGroupPortProvider = new PredefinedPortProvider(ports);
        return this;
    }

    public RedisClusterBuilder ephemeralSentinels() {
        this.sentinelPortProvider = new EphemeralPortProvider();
        return this;
    }

    public RedisClusterBuilder ephemeralServers() {
        this.replicationGroupPortProvider = new EphemeralPortProvider();
        return this;
    }


    public RedisClusterBuilder ephemeral() {
        ephemeralSentinels();
        ephemeralServers();
        return this;
    }

    public RedisClusterBuilder sentinelCount(int sentinelCount) {
        this.sentinelCount = sentinelCount;
        return this;
    }

    public RedisClusterBuilder sentinelStartingPort(int startingPort) {
        this.sentinelPortProvider = new SequencePortProvider(startingPort);
        return this;
    }

    public RedisClusterBuilder quorumSize(int quorumSize) {
        this.quorumSize = quorumSize;
        return this;
    }

    public RedisClusterBuilder replicationGroup(String masterName, int slaveCount) {
        this.groups.add(new ReplicationGroup(masterName, slaveCount, this.replicationGroupPortProvider));
        return this;
    }

    public RedisCluster build() {
        final List<Redis> sentinels = buildSentinels();
        final List<Redis> servers = buildServers();
        return new RedisCluster(sentinels, servers);
    }

    private List<Redis> buildServers() {
        return groups.stream().flatMap(g -> {
            final Stream.Builder<Redis> builder = Stream.builder();
            builder.accept(buildMaster(g));
            buildSlaves(builder, g);
            return builder.build();
        }).collect(Collectors.toList());
    }

    private void buildSlaves(Stream.Builder<Redis> builder, ReplicationGroup g) {
        for (Integer slavePort : g.slavePorts) {
            serverBuilder.reset();
            serverBuilder.port(slavePort);
            serverBuilder.slaveOf("localhost", g.masterPort);
            final RedisServer slave = serverBuilder.build();
            builder.accept(slave);
        }
    }

    private Redis buildMaster(ReplicationGroup g) {
        serverBuilder.reset();
        return serverBuilder.port(g.masterPort).build();
    }

    private List<Redis> buildSentinels() {
        int toBuild = this.sentinelCount;
        final List<Redis> sentinels = new LinkedList<>();
        while (toBuild-- > 0) {
            sentinels.add(buildSentinel());
        }
        return sentinels;
    }

    private Redis buildSentinel() {
        sentinelBuilder.reset();
        sentinelBuilder.port(nextSentinelPort());
        groups.stream().forEach(g -> {
            sentinelBuilder.masterName(g.masterName);
            sentinelBuilder.masterPort(g.masterPort);
            sentinelBuilder.quorumSize(quorumSize);
            sentinelBuilder.addDefaultReplicationGroup();
        });
        return sentinelBuilder.build();
    }

    private int nextSentinelPort() {
        return sentinelPortProvider.next();
    }

    private static class ReplicationGroup {
        private final String masterName;
        private final int masterPort;
        private final List<Integer> slavePorts = new LinkedList<>();

        private ReplicationGroup(String masterName, int slaveCount, PortProvider portProvider) {
            this.masterName = masterName;
            masterPort = portProvider.next();
            while (slaveCount-- > 0) {
                slavePorts.add(portProvider.next());
            }
        }
    }
}
