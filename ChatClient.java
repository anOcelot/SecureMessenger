package secP;



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
public class ChatClient implements Runnable {

    SocketChannel socket;
    Stack<String> messageStack;
    Selector selector;
    String screenName;
    Scanner scan = new Scanner(System.in);


    public ChatClient(String ip, int port, String str) throws IOException{
        messageStack = new Stack<String>();
        socket = SocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(ip, port);
        socket.socket().connect(address, 1000);
        socket.configureBlocking(false);
        selector = Selector.open();
        socket.register(selector, SelectionKey.OP_READ);
        socket.register(selector, SelectionKey.OP_WRITE);
        screenName = '%' + str;
        ByteBuffer nameBuf = ByteBuffer.wrap(screenName.getBytes());
        socket.write(nameBuf);
        this.run();

    }
    

	@Override
    public void run() {

        System.out.println("Chat session initiated, screenname: " + screenName);

        while(true) {

            try {
                int num = selector.select();

                if (num == 0) continue;
                Set keys = selector.selectedKeys();
                Iterator it = keys.iterator();
                while (it.hasNext()){
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    
                    if(key.isConnectable()){
                    	connect(key);
                    }
                    
                    if(key.isReadable()){
                    	recieve(key);
                    }
                    
                    if(key.isWritable()){
                    	write(key);
                    }

                }



                keys.clear();
            } catch (IOException e){

            }

            //wait for user input on one thread, receive in another?

            //recieve();



        }
    }

    private void write(SelectionKey key) {
    	System.out.println("Enter a message");
        String msg = scan.nextLine();
    	  ByteBuffer out = ByteBuffer.wrap(msg.getBytes());
          try {
              socket.write(out);
              messageStack.push(msg);
          } catch (IOException e){
              System.out.println("Send error");
          }
          
          key.interestOps(SelectionKey.OP_READ);
	}

	private void connect(SelectionKey key) {
		try {
			if(socket.finishConnect()){
				key.interestOps(SelectionKey.OP_READ);
			}
			
		} catch (IOException e) {
			key.cancel();
			e.printStackTrace();
		}
		
	}

    public void recieve(SelectionKey key){

        ByteBuffer inBuffer = ByteBuffer.allocate(1024);
        System.out.println("Line 123 Recieve");

        try {
            socket.read(inBuffer);
            String message = new String(inBuffer.array()).trim();
            System.out.println(message);
            //messageStack.push(message);
        } catch (IOException e){
            System.out.println("Recieve error @ void recieve");
        }
        
        key.interestOps(SelectionKey.OP_WRITE);

    }

    public void disconnect(){

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in disconnect");
        }

    }




}
