package Java;

import java.io.IOException;

/**************************************************************************************
 * The following class tests the server
 * 
 * @author Cody West|Peter Holleman
 * @version Project 4 Security
 * @date 12/02/2017
 *************************************************************************************/
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
