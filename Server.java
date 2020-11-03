import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{

    static Map<String,String> credentialsMap = new HashMap<>(); // Map<user_name,password>
    protected static Set<String> loggedInUsersSet = new HashSet<>(); // record user that has currently logged in
    protected static Set<String> commandsSet = new HashSet<>(); // record legal operation commands
    //protected static Set<String> activeThreadsSet =  new HashSet<>(); // record thread titles
    protected static Map<String, ThreadObj> activeThreadsMap = new HashMap<>(); //Map<threadTitle, ThreadObj>
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
        initActiveThreadsMap();

        ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Waiting for clients");

        while(true){

            Socket connectionSocket = welcomeSocket.accept();
            
            System.out.println("Client connected");
            
            Server s2 = new Server(connectionSocket); //once client connects to server, throw the rest of the jobs to the thread
            s2.start();

        }
    }

    @Override
    public void run(){

        try{

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(this.connectionSocket.getOutputStream());
            UserInformation userInfo = new UserInformation();

            // begin authentication
            boolean authenticationComplete = false;
            while(!authenticationComplete){
                authenticationComplete = authenticationHandler(userInfo, inFromClient, outToClient);
            }

            // listening for operations from client
            boolean operationsComplete = false;
            while(!operationsComplete){
                operationsComplete = commandsHadler(userInfo, inFromClient, outToClient);
            }

            loggedInUsersSet.remove(userInfo.userName);
            this.connectionSocket.close();

        }catch(Exception e){

            System.out.println("Thread crashes");

        }
    }

    //handle authentication process, return true if this process completes
    public static boolean authenticationHandler(UserInformation userInfo, BufferedReader inFromClient, DataOutputStream outToClient){
        
        try{
        
            // read user name from client
            userInfo.userName = inFromClient.readLine();
            boolean isNewUser = !credentialsMap.containsKey(userInfo.userName);
            // validate username-password
            if(isNewUser){
                System.out.println("New user");
                // notify client this is a new user
                outToClient.writeBytes("new user\n");
                outToClient.flush();

                // read user password from client
                userInfo.userPassword = inFromClient.readLine();

                credentialsMap.put(userInfo.userName, userInfo.userPassword);

                outToClient.writeBytes("new password set\n");
                outToClient.flush();
                loggedInUsersSet.add(userInfo.userName);
                writeCredentialsFile();
                System.out.println(userInfo.userName + " successfully login");
                return true;

            }else{

                if(loggedInUsersSet.contains(userInfo.userName)){

                    outToClient.writeBytes("user already logged in\n");
                    outToClient.flush();
                    System.out.println(userInfo.userName + " has already logged in");

                }else{
                    // notify client this is an old user
                    outToClient.writeBytes("old user\n");
                    outToClient.flush();
                    // read user password from client
                    userInfo.userPassword = inFromClient.readLine();

                    if(loggedInUsersSet.contains(userInfo.userName)){ //double check in case user logged in at another terminal during password typing
                        outToClient.writeBytes("user already logged in\n");
                        outToClient.flush();
                        System.out.println(userInfo.userName + " has already logged in");
                        return false;
                    }

                    if(userInfo.userPassword.equals(credentialsMap.get(userInfo.userName))){

                        outToClient.writeBytes("password correct\n");
                        outToClient.flush();
                        System.out.println(userInfo.userName + " successfully login");
                        loggedInUsersSet.add(userInfo.userName);
                        return true;

                    }else{

                        outToClient.writeBytes("passsword incorrect\n");
                        outToClient.flush();
                        System.out.println("Incorrect password");

                    }

                }
            }

        }catch(Exception e){

            System.out.println("anthenticaion handler crashes");
            System.exit(1);

        }
        return false;
    }

    // handle all commands, if the client is asked to exit then return true
    public static boolean commandsHadler(UserInformation userInfo, BufferedReader inFromClient, DataOutputStream outToClient){
        
        try{

            String[] operation = inFromClient.readLine().split(" ");
            String command = operation[0];
            System.out.println(userInfo.userName + " issued " + command + " command");
            
            if(command.equals("CRT") && operation.length == 2){

                String threadTitle = operation[1];
                File myObj = new File(threadTitle + ".txt");

                if(myObj.exists()){

                    outToClient.writeBytes("Thread " + threadTitle + " exists\n");
                    outToClient.writeBytes("\n"); //it tells multiple lines writing is end

                }else{

                    outToClient.writeBytes("Thread " + threadTitle + " created\n");
                    outToClient.writeBytes("\n");

                    // create text file 
                    try{        
                        myObj.createNewFile();
                    }catch(Exception e){
                        System.out.println("create thread crashes");
                        System.exit(1);
                    }
                    
                    // write creator of the thread to file
                    try(FileWriter fw = new FileWriter(threadTitle + ".txt", true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        PrintWriter out = new PrintWriter(bw)){
                        out.println(userInfo.userName);
                    } catch (IOException e) {
                        System.out.println("write thread crashes");
                        System.exit(1);
                    }

                    ThreadObj newThread =  new ThreadObj(threadTitle, userInfo.userName);
                    activeThreadsMap.put(threadTitle, newThread);

                }

            }else if(command.equals("LST") && operation.length == 1){

                try{

                    if(activeThreadsMap.isEmpty()){
                        outToClient.writeBytes("No threads to list\n");
                    }else{
                        outToClient.writeBytes("The list of active threads:\n");
                        Iterator<String> itr = activeThreadsMap.keySet().iterator();
                        //Iterator<String> it = activeThreadsSet.iterator();
                        while(itr.hasNext()){
                            outToClient.writeBytes(itr.next() + "\n");
                        }
                    }
                    outToClient.writeBytes("\n"); //it tells multiple lines writing is end
                    return false;

                }catch(Exception e){
                    System.out.println("list thread crashes");
                    System.exit(1);
                }

            }else if(command.equals("MSG") && operation.length >=3){

                String threadTitle = operation[1];
                String message = operation[2];
                for(int i=3; i<operation.length; i+=1){ // message concatenate into one string
                    message += " " + operation[i];
                }
                if(activeThreadsMap.containsKey(threadTitle)){
                    ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                    targetThread.appendToThread(userInfo.userName, message);
                }else{
                    outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
                }
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end

            }else if(command.equals("DLT") && operation.length == 3){

                String threadTitle = operation[1];
                int messageNumber = Integer.parseInt(operation[2]);
                if(activeThreadsMap.containsKey(threadTitle)){ //check whether thread exist
                    ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                    if(messageNumber > 0 && messageNumber <= targetThread.size()){  //check whether messageNumber is valid
                        if(userInfo.userName.equals(targetThread.getAuthorAtLine(messageNumber))){ //check whether user has the right to delete
                            targetThread.deleteLine(messageNumber);
                            outToClient.writeBytes("The message has been deleted\n");
                        }else{
                            outToClient.writeBytes(userInfo.userName + " has no right to delete\n");
                        }
                    }else{
                        outToClient.writeBytes("Message number invalid\n");
                    } 
                }else{
                    outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
                }
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end

            }else if(command.equals("RDT") && operation.length == 2){

                String threadTitle = operation[1];
                if(activeThreadsMap.containsKey(threadTitle)){
                    ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                    if(targetThread.size()==0){ //only contains header(thread creator)
                        outToClient.writeBytes("Thread " + threadTitle + " is empty\n" );
                    }else{
                        for(int i=0; i<targetThread.size(); i+=1){ // header does not display to client
                            outToClient.writeBytes(targetThread.getLineContent(i+1));
                        }
                    }
                }else{
                    outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
                }
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end

            }else if(command.equals("EDT") && operation.length >= 4){

                String threadTitle = operation[1];
                int messageNumber = Integer.parseInt(operation[2]);
                String newMessage = operation[3];
                for(int i=4; i<operation.length; i+=1){ //concatenate message into a String
                    newMessage += " " + operation[i];
                }
                if(activeThreadsMap.containsKey(threadTitle)){ //check if threadTitle exist
                    ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                    if(messageNumber > 0 && messageNumber <= targetThread.size()){//check whether messageNumber is valid
                        if(userInfo.userName.equals(targetThread.getAuthorAtLine(messageNumber))){//check whether user has the right to edit
                            targetThread.editLine(messageNumber, newMessage);
                        }else{
                            outToClient.writeBytes(userInfo.userName + " has no right to edit\n");
                        }
                    }else{
                        outToClient.writeBytes("Message number invalid\n");
                    }
                }else{
                    outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
                }
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end
                
            }else if(command.equals("UPD")){

                outToClient.writeBytes("UPD not implemented.\n");

            }else if(command.equals("DWN")){

                outToClient.writeBytes("DWNnot implemented.\n");

            }else if(command.equals("RMW")){

                outToClient.writeBytes("RMW not implemented.\n");

            }else if(command.equals("SHT")){

                outToClient.writeBytes("SHT not implemented.\n");

            }else if(command.equals("XIT") && operation.length == 1 ){

                System.out.println(userInfo.userName + " exit");
                outToClient.writeBytes("Goodbye\n");
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end
                return true;

            }else{
                outToClient.writeBytes("Invalid comand.\n");
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end
            }

        }catch(Exception e){

            System.out.println("commands handler crashes.");
            System.exit(1);

        }
        return false;
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
            System.exit(1);

        }
    }

    public static void initCommandsSet(){
        commandsSet.addAll(Arrays.asList(new String[]{"CRT","LST","MSP","DLT","RDT","EDT","UPD","DWN","RMW","XIT","SHT"}));
    }

    // load all active threads name to activetThreadsSet
    public static void initActiveThreadsMap(){
        String currentDir = System.getProperty("user.dir");
        File myObj = new File(currentDir); // we assume all thread files are store in the current directory
        String[] fileNames = myObj.list();
        for(String fileName:fileNames){
            if(fileIsThread(fileName)){
                try{
                    String threadTitle = fileName.substring(0,fileName.length()-4);
                    // first line of the thread file is the threadCreator, we can assume that it always exists
                    File f = new File(fileName);
                    Scanner rdr = new Scanner(f);
                    String threadCreator = rdr.nextLine();
                    rdr.close();
                    ThreadObj newThread = new ThreadObj(threadTitle, threadCreator);
                    activeThreadsMap.put(threadTitle, newThread);
                }catch(FileNotFoundException e){
                    System.out.println("init acitive threads crashes");
                    System.exit(1);
                } 
            }
        }
    }

    // if the file is a thread file then it must be .txt in the current working directory and not credentials.txt
    public static boolean fileIsThread(String fileName){
        return fileName.length()>4 && fileName.substring(fileName.length()-4,fileName.length()).equals(".txt") && !fileName.equals("credentials.txt");
    }
}