import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{
    static Map<String,String> credentialsMap = new HashMap<>(); // Map<user_name,password>
    protected static Set<String> loggedInUsers = new HashSet<>(); //record user that has currently logged in
    protected static String adminPassword;
    static ReentrantLock syncLock = new ReentrantLock();
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

        updateCredentials();

        ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Waiting for clients");

        while(true){
            Socket connectionSocket = welcomeSocket.accept();
            
            System.out.println("Client connected");
            
            Server s2 = new Server(connectionSocket);
            s2.start();

            
        }
    }

    @Override
    public void run(){
        try{

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(this.connectionSocket.getOutputStream());
            
            // begin authentication
            boolean authenticationComplete = false;
            String userName = "Nan", userPassword="Nan"; //give it an initialized value for successfully compile 
            while(!authenticationComplete){

                // read user name from client
                
                userName = inFromClient.readLine();
                boolean isNewUser = !credentialsMap.containsKey(userName);
                boolean userLoggedIn = loggedInUsers.contains(userName);
                // validate username-password
                if(isNewUser){
                    System.out.println("New user");
                    // notify client this is a new user
                    outToClient.writeBytes("new user\n");

                    // read user password from client
                    userPassword = inFromClient.readLine();

                    credentialsMap.put(userName, userPassword);

                    outToClient.writeBytes("password correct\n");
                    loggedInUsers.add(userName);
                    updateCredentials();
                    authenticationComplete = true;
                    System.out.println(userName + " successful login");

                }else{
                    if(userLoggedIn){
                        outToClient.writeBytes("user already logged in\n");
                        System.out.println(userName + " has already logged in");
                    }else{
                        // notify client this is an old user
                        outToClient.writeBytes("old user\n");
                        // read user password from client
                        userPassword = inFromClient.readLine();

                        if(userPassword.equals(credentialsMap.get(userName))){
                            outToClient.writeBytes("password correct\n");
                            System.out.println(userName + " successful login");
                            loggedInUsers.add(userName);
                            authenticationComplete = true;
                        }else{
                            outToClient.writeBytes("passsword incorrect\n");
                            System.out.println("Incorrect password");
                        }

                    }
                }

            }

            loggedInUsers.remove(userName);
            this.connectionSocket.close();

        }catch(Exception e){

            System.out.println("Thread crashes");

        }
        System.out.println("A thread is finished.");
    }

    public static void loadCredentials(){
        try{
            syncLock.lock(); // we don't want other threads write on credentials.txt while we're reading
            File myObj = new File("credentials.txt");
            Scanner myReader = new Scanner(myObj);
            while(myReader.hasNext()){
                String[] line = myReader.nextLine().split(" ");
                credentialsMap.put(line[0], line[1]);
            }
            myReader.close();
            syncLock.unlock();
        }catch(IOException e){
            System.out.println("load credentials failed.");
            System.exit(1);
        }
    }

    public static void updateCredentials(){
        try{
            syncLock.lock(); // we don't want other threads read credentials.txt before writing is done
            PrintWriter out = new PrintWriter("credentials.txt");
            Iterator<String> it = credentialsMap.keySet().iterator();
            while(it.hasNext()){
                String userName =  it.next();
                out.println(userName + " " + credentialsMap.get(userName));
            }
            out.close();
            syncLock.unlock();
        }catch(IOException e){
            System.out.println("update credentials failed.");
        }
    }
}