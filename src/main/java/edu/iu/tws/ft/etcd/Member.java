package edu.iu.tws.ft.etcd;

import io.etcd.jetcd.Watch;

public class Member {
    private final String key;
    private final Watch.Listener listner;

    public Member(String key, Watch.Listener listener) {
        this.key = key;
        this.listner = listener;
    }



}
