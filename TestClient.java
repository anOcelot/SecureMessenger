package Java;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by pieterholleman on 11/15/17.
 */
public class TestClient {

	public static void main(String[] args) {

		try {
			Scanner scan = new Scanner(System.in);
			System.out.print("Enter name: ");
			String user = scan.nextLine();
			ChatClient one = new ChatClient("127.0.0.1", 1984, user);
			one.go();
			//System.out.println("Connected!");

		} catch (IOException e) {
			System.out.println("Connect");
			e.printStackTrace();
		}
	}
}
