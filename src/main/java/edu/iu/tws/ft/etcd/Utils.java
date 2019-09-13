package edu.iu.tws.ft.etcd;

import io.etcd.jetcd.ByteSequence;

import java.nio.charset.StandardCharsets;

public class Utils {
    public static final String MASTER_KEY = "master";
    public static final String WORKER_KEY= "worker";

    public static String concat(String ... paths){
        StringBuilder builder = new StringBuilder();

        for(String path:paths){
            builder.append(path);
            builder.append("_");
        }

        return builder.toString();
    }

    public static ByteSequence toByteSeq (String ... strings){
        return ByteSequence.from(concat(strings), StandardCharsets.UTF_8);
    }

}
