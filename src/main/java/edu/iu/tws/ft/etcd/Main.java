package edu.iu.tws.ft.etcd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Util;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchEvent;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Args cmd = new Args();

        JCommander.newBuilder()
                .addObject(cmd)
                .build()
                .parse(args);

        CountDownLatch latch = new CountDownLatch(cmd.maxEvents);
        ByteSequence key = ByteSequence.from(cmd.key, StandardCharsets.UTF_8);
        Collection<URI> endpoints = Util.toURIs(cmd.endpoints);

        LOGGER.info("SSSS");

        Watch.Listener listener = Watch.listener(response -> {
            LOGGER.info("Watching for key=" + cmd.key);

            for (WatchEvent event : response.getEvents()) {
                LOGGER.info(String.format("type=%s, key=%s, value=%s",
                        event.getEventType(),
                        Optional.ofNullable(event.getKeyValue().getKey())
                                .map(bs -> bs.toString(StandardCharsets.UTF_8))
                                .orElse(""),
                        Optional.ofNullable(event.getKeyValue().getValue())
                                .map(bs -> bs.toString(StandardCharsets.UTF_8))
                                .orElse("")
                ));
            }

            latch.countDown();
        });

        try (Client client = Client.builder().endpoints(endpoints).build();
             Watch watch = client.getWatchClient();
             Watch.Watcher watcher = watch.watch(key, listener)) {

            latch.await();
        } catch (Exception e) {
            LOGGER.severe("Watching Error " + e);
            System.exit(1);
        }
    }

    public static class Args {
        @Parameter(required = true, names = {"-e", "--endpoints"}, description = "the etcd endpoints")
        private List<String> endpoints = new ArrayList<>();

        @Parameter(required = true, names = {"-k", "--key"}, description = "the key to watch")
        private String key;

        @Parameter(names = {"-m", "--max-events"}, description = "the maximum number of events to receive")
        private Integer maxEvents = Integer.MAX_VALUE;
    }
}
