import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{
    static Map<String,String> credentialsMap = new HashMap<>(); // Map<user_name,password>
    protected static String adminPassword;
    private Socket connectionSocket;

    public Server (Socket cs){
        this.connectionSocket = cs;
    }

    public static void main(String[] args) throws Exception{

        if(args.length != 2){
            System.out.println("Usage: java Server <server_port> <admin_password>");
            return;
        }
        
        int serverPort = Integer.parseInt(args[0]);
        adminPassword = args[1];

        loadCredentials();

        ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Waiting for clients");

        while(true){
            Socket connectionSocket = welcomeSocket.accept();
            
            System.out.println("Client connected");
            
            Server s2 = new Server(connectionSocket);
            s2.start();

            
        }
    }

    public static void loadCredentials(){
        try{
            File myObj = new File("credentials.txt");
            Scanner myReader = new Scanner(myObj);
            while(myReader.hasNext()){
                String[] line = myReader.nextLine().split(" ");
                credentialsMap.put(line[0], line[1]);
            }
            myReader.close();
        }catch(IOException e){
            System.out.println("loading credentials failed.");
            System.exit(1);
        }
    }

    @Override
    public void run(){
        try{
        // begin authentication
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(this.connectionSocket.getOutputStream());

        boolean authenticationComplete = false;
        while(!authenticationComplete){
            // read user name from client
            String userName = inFromClient.readLine();
            boolean isNewUser = !credentialsMap.containsKey(userName);
            if(isNewUser){ System.out.println("New user");}

            // read user password from client
            String userPassword = inFromClient.readLine();

            // validate username-password
            if(!isNewUser){
                if(userPassword.equals(credentialsMap.get(userName))){
                    outToClient.writeBytes("password correct\n");
                    System.out.println(userName + " successful login");
                    authenticationComplete = true;
                }else{
                    outToClient.writeBytes("passsword incorrect\n");
                    System.out.println("Incorrect password");
                }
            }else{
                credentialsMap.put(userName, userPassword);
                outToClient.writeBytes("password correct\n");
                System.out.println(userName + " successful login");
                authenticationComplete = true;
            }

        }
        this.connectionSocket.close();

        }catch(Exception e){

        System.out.println("Thread crashes");

        }
        System.out.println("A thread is finished.");
    }
}