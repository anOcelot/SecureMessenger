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

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
	private Map clientKeyMap;
	private Map nameKeyMap;
	private ServerSocketChannel ServerChannel;
	Selector selector;
	cryptotest encoder;

	public ChatServer(int port) throws IOException {

		encoder = new cryptotest();
		encoder.setPrivateKey("RSApriv.der");
		encoder.setPublicKey("RSApub.der");
		clientMap = Collections.synchronizedMap(new HashMap<SocketChannel, String>());
		screenNameMap = Collections.synchronizedMap(new HashMap<String, SocketChannel>());
		clientKeyMap = Collections.synchronizedMap(new HashMap<SocketChannel, byte[]>());
		nameKeyMap = Collections.synchronizedMap(new HashMap<String, String>());
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
						// clientMap.put(clientSocket, "test");
						// screenNameMap.put("test", clientSocket);
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
					Iterator<Map.Entry<SocketChannel, byte[]>> itA = clientKeyMap.entrySet().iterator();
					SecretKey symKey = null;
					while (itA.hasNext()) {
						Map.Entry<SocketChannel, byte[]> pair = itA.next();
						if (sch == pair.getKey()) {
							symKey = new SecretKeySpec(pair.getValue(), "AES");
						}
					}
					
					byte ivBytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
					IvParameterSpec iv = new IvParameterSpec(ivBytes);
					byte[] sendMsgEnc = encoder.encrypt(message.getBytes(), symKey, iv);
					byte[] seq = ByteBuffer.allocate(4).putInt(sendMsgEnc.length).array();
					byte[] sendB = new byte[4 + sendMsgEnc.length];
					System.arraycopy(seq, 0, sendB, 0, 4);
					System.arraycopy(sendMsgEnc, 0, sendB, 4, sendMsgEnc.length);
					ByteBuffer out = ByteBuffer.wrap(sendB);
					sch.write(out);
					out.rewind();
				} catch (IOException e) {
					System.out.println("BroBuf error");
				}
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
			Iterator<Map.Entry<SocketChannel, byte[]>> itA = clientKeyMap.entrySet().iterator();
			SecretKey symKey = null;
			while (itA.hasNext()) {
				Map.Entry<SocketChannel, byte[]> pair = itA.next();
				if (sendTo == pair.getKey()) {
					symKey = new SecretKeySpec(pair.getValue(), "AES");
				}
			}

			byte ivBytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
			IvParameterSpec iv = new IvParameterSpec(ivBytes);
			byte[] sendMsgEnc = encoder.encrypt(messageTo.getBytes(), symKey, iv);
			byte[] seq = ByteBuffer.allocate(4).putInt(sendMsgEnc.length).array();
			byte[] sendB = new byte[4 + sendMsgEnc.length];
			System.arraycopy(seq, 0, sendB, 0, 4);
			System.arraycopy(sendMsgEnc, 0, sendB, 4, sendMsgEnc.length);
			ByteBuffer out = ByteBuffer.wrap(sendB);
			try {
				sendTo.write(out);
			} catch (IOException e) {
				System.out.println("Send error");
			}
		}

	}

	private void sendList(SocketChannel s, String messageTo) {

		Iterator<Map.Entry<SocketChannel, byte[]>> itA = clientKeyMap.entrySet().iterator();
		SecretKey symKey = null;
		while (itA.hasNext()) {
			Map.Entry<SocketChannel, byte[]> pair = itA.next();
			if (s == pair.getKey()) {
				symKey = new SecretKeySpec(pair.getValue(), "AES");
			}
		}

		byte ivBytes[] = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
		IvParameterSpec iv = new IvParameterSpec(ivBytes);
		byte[] sendMsgEnc = encoder.encrypt(messageTo.getBytes(), symKey, iv);
		byte[] seq = ByteBuffer.allocate(4).putInt(sendMsgEnc.length).array();
		byte[] sendB = new byte[4 + sendMsgEnc.length];
		System.arraycopy(seq, 0, sendB, 0, 4);
		System.arraycopy(sendMsgEnc, 0, sendB, 4, sendMsgEnc.length);
		ByteBuffer out = ByteBuffer.wrap(sendB);
		try {
			s.write(out);
		} catch (IOException e) {
			System.out.println("Send error");
		}

	}

	public void recieve(SocketChannel s) {

		try {

			// if (clientMap.get(s) == null){
			// clientMap.put(s, message);
			// screenNameMap.put(message, s);
			// }
			// System.out.println(message);

			if (!clientKeyMap.containsKey(s)) {
				ByteBuffer inBuffer = ByteBuffer.allocate(256);
				s.read(inBuffer);
				String messageEnc = new String(inBuffer.array()).trim();
				byte b128[] = inBuffer.array();
				byte b2[] = new byte[256];
				// byte[] sKey = encoder.RSADecrypt(inBuffer.array());
				// byte[] sKey = encoder.RSADecrypt(messageEnc.getBytes());
				int bSize = b128.length;
				System.out.println("Size: " + bSize);
				// for (byte b : b128){
				// System.out.print(b + " ");
				// }
				byte[] sKey = encoder.RSADecrypt(b128);
				clientKeyMap.put(s, sKey);
			}

			else {
				ByteBuffer inBuffer = ByteBuffer.allocate(1042);
				s.read(inBuffer);
				byte[] msgTot = inBuffer.array();
				byte[] msgSizeB = new byte[4];
				System.arraycopy(msgTot, 0, msgSizeB, 0, 4);
				byte[] otherWay = Arrays.copyOfRange(msgSizeB, 0, 4);
				int msgSize = ByteBuffer.wrap(otherWay).getInt();
				// System.out.println("MSG SIZE: " + msgSize);
				// for (byte b : msgSizeB){
				// System.out.print(b + " ");
				// }
				byte[] msgStringEnc = new byte[msgSize];
				System.arraycopy(msgTot, 4, msgStringEnc, 0, msgSize);

				Iterator<Map.Entry<SocketChannel, byte[]>> itA = clientKeyMap.entrySet().iterator();
				SecretKey symKey = null;
				// maybe?
				// symKey = new SecretKeySpec((byte[])clientKeyMap.get(s),
				// "AES");
				while (itA.hasNext()) {
					Map.Entry<SocketChannel, byte[]> pair = itA.next();
					if (s == pair.getKey()) {
						symKey = new SecretKeySpec(pair.getValue(), "AES");
					}
				}

				byte ivBytes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
				IvParameterSpec iv = new IvParameterSpec(ivBytes);
				String message;
				message = new String(encoder.decrypt(msgStringEnc, symKey, iv));

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

				if (message.equalsIgnoreCase("!List")) {
					String list = "Users: ";
					Iterator<Map.Entry<String, SocketChannel>> it = screenNameMap.entrySet().iterator();
					while (it.hasNext()) {
						System.out.println("Line 161 sendToUser");
						Map.Entry<String, SocketChannel> pair = it.next();
						if (pair.getValue() != s) {
							list = list + pair.getKey() + "|";
						}
					}

					sendList(s, list);
				}

				// ByteBuffer out = ByteBuffer.wrap(list.getBytes());
				// try {
				// s.write(out);
				// } catch (IOException e) {
				// System.out.println("Send error");
				// }

			}

		} catch (

		IOException e) {
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
