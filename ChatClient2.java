package secP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class ChatClient2 {

	SocketChannel socket;
	Stack<String> messageStack;
	Selector selector;
	String screenName;
	Scanner scan = new Scanner(System.in);

	public ChatClient2(String ip, int port, String str) throws IOException {
		messageStack = new Stack<String>();
		socket = SocketChannel.open();
		InetSocketAddress address = new InetSocketAddress(ip, port);
		socket.socket().connect(address, 1000);
		socket.configureBlocking(false);
		// selector = Selector.open();
		screenName = '%' + str;
		ByteBuffer nameBuf = ByteBuffer.wrap(screenName.getBytes());
		socket.write(nameBuf);

		new ClientListener().start();

	}

	public void send(String msg) {
		ByteBuffer out = ByteBuffer.wrap(msg.getBytes());
		try {
			socket.write(out);
		} catch (IOException e) {
			System.out.println("Send error");
		}
	}

	class ClientListener extends Thread {
		public void run() {
			while (true) {
				ByteBuffer inBuffer = ByteBuffer.allocate(1024);
				// System.out.println("Line 123 Recieve");
				try {
					int x = socket.read(inBuffer);
					if (x > 0) {
						String message = new String(inBuffer.array()).trim();
					}
					// messageStack.push(message);
				} catch (IOException e) {
					// System.out.println("Recieve error @ void recieve");
					continue;
				}

			}
		}
	}
}
