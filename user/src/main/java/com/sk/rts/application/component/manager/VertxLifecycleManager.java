package com.sk.rts.application.component.manager;

import com.sk.rts.application.config.VertxProperties;
import io.vertx.core.Deployable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.sqlclient.Pool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VertxLifecycleManager implements SmartLifecycle {

    private final Pool pool;
    private final Redis redis;
    private final Vertx vertx;

    private final VertxProperties vertxProperties;

    private final ApplicationContext applicationContext;

    private boolean running = false;

    private Future<Long> deployVerticles() {
        Map<Class<? extends Deployable>, Integer> deploymentInstances = vertxProperties.getDeploymentInstances();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        List<Future<Void>> futures = new ArrayList<>();
        deploymentInstances.forEach((deployableClass, instances) -> {
            for (int i = 0; i < instances; i++) {
                Deployable deployable = beanFactory.createBean(deployableClass);

                Promise<Void> deployPromise = Promise.promise();
                vertx.deployVerticle(deployable).onComplete(ar -> {
                    if (ar.succeeded()) {
                        log.info("组件{}部署成功，ID={}", deployable.getClass().getName(), ar.result());
                        deployPromise.complete();
                    } else {
                        log.error("组件{}部署失败", deployable.getClass().getName(), ar.cause());
                        deployPromise.fail(ar.cause());
                    }
                });
                futures.add(deployPromise.future());
            }
        });

        return Future.all(futures).map((long) futures.size());
    }

    @Override
    public void start() {
        log.info("Vert.x生命周期管理器启动，正在部署Verticle组件~~~");
        try {
            long verticles = Future.await(deployVerticles());
            running = true;
            log.info("Vert.x生命周期管理器启动完成，verticles={}", verticles);
        } catch (Exception error) {
            log.error("Vert.x生命周期管理器启动失败", error);
            throw error;
        }
    }

    @Override
    public void stop() {
        log.info("Vert.x生命周期管理器正在关闭~~~");
        Future.await(Future.all(Future.all(pool.close(), redis.close()).eventually(vertx::close)));
        running = false;
        log.info("Vert.x生命周期管理器关闭完成");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 10;
    }
}
