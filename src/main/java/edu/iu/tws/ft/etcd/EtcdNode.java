package edu.iu.tws.ft.etcd;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class EtcdNode {
    private static final Logger LOG = Logger.getLogger(EtcdNode.class.getName());

    private final ByteSequence key;
    private final ByteSequence value;
    private final ByteSequence watchKey;
    private final BlockingQueue<WatchEvent> eventQueue;

    private WatchOption watchOption;

    public EtcdNode(ByteSequence key, ByteSequence value, ByteSequence watchKey,
                    BlockingQueue<WatchEvent> msgQueue) {
        this(key, value, watchKey, null, msgQueue);
    }

    public EtcdNode(ByteSequence key, ByteSequence value, ByteSequence watchKey,
                    WatchOption watchOption, BlockingQueue<WatchEvent> msgQueue) {
        this.key = key;
        this.value = value;
        this.watchKey = watchKey;
        this.eventQueue = msgQueue;
        this.watchOption = watchOption;
    }


    public void init(Client client) throws EtcdNodeException {
        // initialize my key
        try {
            client.getKVClient().put(key, value).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new EtcdNodeException("Unable to set my key "
                    + key.toString(StandardCharsets.UTF_8), e);
        }
    }

    public void start(Client client) throws EtcdNodeException {
        Watch.Listener listener = Watch.listener(response -> {
            eventQueue.addAll(response.getEvents());
        });

        Watch watch = client.getWatchClient();

        LOG.info("Watching for key=" + watchKey.toString(StandardCharsets.UTF_8));
        if (watchOption == null) {
            watch.watch(watchKey, listener);
        } else {
            watch.watch(watchKey, watchOption, listener);
        }
    }

    public static class EtcdNodeException extends Exception {
        public EtcdNodeException(Throwable cause) {
            super(cause);
        }

        public EtcdNodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
