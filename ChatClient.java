package Java;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;

/**
 * Created by pieterholleman on 11/14/17.
 */
public class ChatClient {

    SocketChannel socket;
    Stack<String> messageStack;


    public ChatClient(String ip, int port) throws IOException{
        messageStack = new Stack<String>();
        socket = SocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(ip, port);
        socket.socket().connect(address, 1000);

    }

    public void run() {

        System.out.println("Receiving...");

        while(true) {

            //wait for user input on one thread, receive in another?
            recieve();
            System.out.println(messageStack.peek());


        }
    }



    public void send(String msg){

        ByteBuffer out = ByteBuffer.wrap(msg.getBytes());
        try {
            socket.write(out);
            messageStack.push(msg);
        } catch (IOException e){
            System.out.println("Send error");
        }
    }

    public void recieve(){

        ByteBuffer inBuffer = ByteBuffer.allocate(1024);

        try {
            socket.read(inBuffer);
            String message = new String(inBuffer.array()).trim();
            messageStack.push(message);
        } catch (IOException e){
            System.out.println("Recieve error");
        }

    }

    public void disconnect(){

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in disconnect");
        }

    }


}
