import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{
    static Map<String,String> credentialsMap = new HashMap<>(); // Map<user_name,password>
    protected static Set<String> loggedInUsersSet = new HashSet<>(); //record user that has currently logged in
    protected static Set<String> commandsSet = new HashSet<>(); //record legal operation commands
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

        initCredentialsMap();
        initCommandsSet();

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
                boolean userLoggedIn = loggedInUsersSet.contains(userName);
                // validate username-password
                if(isNewUser){
                    System.out.println("New user");
                    // notify client this is a new user
                    outToClient.writeBytes("new user\n");

                    // read user password from client
                    userPassword = inFromClient.readLine();

                    credentialsMap.put(userName, userPassword);

                    outToClient.writeBytes("new password set\n");
                    loggedInUsersSet.add(userName);
                    writeCredentialsFile();
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
                            System.out.println(userName + " successfully login");
                            loggedInUsersSet.add(userName);
                            authenticationComplete = true;
                        }else{
                            outToClient.writeBytes("passsword incorrect\n");
                            System.out.println("Incorrect password");
                        }

                    }
                }

            }

            // listening for operations from client
            boolean operationsComplete = false;
            while(!operationsComplete){
                String[] operation = inFromClient.readLine().split(" ");
                if(operation.length!=2){
                    outToClient.writeBytes("invalid command\n");
                    continue;
                }
                String command = operation[0];
                String argument = operation[1];
                commandsHadler(userName, command, argument, outToClient);
            }

            loggedInUsersSet.remove(userName);
            this.connectionSocket.close();

        }catch(Exception e){

            System.out.println("Thread crashes");

        }
        System.out.println("A thread is finished.");
    }

    public static void initCredentialsMap(){
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
        }catch(Exception e){
            System.out.println("load credentials failed.");
            System.exit(1);
        }
    }

    // write current credential information back to credentials.txt
    public static void writeCredentialsFile(){
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

    public static void initCommandsSet(){
        commandsSet.addAll(Arrays.asList(new String[]{"CRT","LST","MSP","DLT","RDT","EDT","UPD","DWN","RMW","XIT","SHT"}));
    }

    public static void commandsHadler(String userName, String command, String argument, DataOutputStream outToClient){
        try{
            System.out.println(userName + " issued " + command +" command");
            if(command.equals("CRT")){
                
                outToClient.writeBytes("CRT not implemented.\n");

            }else if(command.equals("LST")){

                outToClient.writeBytes("LST not implemented.\n");

            }else if(command.equals("MSP")){

                outToClient.writeBytes("MSP not implemented.\n");

            }else if(command.equals("DLT")){

                outToClient.writeBytes("DLT not implemented.\n");

            }else if(command.equals("RDT")){

                outToClient.writeBytes("RDT not implemented.\n");

            }else if(command.equals("EDT")){

                outToClient.writeBytes("EDT not implemented.\n");

            }else if(command.equals("UPD")){

                outToClient.writeBytes("UPD not implemented.\n");

            }else if(command.equals("DWN")){

                outToClient.writeBytes("DWNnot implemented.\n");

            }else if(command.equals("RMW")){

                outToClient.writeBytes("RMW not implemented.\n");

            }else if(command.equals("XIT")){

                outToClient.writeBytes("XIT not implemented.\n");

            }else if(command.equals("SHT")){

                outToClient.writeBytes("SHT not implemented.\n");

            }else{
                outToClient.writeBytes("Invalid comand.\n");
            }
        }catch(Exception e){
            System.out.println("commands handler crashes.");
        }
    }
}