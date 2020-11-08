import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;

public class ServerConnectionChecker extends Thread{
    private Socket checkerSocket;

    public ServerConnectionChecker(Socket s){
        this.checkerSocket = s;
    }
    
    @Override
    public void run(){
        try{
            DataOutputStream heartBeat = new DataOutputStream(checkerSocket.getOutputStream());
            while(!Server.isShutDown){
                heartBeat.writeBytes("OK\n");// just send some arbitrary message to let client know server is on
                Thread.sleep(100);
            }
        }catch(Exception e){
            //if exception is raised it means XIT is triggered
        }
    }
}