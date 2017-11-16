package Java;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by pieterholleman on 11/14/17.
 */
public class ChatServer {

    //use "synchronized" keyword
    //some java collections (i.e hashmap) act strange
    //ConcurrentHashMap
    private ArrayList<SocketChannel> clients;
    private Map clientMap;
    private ServerSocketChannel ServerChannel;

    public ChatServer(int port) throws IOException {

        clientMap = Collections.synchronizedMap(new HashMap<SocketChannel, Integer>());

        ServerChannel = ServerSocketChannel.open();
        ServerChannel.bind(new InetSocketAddress(port));
        clients = new ArrayList<SocketChannel>();

    }

    public void listenForConnections() {

        System.out.println("Listening for clients");
        int i = 0;
        while(true){

            try {
                SocketChannel clientSocket = ServerChannel.accept();
                clients.add(clientSocket);
                ++i;
                clientMap.put(clientSocket, i);
                System.out.println("Client connected");

            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }



    private void startChatSession(SocketChannel clientSocket){

        System.out.println("Chat session started");

        while (clientSocket.isConnected()){

            ByteBuffer buffer = ByteBuffer.allocate(4096);
            try {

                clientSocket.read(buffer);
                String message = new String(buffer.array()).trim();
                System.out.print("Recieved message: ");
                System.out.println(message);

            } catch (IOException E){
                System.out.println("SocketChannel read error");
            }
        }
    }

    private class ChatServerThread extends Thread{

        SocketChannel sc;

        ChatServerThread(SocketChannel channel){
            sc = channel;
        }

        public void run() {
           startChatSession(sc);
        }
    }

}
