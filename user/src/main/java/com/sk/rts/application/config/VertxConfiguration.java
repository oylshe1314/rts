package com.sk.rts.application.config;

import com.hazelcast.config.*;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.spi.cluster.hazelcast.ConfigUtil;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.TimeUnit;

@SpringBootConfiguration
public class VertxConfiguration {

    @Bean
    public Vertx vertx(VertxProperties properties) {

        VertxOptions options = new VertxOptions();
        options.setEventLoopPoolSize(16);
        options.setWorkerPoolSize(16);
        options.setInternalBlockingPoolSize(16);
        options.setHAEnabled(false);
        options.setMetricsOptions(null);
        options.setFileSystemOptions(new FileSystemOptions().setFileCachingEnabled(false));
        options.setWarningExceptionTimeUnit(TimeUnit.MILLISECONDS).setWarningExceptionTime(3000L);
        options.setBlockedThreadCheckIntervalUnit(TimeUnit.MILLISECONDS).setBlockedThreadCheckInterval(3000L);

        VertxBuilder builder = Vertx.builder();
        builder.with(options);

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
//            interfaces.addInterface("0.0.0.0");
            }

            return Future.await(builder.withClusterManager(new HazelcastClusterManager(hazelcastConfig)).buildClustered());
        }
    }
}
