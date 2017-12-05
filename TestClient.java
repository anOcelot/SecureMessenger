package Java;

import java.io.IOException;
import java.util.Scanner;

/**************************************************************************************
 * The following class tests the client
 * 
 * @author Cody West|Peter Holleman
 * @version Project 4 Security
 * @date 12/02/2017
 *************************************************************************************/
public class TestClient {

	public static void main(String[] args) {

		try {
			Scanner scan = new Scanner(System.in);
			System.out.print("Enter name: ");
			String user = scan.nextLine();
			System.out.print("Enter IP: ");
			String ip = scan.nextLine();
			ChatClient one = new ChatClient(ip, 1984, user);
			one.go();
			System.out.println("Connected!");

		} catch (IOException e) {
			System.out.println("Connect");
			e.printStackTrace();
		}
	}
}
