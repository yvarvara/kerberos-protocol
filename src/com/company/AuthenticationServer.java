package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AuthenticationServer {
    private static ObjectInputStream in;
    private static ObjectOutputStream out;

    private static final String host = "127.0.0.1";
    private static final int authenticationServerPort = 8001;
    private static final int ticketGrantingServerPort = 8002;
    private static final int mainServerPort = 8000;

    private static Package authenticationServerMessage = new Package();

    private static Map<String, Integer> users = new HashMap<>() ;
    private static Map<String, Integer> servers = new HashMap<>();

    public static void main(String[] args) {
        users.put("varechka", -443869312);    // 866999aaa
        users.put("vladik1231", 1283847883);  // lolkek12345

        servers.put(host + ":" + ticketGrantingServerPort, -78682240);
        servers.put(host + ":" + mainServerPort, 2067029234);

        try {
            clientAuthentication();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void clientAuthentication() throws IOException, ClassNotFoundException {
        try (ServerSocket authenticationServerSocket = new ServerSocket(authenticationServerPort)) {
            try (Socket clientSocket = authenticationServerSocket.accept()) {
                in = new ObjectInputStream(clientSocket.getInputStream());
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                Package clientMessage = (Package) in.readObject();
                Logger.log("\nAS\t\tGets client ID from the client.");
                Logger.log(clientMessage.toString());

                Logger.log("\nAS\t\tChecks to see if the client is in its database.");
                if (users.containsKey(clientMessage.getUserId())) {
                    Logger.log("\nAS\t\tThe client is present in the database.");
                    out.writeBoolean(true);
                    out.flush();

                    int sessionKey = (new Random()).nextInt();
                    Logger.log("\nAS\t\tGenerates Client/TGS session key " + sessionKey);

                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    Timestamp expiration = new Timestamp(timestamp.getTime() + 24 * 3600 * 1000);
                    String ticketGrantingServerId = host + ":" + ticketGrantingServerPort; // todo leave id only

                    authenticationServerMessage.put(sessionKey, ticketGrantingServerId, timestamp, expiration);
                    Logger.log("\nAS\t\tSends Message A: Client/TGS session key encrypted using the secret key of the client.");
                    Logger.log(authenticationServerMessage.toString());

                    byte[] message = DES.encrypt(Package.toByteArray(authenticationServerMessage), users.get(clientMessage.getUserId()));
                    out.writeObject(message);
                    out.flush();

                    authenticationServerMessage.put(sessionKey, clientMessage.getUserId(), host, timestamp, expiration);
                    Logger.log("\nAS\t\tSends Message B: TGT, which includes the client ID, client network " +
                            "address, ticket validity period,\n\t\t and the Client/TGS session key encrypted using the secret key of the TGS.");
                    Logger.log(authenticationServerMessage.toString());

                    message = DES.encrypt(Package.toByteArray(authenticationServerMessage),
                            servers.get(ticketGrantingServerId));
                    out.writeObject(message);
                    out.flush();
                } else {
                    Logger.log("\nAS\t\tThe client is not present in the database.");
                    out.writeBoolean(false);
                    out.flush();
                }
            } finally {
                in.close();
                out.close();
            }
        }
    }
}