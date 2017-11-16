package Java;

import java.io.IOException;

/**
 * Created by pieterholleman on 11/15/17.
 */
public class TestClient {

    public static void main(String[] args) {

        try{
            ChatClient testClient = new ChatClient("127.0.0.1", 1984);
            testClient.run();
        } catch (IOException e){
            System.out.println("Connect");
            e.printStackTrace();
        }
    }
}
