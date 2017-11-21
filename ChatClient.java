package Java;



import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by pieterholleman on 11/14/17.
 */
public class ChatClient {

    SocketChannel socket;
    Stack<String> messageStack;
    Selector selector;
    String screenName;


    public ChatClient(String ip, int port, String str) throws IOException{
        messageStack = new Stack<String>();
        socket = SocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(ip, port);
        selector = Selector.open();
        socket.socket().connect(address, 1000);
        socket.configureBlocking(false);
        //socket.register(selector, SelectionKey.OP_READ);

        socket.register(selector, SelectionKey.OP_WRITE);
        socket.register(selector, SelectionKey.OP_READ);
        screenName = str;
        //socket.socket().connect(address, 1000);

    }

    public void runChat() {

        System.out.println("Chat session initiated, screenname: " + screenName);

        while(true) {

            try {
                int num = selector.select();

                if (num == 0) continue;
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()){
                    SelectionKey key = (SelectionKey) it.next();

                    if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ){
                        recieve();
                    }

                    if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE){

                    }

                }



                keys.clear();
            } catch (IOException e){

            }

            //wait for user input on one thread, receive in another?

            //recieve();



        }
    }

    public void go(){

        ChatClientT t = new ChatClientT();
        t.start();
        send( '#' + screenName);
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter a message");
        while(true){
            //System.out.println("Enter a message");
            String message = scan.nextLine();
            send(message);


        }
    }

    private class ChatClientT extends Thread {

         ChatClientT() { }

         public void run(){
             runChat();
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
            System.out.println(message);
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
