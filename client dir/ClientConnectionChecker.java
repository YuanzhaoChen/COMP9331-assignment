import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;

public class ClientConnectionChecker extends Thread{
    private Socket checkerSocket;

    public ClientConnectionChecker(Socket s){
        checkerSocket = s;
    }

    @Override
    public void run(){
        try{
            BufferedReader heartBeat = null;
            heartBeat = new BufferedReader(new InputStreamReader(checkerSocket.getInputStream()));
            
            while(true){
                String s = heartBeat.readLine();
                if(s==null){
                    Client.isShutDown = true; //tell Client to shut down
                    break;
                }
                Thread.sleep(100);
            }
            checkerSocket.close();
        }catch(Exception e){
            System.out.println("ServerConnectionChecker crashes");
        }
    }
}