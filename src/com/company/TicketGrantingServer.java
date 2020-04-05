package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TicketGrantingServer {
    private static ObjectInputStream in;
    private static ObjectOutputStream out;

    private static final int ticketGrantingServerPort = 8002;
    private static final int TGSkey = -78682240;

    private static Package ticketGrantingServerMessage = new Package();
    private static Map<String, Integer> servers = new HashMap<>();

    public static void main(String[] args)
    {
        servers.put("127.0.0.1:8000", 2067029234);
        try {
            clientServiceAuthorization();
        } catch (StreamCorruptedException e) {
            Logger.log("\nTGS\t\tWrong secret key. Unable to decrypt message.");
            System.exit(1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void clientServiceAuthorization() throws IOException, ClassNotFoundException {
        try (ServerSocket ticketGrantingServerSocket = new ServerSocket(ticketGrantingServerPort)) {
            try (Socket clientSocket = ticketGrantingServerSocket.accept()) {
                in = new ObjectInputStream(clientSocket.getInputStream());
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                Logger.log("\nTGS\t\tTGS secret key: " + TGSkey);

                Package TGT = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), TGSkey));
                Logger.log("\nTGS\t\tGets TGT from the client.");
                Logger.log("\nTGS\t\tTGT decrypted:\n" + TGT);

                String requestedServerId = in.readUTF();
                int sessionKey = TGT.getKey();

                Package clientMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), sessionKey));
                Logger.log("\nTGS\t\tGets client ID and the timestamp from the client.");
                Logger.log("\nTGS\t\tMessage decrypted:\n" + clientMessage);

                if (TGT.getExpiration().getTime() < System.currentTimeMillis()) {
                    Logger.log("\nTGS\t\tExpiration time exceeded.");
                    System.exit(1);
                }

                int sessionKey2 = (new Random()).nextInt();
                Logger.log("\nTGS\t\tGenerates Client/Server session key " + sessionKey);

                ticketGrantingServerMessage.put(sessionKey2, TGT.getUserId(), TGT.getServerId(),
                        TGT.getTimestamp(), TGT.getExpiration());
                Logger.log("\nTGS\t\tSends Message A: the client ID, client network address, validity period " +
                        "and Client/Server session Key \n\t\tencrypted using the main service's secret key to the client");
                Logger.log(ticketGrantingServerMessage.toString());

                byte[] message = DES.encrypt(Package.toByteArray(ticketGrantingServerMessage), servers.get(requestedServerId));
                out.writeObject(message);
                out.flush();

                ticketGrantingServerMessage.put(sessionKey2, requestedServerId, TGT.getExpiration());
                Logger.log("\nTGS\t\tSends Message B: Client/Server session Key encrypted with the " +
                        "Client/TGS session Key to the client");
                Logger.log(ticketGrantingServerMessage.toString());

                message = DES.encrypt(Package.toByteArray(ticketGrantingServerMessage), sessionKey);
                out.writeObject(message);
                out.flush();
            } finally {
                in.close();
                out.close();
            }
        }
    }
}