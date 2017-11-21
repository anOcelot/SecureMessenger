package Java;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by pieterholleman on 11/14/17.
 */
public class ChatServer {

	// use "synchronized" keyword
	// some java collections (i.e hashmap) act strange
	// ConcurrentHashMap
	private ArrayList<SocketChannel> clients;
	private Map clientMap;
	private Map screenNameMap;
	private ServerSocketChannel ServerChannel;
	Selector selector;

	public ChatServer(int port) throws IOException {

		clientMap = Collections.synchronizedMap(new HashMap<SocketChannel, String>());
		screenNameMap = Collections.synchronizedMap(new HashMap<String, SocketChannel>());
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

					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == (SelectionKey.OP_ACCEPT)) {
						SocketChannel clientSocket = ServerChannel.accept();
						clientSocket.configureBlocking(false);
						// clientSocket.register(selector,
						// SelectionKey.OP_WRITE);
						clientSocket.register(selector, SelectionKey.OP_READ);
						clients.add(clientSocket);
						clientMap.put(clientSocket, "test");
						screenNameMap.put("test", clientSocket);
						System.out.println("Client connected");
					}

					if ((key.readyOps() & SelectionKey.OP_READ) == (SelectionKey.OP_READ)) {
						SocketChannel sc = (SocketChannel) key.channel();
						recieve(sc);

						String response = "hi - from non-blocking server";
						byte[] bs = response.getBytes();
						ByteBuffer buffer = ByteBuffer.wrap(bs);

						// for (SocketChannel sock : clients){
						//
						// sock.write(buffer);
						// System.out.println("sent");
						//
						// }

					}

					if (key.isWritable()) {
						// SocketChannel sc = (SocketChannel) key.channel();
						// String response = "hi - from non-blocking server";
						// byte[] bs = response.getBytes();
						// ByteBuffer buffer = ByteBuffer.wrap(bs);
						// sc.write(buffer);
						// System.out.println("sent");
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

	private void broadcast(String message) {
		message = "BROADCAST: " + message;
		ByteBuffer broBuf = ByteBuffer.wrap(message.getBytes());
		for (SelectionKey key : selector.keys()) {
			if (key.isValid() && key.channel() instanceof SocketChannel) {
				SocketChannel sch = (SocketChannel) key.channel();
				try {
					sch.write(broBuf);
				} catch (IOException e) {
					System.out.println("BroBuf error");
				}
				broBuf.rewind();
			}
		}

	}

	private void sendToUser(String user, String messageTo, String sendUser) {
		SocketChannel sendTo = null;
		boolean got = false;
		Iterator<Map.Entry<String, SocketChannel>> it = screenNameMap.entrySet().iterator();
		while (it.hasNext()) {
			System.out.println("Line 161 sendToUser");
			Map.Entry<String, SocketChannel> pair = it.next();
			if (user.equals(pair.getKey())) {
				System.out.println("Line 164 Got User");
				messageTo = "From " + sendUser + "::" + messageTo;
				sendTo = pair.getValue();
				got = true;
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

	public void recieve(SocketChannel s) {

		ByteBuffer inBuffer = ByteBuffer.allocate(1024);

		try {
			s.read(inBuffer);
			String message = new String(inBuffer.array()).trim();
			// if (clientMap.get(s) == null){
			// clientMap.put(s, message);
			// screenNameMap.put(message, s);
			// }
			System.out.println(message);

			if (message.startsWith("%")) {
				s.close();
			}

			if (message.startsWith("$")) {
				broadcast(message.substring(1));
			}

			if (message.startsWith("@")) {
				String user = message.substring(1, message.indexOf(' '));
				String messageTo = message.substring(message.indexOf(' '));
				Iterator<Map.Entry<String, SocketChannel>> it = screenNameMap.entrySet().iterator();
				String sendUser = "";
				while (it.hasNext()) {
					System.out.println("Line 161 sendToUser");
					Map.Entry<String, SocketChannel> pair = it.next();
					if (s == pair.getValue()) {
						sendUser = pair.getKey();
					}
				}
				sendToUser(user, messageTo, sendUser);
			}

			if (message.startsWith("#")) {
				String user = message.substring(1);
				System.out.println("New User: " + user);
				clientMap.put(s, user);
				screenNameMap.put(user, s);
				System.out.println(Arrays.toString(clientMap.entrySet().toArray()));
				System.out.println(Arrays.toString(screenNameMap.entrySet().toArray()));
			}
			
			if(message.equalsIgnoreCase("!List")){
				String list = "Users: ";
				Iterator<Map.Entry<String, SocketChannel>> it = screenNameMap.entrySet().iterator();
				while (it.hasNext()) {
					System.out.println("Line 161 sendToUser");
					Map.Entry<String, SocketChannel> pair = it.next();
					if(pair.getValue()!=s){
						list = list + pair.getKey() + "|";
					}
				}
				
				ByteBuffer out = ByteBuffer.wrap(list.getBytes());
				try {
					s.write(out);
				} catch (IOException e) {
					System.out.println("Send error");
				}
				
				
			}

		} catch (IOException e) {
			System.out.println("Recieve error");
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
				if (message.startsWith("%")) {
				}

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
