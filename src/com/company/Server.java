package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static ObjectInputStream in;
    private static ObjectOutputStream out;

    private static final String host = "127.0.0.1";
    private static final int mainServerPort = 8000;
    private static final int mainServerKey = 2067029234;

    private static Package mainServerMessage = new Package();

    public static void main(String[] args) {
        try {
            clientServiceRequest();
        } catch (StreamCorruptedException e) {
            Logger.log("\nSERVER\tWrong secret key. Unable to decrypt message.");
            System.exit(1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void clientServiceRequest() throws IOException, ClassNotFoundException {
        try (ServerSocket mainServerSocket = new ServerSocket(mainServerPort)) {
            try (Socket clientSocket = mainServerSocket.accept()) {
                in = new ObjectInputStream(clientSocket.getInputStream());
                out = new ObjectOutputStream(clientSocket.getOutputStream());

                Logger.log("\nSERVER\tServer secret key: " + mainServerKey);

                Package clientMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), mainServerKey));
                Logger.log("\nSERVER\tGets client-to-server ticket encrypted using server's secret key from client.");
                Logger.log("\nSERVER\tMessage decrypted:\n" + clientMessage);

                int sessionKey2 = clientMessage.getKey();

                clientMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), sessionKey2));
                Logger.log("\nSERVER\tGets the client ID and timestamp encrypted using Client/Server Session Key from client.");
                Logger.log("\nSERVER\tMessage decrypted:\n" + clientMessage);

                mainServerMessage.put(host + ":" + mainServerPort, clientMessage.getTimestamp());
                Logger.log("\nSERVER\tSends the server ID and the timestamp encrypted using the Client/Server session Key to the client");
                Logger.log(mainServerMessage.toString());

                byte[] message = DES.encrypt(Package.toByteArray(clientMessage), sessionKey2);
                out.writeObject(message);
                out.flush();
            } finally {
                in.close();
                out.close();
            }
        }
    }
}