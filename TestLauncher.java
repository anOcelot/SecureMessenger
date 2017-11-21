package Java;

import java.io.IOException;

/**
 * Created by pieterholleman on 11/15/17.
 */
public class TestLauncher {


    public static void main(String[] args) {

        try {

           ChatServer testServer = new ChatServer(1984);
           testServer.listenForConnections();

        } catch (IOException e){
            System.out.println("Connection failed");
            e.printStackTrace();
        }

    }

}
