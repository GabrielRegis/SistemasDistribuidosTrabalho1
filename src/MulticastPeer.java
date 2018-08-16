
import org.apache.commons.lang3.SerializationUtils;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MulticastPeer {

    public static User user;

    public static void main(String args[]) throws IOException {

        final MulticastSocket socket = new MulticastSocket(6789);
        user = new User();

        try {
            InetAddress group = InetAddress.getByName("ff02::1");
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));
            socket.joinGroup(group);

            UserEventPackage newUserPackage = new UserEventPackage(UserEventType.NEW_USER_REQUEST, user);
            sendUserEventPackage(socket, group, newUserPackage);

//            UserEventPackage userConnectedPackage = new UserEventPackage(UserEventType.USER_CONNECTED, user);
//            sendUserEventPackage(socket, group, userConnectedPackage);

//            byte[] m = "teste".getBytes();
//            DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
//            socket.send(messageOut);
//
//            new Thread() {
//
//                @Override
//                public void run() {
//
//                    Scanner scanner = new Scanner(System.in);
//                    while (true) {
//                        String message = scanner.nextLine();
//                        byte[] m = message.getBytes();
//                        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
//                        try {
//                            socket.send(messageOut);
//                        } catch (IOException ex) {
//                            Logger.getLogger(MulticastPeer.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//
//                }
//            }.start();

            while (true) {
                byte[] buffer = new byte[10000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);

                UserEventPackage eventRecieved = SerializationUtils.deserialize(messageIn.getData());

                switch (eventRecieved.getType()){
                    case NEW_USER_REQUEST:
                        UserEventPackage userResponsePackage = new UserEventPackage(UserEventType.NEW_USER_RESPONSE, user);
                        sendUserEventPackage(socket, group, userResponsePackage);
                        break;
                    case NEW_USER_RESPONSE:

                        String peerPublicKey = eventRecieved.getUser().publicKey;
                        user.getKeys().put(peerPublicKey, "");
                        user.getUsernames().put(eventRecieved.getUser().getPeerName(), peerPublicKey);
                        if(user.getKeys().get(user.getPublicKey()) != ""){
                            user.setUsername();
                            UserEventPackage userConnectedPackage = new UserEventPackage(UserEventType.USER_CONNECTED, user);
                            sendUserEventPackage(socket, group, userConnectedPackage);
                        }
                        break;
                }

                System.out.println(eventRecieved.getMessage());
            }

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public static void sendUserEventPackage(MulticastSocket socket, InetAddress group, UserEventPackage eventPackage) throws IOException {
        byte[] m = SerializationUtils.serialize(eventPackage);
        DatagramPacket messageOut = new DatagramPacket(m, m.length, group, 6789);
        socket.send(messageOut);
    }

}
