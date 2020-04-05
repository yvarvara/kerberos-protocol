package com.company;

import java.io.*;
import java.sql.Timestamp;

public class Package implements Serializable {
    private Integer key;
    private String userId;
    private String serverId;
    private Timestamp timestamp;
    private Timestamp expiration;

    public int getKey() {
        return key;
    }

    public String getUserId() {
        return userId;
    }

    public String getServerId() {
        return serverId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public Timestamp getExpiration() {
        return expiration;
    }

    public void put(String userId, Timestamp timestamp) {
        clear();
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public void put(String userId) {
        clear();
        this.userId = userId;
    }

    public void put(int key, String serverId, Timestamp expiration) {
        clear();
        this.key = key;
        this.serverId = serverId;
        this.expiration = expiration;
    }

    public void put(int key, String serverId, Timestamp timestamp, Timestamp expiration) {
        clear();
        this.key = key;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.expiration = expiration;
    }

    public void put(int key, String userId, String serverId, Timestamp timestamp, Timestamp expiration) {
        clear();
        this.key = key;
        this.userId = userId;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.expiration = expiration;
    }

    private void clear() {
        key = null;
        userId = null;
        serverId = null;
        timestamp = null;
        expiration = null;
    }

    @Override
    public String toString() {
        String string = "";
        if (key != null)
            string += "\t\tkey: " + key + '\n';
        if (userId != null)
            string += "\t\tuserId: " + userId + '\n';
        if (serverId != null)
            string += "\t\tserverId: " + serverId + '\n';
        if (timestamp != null)
            string += "\t\ttimestamp: " + timestamp + '\n';
        if (expiration != null)
            string += "\t\texpiration: " + expiration;

        return string;
    }

    public static byte[] toByteArray(Package pack) {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                out.writeObject(pack);
                out.flush();
                bytes = bos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public static Package fromByteArray(byte[] bytes) throws ClassNotFoundException, IOException {
        Package pack;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            try (ObjectInput in = new ObjectInputStream(bis)) {
                pack = (Package) in.readObject();
            }
        }

        return pack;
    }
}