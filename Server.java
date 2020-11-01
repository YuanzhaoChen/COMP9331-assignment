import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{
    static List<SocketAddress> clients = new ArrayList<SocketAddress>();
    static Map<String,String> credentialMap = new HashMap<>(); // Map<user_name,password>
    static ReentrantLock syncLock = new ReentrantLock();
    static int UPDATE_INTERVAL = 1000;//milliseconds
    public static String adminaPassword;

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

        //Server s2 = new Server();
        //s2.run();

        while(true){
            Socket connectionSocket = welcomeSocket.accept();
            
            System.out.println("Client connected");
            // begin authentication
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            boolean authenticationComplete = false;
            while(!authenticationComplete){
                // read user name from client
                String userName = inFromClient.readLine();

                boolean isNewUser = !credentialMap.containsKey(userName);
                if(isNewUser){
                    System.out.println("New user");
                }

                //read user password from client
                String userPassword = inFromClient.readLine();

                if(!isNewUser){
                    if(userPassword.equals(credentialMap.get(userName))){
                        outToClient.writeBytes("password correct\n");
                        authenticationComplete = true;
                    }else{
                        outToClient.writeBytes("passsword incorrect\n");
                    }
                }else{
                    credentialMap.put(userName, userPassword);
                    outToClient.writeBytes("password correct\n");
                    authenticationComplete = true;
                }

                System.out.println(userName);
                System.out.println(userPassword);

            }
            
            
            

            connectionSocket.close();
        }
    }

    public static void loadCredentials(){
        try{
            File myObj = new File("credentials.txt");
            Scanner myReader = new Scanner(myObj);
            while(myReader.hasNext()){
                String[] line = myReader.nextLine().split(" ");
                credentialMap.put(line[0], line[1]);
            }
            myReader.close();
        }catch(IOException e){
            System.out.println("loading credentials failed.");
            System.exit(1);
        }
    }

    @Override
    public void run(){
        // once authentication is successful, throw the rest of the server-client interaction to here
        System.out.println("Hi~");
    }
}