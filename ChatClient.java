package Java;



import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**************************************************************************************
 * The following class is a chat client that links to a speciied server and encrypts
 * messages sent to the server with a generated AES key that the server
 * also holds
 * 
 * @author Cody West|Peter Holleman
 * @version Project 4 Security
 * @date 12/02/2017
 *************************************************************************************/
public class ChatClient {

    SocketChannel socket;
    Stack<String> messageStack;
    Selector selector;
    String screenName;
    cryptotest encoder;
    SecretKey sKey;
    byte sKeyB[];
    byte ivBytes[];
    SecureRandom r;
    

    /*************************************************************************
	 * Constructor creates client with proper sockets, channel, 
	 * IP address and port.
	 * @param port pre-defined port for client
	 * @param ip pre-defined ip for client
	 * @param str username for the client
	 ************************************************************************/
    public ChatClient(String ip, int port, String str) throws IOException{
        messageStack = new Stack<String>();
        socket = SocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(ip, port);
        selector = Selector.open();
        encoder = new cryptotest();
        encoder.setPublicKey("RSApub.der");
        sKey = encoder.generateAESKey();
        sKeyB = encoder.RSAEncrypt(sKey.getEncoded());
        socket.socket().connect(address, 1000);
        int bSize = sKeyB.length;
        //System.out.println("Size " + bSize);
        //for (byte b: sKeyB){
        //    System.out.print(b + " ");
        //}
        socket.configureBlocking(false);
        //socket.register(selector, SelectionKey.OP_READ);
        socket.register(selector, SelectionKey.OP_WRITE);
        socket.register(selector, SelectionKey.OP_READ);
        screenName = str;
        //socket.socket().connect(address, 1000);

    }

    /*************************************************************************
	 * Method for keeping track of read and write operations
	 ************************************************************************/
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

    /*************************************************************************
	 * Method for taking user input and encrypting it then sending it to the
	 * server
	 ************************************************************************/
    public void go(){

        ChatClientT t = new ChatClientT();
        t.start();
        sendKey(sKeyB);

        byte ivBytes[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
        //SecureRandom r = new SecureRandom();
        //r.nextBytes(ivBytes);

        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        String userSend = "#" + screenName;
        send(encoder.encrypt(userSend.getBytes(), sKey,iv));
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter a message");
        while(true){
            System.out.println("Enter a message");
            String message = scan.nextLine();
            send(encoder.encrypt(message.getBytes(), sKey,iv));


        }
    }

    /*************************************************************************
	 * Thread method for chat client
	 ************************************************************************/
    private class ChatClientT extends Thread {

         ChatClientT() { }

         public void run(){
             runChat();
         }


    }



    /*************************************************************************
	 * Method that takes a string and sends it to server
	 * (Old and no longer used)
	 * @param msg the message to be sent
	 ************************************************************************/
    public void send(String msg){

        ByteBuffer out = ByteBuffer.wrap(msg.getBytes());
        try {
            socket.write(out);
            messageStack.push(msg);
        } catch (IOException e){
            System.out.println("Send error");
        }
    }

    /*************************************************************************
  	 * Method that takes a string and sends it to server
  	 * (Old and no longer used)
  	 * @param b the encrypted message to be sent
  	 ************************************************************************/
    public void send(byte b[]){
        try {
        	byte[] seq = ByteBuffer.allocate(4).putInt(b.length).array();
        	byte[] sendB = new byte[4+b.length];
        	System.arraycopy(seq,0,sendB,0,4);
        	System.arraycopy(b,0,sendB,4,b.length);
//        	System.out.println("MSG SIZE: " + sendB.length);
//        	byte[] otherWay = Arrays.copyOfRange(seq, 0, 4);
//			int msgSize = ByteBuffer.wrap(otherWay).getInt();
//			System.out.println("MSG SIZE FROM BUFF: " + msgSize);
//			byte ivBytes[] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
//	        IvParameterSpec iv = new IvParameterSpec(ivBytes);
//			System.out.println(encoder.decrypt(b, sKey, iv).toString());
//			String byteMsg = new String(encoder.decrypt(b, sKey, iv));
//			System.out.println(byteMsg);
//        	for (byte b22 : seq){
//        		System.out.print(b22 + " ");
//        	}
        	
        	
        	
            socket.write(ByteBuffer.wrap(sendB));
        } catch (IOException e){
            System.out.println("Send error");
        }
    }

    /*************************************************************************
  	 * Method that is specifically used to send the AES key with RSA
  	 * encryption
  	 * @param b the encrypted key to be sent
  	 ************************************************************************/
    public void sendKey(byte b[]){
        try {
            socket.write(ByteBuffer.wrap(b));
        } catch (IOException e){
            System.out.println("Send error");
        }
    }
    
    /*************************************************************************
  	 * Method that recieves encrypted messages from the server and
  	 * decrypts them to print for the user
  	 ************************************************************************/
    public void recieve(){

        ByteBuffer inBuffer = ByteBuffer.allocate(1024);

        try {
            socket.read(inBuffer);
            
            byte[] msgTot = inBuffer.array();
			byte[] msgSizeB = new byte[4];
			System.arraycopy(msgTot,0,msgSizeB,0,4);
			byte[] otherWay = Arrays.copyOfRange(msgSizeB, 0, 4);
			int msgSize = ByteBuffer.wrap(otherWay).getInt();
			byte[] msgStringEnc = new byte[msgSize];
			System.arraycopy(msgTot,4,msgStringEnc,0,msgSize);
            byte ivBytes[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
            //SecureRandom r = new SecureRandom();
            //r.nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            String message = new String(encoder.decrypt(msgStringEnc, sKey, iv));
            System.out.println(message);
            messageStack.push(message);
        } catch (IOException e){
            System.out.println("Recieve error");
        }

    }

    /*************************************************************************
  	 * Method to disconnect cleanly from server
  	 ************************************************************************/
    public void disconnect(){

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error in disconnect");
        }

    }


}
