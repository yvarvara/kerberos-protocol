package com.company;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;

class Client {
    private static ObjectInputStream in;
    private static ObjectOutputStream out;

    private static final String host = "127.0.0.1";
    private static final int mainServerPort = 8000;
    private static final int authenticationServerPort = 8001;
    private static final int ticketGrantingServerPort = 8002;

    private static int clientKey;
    private static String clientId;

    private static Package clientMessage = new Package();

    private static int sessionKey;
    private static int sessionKey2;
    private static Timestamp timestamp;
    private static byte[] TGT;
    private static byte[] CST;

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("login: ");
            clientId = reader.readLine();
            System.out.print("password: ");
            clientKey = reader.readLine().hashCode();
            Logger.log("\nCLIENT\tClient secret key: " + clientKey);

            clientAuthentication();
            clientServiceAuthorization();
            clientServiceRequest();
        } catch (StreamCorruptedException e) {
                Logger.log("\nCLIENT\tWrong secret key. Unable to decrypt message.");
                System.exit(1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void clientAuthentication() throws IOException, ClassNotFoundException {
        try (Socket clientSocket = new Socket(host, authenticationServerPort)) {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            clientMessage.put(clientId);
            Logger.log("\nCLIENT\tSends the client ID to the AS.");
            Logger.log(clientMessage.toString());

            out.writeObject(clientMessage);
            out.flush();

            boolean answer = in.readBoolean();
            if (!answer) {
                Logger.log("\nCLIENT\tAccount does not exist.");
                System.exit(1);
            } else {
                Package authenticationServerMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), clientKey));
                Logger.log("\nCLIENT\tGets Client/TGS session key encrypted using the secret key of the client from AS.");
                Logger.log("\nCLIENT\tMessage decrypted:\n" + authenticationServerMessage);

                sessionKey = authenticationServerMessage.getKey();
                timestamp = authenticationServerMessage.getTimestamp();

                TGT = (byte[]) in.readObject();
                Logger.log("\nCLIENT\tGets TGT from AS.");
            }
        } finally {
            in.close();
            out.close();
        }
    }

    private static void clientServiceAuthorization() throws IOException, ClassNotFoundException {
        try (Socket clientSocket = new Socket(host, ticketGrantingServerPort)) {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            Logger.log("\nCLIENT\tSends the TGT and the ID of the requested service to the TGS.");
            out.writeObject(TGT);
            out.flush();

            out.writeUTF(host + ":" + mainServerPort);
            out.flush();

            clientMessage.put(clientId, timestamp);
            Logger.log("\nCLIENT\tSends client ID and the timestamp, " +
                    "encrypted using the Client/TGS session key to the TGS.");
            Logger.log(clientMessage.toString());

            byte[] message = DES.encrypt(Package.toByteArray(clientMessage), sessionKey);
            out.writeObject(message);
            out.flush();

            CST = (byte[]) in.readObject();
            Logger.log("\nCLIENT\tGets Client-to-server ticket from TGS.");

            Package tgsMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), sessionKey));
            Logger.log("\nCLIENT\tGets Client/Server session Key encrypted with the Client/TGS session key from TGS.");
            Logger.log("\nCLIENT\tMessage decrypted:\n" + tgsMessage);

            sessionKey2 = tgsMessage.getKey();
        } finally {
            in.close();
            out.close();
        }
    }

    private static void clientServiceRequest() throws IOException, ClassNotFoundException {
        try (Socket clientSocket = new Socket(host, mainServerPort)) {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            Logger.log("\nCLIENT\tSends the client-to-server ticket to the Server");
            out.writeObject(CST);
            out.flush();

            clientMessage.put(clientId, timestamp);
            Logger.log("\nCLIENT\tSends the client ID and timestamp encrypted using Client/Server session Key to the Server");
            Logger.log(clientMessage.toString());

            byte[] message = DES.encrypt(Package.toByteArray(clientMessage), sessionKey2);
            out.writeObject(message);
            out.flush();

            Package serverMessage = Package.fromByteArray(DES.decrypt((byte[]) in.readObject(), sessionKey2));
            Logger.log("\nCLIENT\tGets the server ID and the timestamp from server.");
            Logger.log("\nCLIENT\tMessage decrypted:\n" + serverMessage);
        } finally {
            in.close();
            out.close();
        }
    }
}