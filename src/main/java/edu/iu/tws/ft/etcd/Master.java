package edu.iu.tws.ft.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Util;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import org.apache.commons.cli.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Master {
    private static final Logger LOG = Logger.getLogger(Master.class.getName());

    private final EtcdNode etcdNode;
    private final BlockingQueue<WatchEvent> eventQueue;
    private final Client client;
    private final String jobName;

    private AtomicBoolean running = new AtomicBoolean(true);

    public Master(String jobName, String url, List<String> endpoints)
            throws EtcdNode.EtcdNodeException {
        this.jobName = jobName;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.client = Client.builder().endpoints(Util.toURIs(endpoints)).build();

        ByteSequence masterKey = Utils.toByteSeq(jobName, Utils.MASTER_KEY);
        ByteSequence val = Utils.toByteSeq(url);

        ByteSequence workerKeyPrefix = Utils.toByteSeq(jobName, Utils.WORKER_KEY);
        WatchOption watchOption = WatchOption.newBuilder().withPrefix(workerKeyPrefix).build();

        this.etcdNode = new EtcdNode(masterKey, val, workerKeyPrefix, watchOption, eventQueue);
        etcdNode.init(client);
    }

    public void start() throws EtcdNode.EtcdNodeException {
        etcdNode.start(client);

        try {
            while (running.get()) {
                WatchEvent event = eventQueue.poll(Long.MAX_VALUE, TimeUnit.SECONDS);

                if (event == null) continue;

                LOG.info(() -> String.format("type=%s, key=%s, value=%s",
                        event.getEventType(),
                        Optional.ofNullable(event.getKeyValue().getKey())
                                .map(bs -> bs.toString(StandardCharsets.UTF_8))
                                .orElse(""),
                        Optional.ofNullable(event.getKeyValue().getValue())
                                .map(bs -> bs.toString(StandardCharsets.UTF_8))
                                .orElse(""))
                );
            }
        } catch (InterruptedException e) {
            throw new EtcdNode.EtcdNodeException(e);
        }

    }

    public void shutdown() {
        running.set(false);
    }

    public static void main(String[] args) throws EtcdNode.EtcdNodeException {
        Options options = new Options();

        Option job = new Option("j", "job", true, "job name");
        options.addOption(job);
        Option input = new Option("m", "master", true, "master url");
        options.addOption(input);
        Option etcd = new Option("e", "etcd", true, "etcd endpoints");
        options.addOption(etcd);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOG.severe(e.getMessage());
            formatter.printHelp("Master", options);

            System.exit(1);
        }

        String masterUrl = cmd.getOptionValue("master");
        String jobName = cmd.getOptionValue("job", "job0");
        List<String> etcdEPs = Arrays.asList(cmd.getOptionValue("etcd",
                "http://localhost:2379").split(","));

        LOG.info("Starting master");
        Master master = new Master(jobName, masterUrl, etcdEPs);
        master.start();
    }
}
