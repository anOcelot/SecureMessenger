package secP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pieterholleman on 11/14/17.
 */
public class ChatServer {

	// use "synchronized" keyword
	// some java collections (i.e hashmap) act strange
	// ConcurrentHashMap
	private ArrayList<SocketChannel> clients;

	private Map clientMap;
	private List<String> chatList = Collections.synchronizedList(new ArrayList<String>());;
	private Map screenNameMap;
	private ConcurrentHashMap conUserMap;
	private ServerSocketChannel ServerChannel;
	Selector selector;

	public ChatServer(int port) throws IOException {

		clientMap = Collections.synchronizedMap(new HashMap<SocketChannel, String>());
		screenNameMap = Collections.synchronizedMap(new HashMap<String, SocketChannel>());
		conUserMap = new ConcurrentHashMap<String, SocketChannel>();
		List<String> chatList = Collections.synchronizedList(new ArrayList<String>());
		// chatMap = Collections.synchronizedMap(new HashMap<String, String>())
		selector = Selector.open();
		ServerChannel = ServerSocketChannel.open();
		ServerChannel.configureBlocking(false);
		ServerChannel.bind(new InetSocketAddress(port));
		clients = new ArrayList<SocketChannel>();
		ServerChannel.register(selector, SelectionKey.OP_ACCEPT);

	}

	public void listenForConnections() {

		System.out.println("Listening for clients");
		int i = 0;
		while (true) {

			try {

				int num = selector.select();
				if (num == 0)
					continue;
				Set keys = selector.selectedKeys();
				Iterator it = keys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					if (key.isAcceptable()) {
						SocketChannel clientSocket = ServerChannel.accept();
						clientSocket.configureBlocking(false);
						clientSocket.register(selector, SelectionKey.OP_READ);
						clients.add(clientSocket);
						// clientMap.put(clientSocket, "test");
						// screenNameMap.put("test", clientSocket);
						System.out.println("Client connected" + clientSocket);
					}

					if (key.isReadable()) {
						System.out.println("Is read");
						SocketChannel sc = (SocketChannel) key.channel();
						// System.out.println(sc);
						recieve(sc, key);
					}

					if (key.isWritable()) {
						System.out.println("Is writable");
						if (!chatList.isEmpty()) {
							String message = chatList.get(0);
							String user = message.substring(1, message.indexOf(' '));
							String messageTo = message.substring(message.indexOf(' '));
							this.sendToUser(user, messageTo);
							chatList.remove(0);
							if(chatList.isEmpty()){
								key.interestOps(SelectionKey.OP_READ);
							}
						}

					}
				}
				// clients.add(clientSocket);
				++i;
				// clientMap.put(clientSocket, i);
				keys.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void recieve(SocketChannel s, SelectionKey key) {

		ByteBuffer inBuffer = ByteBuffer.allocate(1024);

		try {
			s.read(inBuffer);
			String message = new String(inBuffer.array()).trim();
			// if (clientMap.get(s) == null){
			// clientMap.put(s, message);
			// screenNameMap.put(message, s);
			// }
			// First message, get username
			if (message.startsWith("%")) {
				String user = message.substring(1);
				System.out.println("New User: " + user);
				this.addUser(user, s);

			}

			// Message to user
			if (message.startsWith("@")) {
				if (!message.isEmpty()) {
					chatList.add(message);
					// Add to ChatQ
					System.out.println("Line 115 sendToUser");
					// this.sendToUser(user,messageTo, s);
					key.interestOps(SelectionKey.OP_WRITE);
				}

			}

			// Broadcast
			if (message.startsWith("$")) {
				broadcast(message);

			}

			// Admin Command (Kick/List)
			if (message.startsWith("!")) {
				// List
				if (message.charAt(1) == 'L') {
					key.interestOps(SelectionKey.OP_WRITE);
				}

				// Kick
				if (message.charAt(1) == 'K') {

				}
			}

			System.out.println("Got Message: " + message);
		} catch (IOException e) {
			System.out.println("Recieve error @ void recieve");
			try {
				s.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	private void broadcast(String message) {
		ByteBuffer broBuf = ByteBuffer.wrap(message.getBytes());
		for(SelectionKey key : selector.keys()){
			if(key.isValid() && key.channel() instanceof SocketChannel){
				SocketChannel sch=(SocketChannel) key.channel();
				try {
					sch.write(broBuf);
				} catch (IOException e) {
					System.out.println("BroBuf error");
				}
				broBuf.rewind();
			}
		}
		
	}

	private void addUser(String user, SocketChannel s) {
		clientMap.put(s, user);
		screenNameMap.put(user, s);
		conUserMap.put(user, s);
		System.out.println(Arrays.toString(clientMap.entrySet().toArray()));
		System.out.println(Arrays.toString(screenNameMap.entrySet().toArray()));

	}

	private void sendToUser(String user, String messageTo) {
		SocketChannel sendTo = null;
		boolean got = false;
		Iterator<Map.Entry<String, SocketChannel>> it = screenNameMap.entrySet().iterator();
		while (it.hasNext()) {
			System.out.println("Line 161 sendToUser");
			Map.Entry<String, SocketChannel> pair = it.next();
			if (user.equals(pair.getKey())) {
				System.out.println("Line 164 Got User");
				messageTo = "From " + user + "::" + messageTo;
				sendTo = pair.getValue();
				got = true;
				System.out.println(sendTo);
			}
		}

		if (got == true) {
			ByteBuffer out = ByteBuffer.wrap(messageTo.getBytes());
			try {
				sendTo.write(out);
			} catch (IOException e) {
				System.out.println("Send error");
			}
		}

	}

	private void startChatSession(SocketChannel clientSocket) {

		System.out.println("Chat session started");

		while (clientSocket.isConnected()) {

			ByteBuffer buffer = ByteBuffer.allocate(4096);
			try {

				clientSocket.read(buffer);
				String message = new String(buffer.array()).trim();
				System.out.print("Recieved message: ");
				System.out.println(message);

			} catch (IOException E) {
				System.out.println("SocketChannel read error");
			}
		}
	}

	private class ChatServerThread extends Thread {

		SocketChannel sc;

		ChatServerThread(SocketChannel channel) {
			sc = channel;
		}

		public void run() {
			startChatSession(sc);
		}
	}

}
