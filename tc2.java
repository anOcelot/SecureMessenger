package secP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class tc2 {
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		String ip;
		String portS;
		String name;
		int port;
		ChatClient2 chat;
		//Get Numbers
		while (true) {
			try {
//				System.out.println("Enter IP: ");
//				ip = scan.nextLine();
//				System.out.println("Enter Port: ");
//				portS = scan.nextLine();
//				port = Integer.parseInt(portS);
//				System.out.println("Enter Name: ");
//				name = scan.nextLine();
				try {
					//chat = new ChatClient2(ip, port,name );
					chat = new ChatClient2("127.0.0.1", 1984, "PR");
					break;
				} catch (IOException e) {
					System.out.println("Error Connecting");
				}

			} catch (Exception e) {
				System.out.println("Error found");
				continue;
			}
		}
		
		while(true){
			System.out.print("> ");
			String msg = scan.nextLine();
			if(msg.equalsIgnoreCase("QUIT")){
				break;
			}
			chat.send(msg);
		}
		
		//chat.quit();
	}
}
