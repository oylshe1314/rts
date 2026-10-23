package com.sk.rts.application.config;

import com.hazelcast.config.*;
import io.netty.channel.EventLoopGroup;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.impl.transports.NioTransport;
import io.vertx.core.impl.transports.TransportInternal;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
public class VertxConfiguration {

    @Bean
    public Vertx vertx(VertxProperties properties, ReactorResourceFactory reactorResourceFactory) {

        EventLoopGroup eventLoopGroup = reactorResourceFactory.getLoopResources().onServer(true);

        VertxOptions options = new VertxOptions();
        options.setEventLoopPoolSize(8);
        options.setWorkerPoolSize(8);
        options.setInternalBlockingPoolSize(8);
        options.setHAEnabled(false);
        options.setMetricsOptions(null);
        options.setFileSystemOptions(new FileSystemOptions().setFileCachingEnabled(false));
        options.setWarningExceptionTimeUnit(TimeUnit.MILLISECONDS).setWarningExceptionTime(3000L);
        options.setBlockedThreadCheckIntervalUnit(TimeUnit.MILLISECONDS).setBlockedThreadCheckInterval(3000L);

        VertxBuilder builder = Vertx.builder();
        builder.with(options);
        builder.withTransport(new TransportInternal("webflux", true, null, new ApplyWebfluxEventLoopGroupTransport(eventLoopGroup)));

        if (!Boolean.TRUE.equals(properties.getClusterEnabled())) {
            return builder.build();
        } else {
            Config hazelcastConfig = ConfigUtil.loadConfig();
            if (!CollectionUtils.isEmpty(properties.getClusterMembers())) {
                NetworkConfig network = hazelcastConfig.getNetworkConfig();
                if (properties.getMemberPort() == null || properties.getMemberPort() <= 0) {
                    network.setPortAutoIncrement(true);
                } else {
                    network.setPort(properties.getMemberPort());
                }

                JoinConfig join = network.getJoin();

                join.getMulticastConfig().setEnabled(false);
                join.getAwsConfig().setEnabled(false);

                TcpIpConfig tcpIp = join.getTcpIpConfig();
                tcpIp.setEnabled(true);

                for (String clusterMember : properties.getClusterMembers()) {
                    tcpIp.addMember(clusterMember);
                }

                InterfacesConfig interfaces = network.getInterfaces();
                interfaces.setEnabled(false);
//                interfaces.addInterface("0.0.0.0");
            }

            return Future.await(builder.withClusterManager(new HazelcastClusterManager(hazelcastConfig)).buildClustered());
        }
    }

    private static class ApplyWebfluxEventLoopGroupTransport extends NioTransport {

        private final EventLoopGroup eventLoopGroup;

        private ApplyWebfluxEventLoopGroupTransport(EventLoopGroup eventLoopGroup) {
            this.eventLoopGroup = eventLoopGroup;
        }

        @Override
        public EventLoopGroup eventLoopGroup(int type, int nThreads, ThreadFactory threadFactory, int ioRatio) {
            return eventLoopGroup;
        }
    }
}
