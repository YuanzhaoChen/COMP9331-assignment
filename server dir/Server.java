import java.io.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.util.concurrent.locks.*;

public class Server extends Thread{
    private static Map<String,String> credentialsMap = new HashMap<>(); // key: user_name, value: password
    private static Set<String> loggedInUsersSet = new HashSet<>();      // record user that has currently logged in
    protected static Set<String> commandsSet = new HashSet<>();         // record legal operation commands
    private static int activeClientNum = 0;
    private static Map<String, ThreadObj> activeThreadsMap = new HashMap<>(); // key: threadTitle, value: ThreadObj
    private static String adminPassword;
    private static ReentrantLock syncLock = new ReentrantLock();
    private static ServerSocket welcomeSocket;
    private Socket connectionSocket;
    public static boolean isShutDown = false;

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

        welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Waiting for clients");

        while(true){
            Socket newSocket=null;
            try{
                newSocket = welcomeSocket.accept();
            }catch(SocketException e){ // exception will throw if a thread close the welcome socket, 
                System.out.println("Server shutting down");
                isShutDown = true;
                Thread.sleep(300);
                welcomeSocket.close(); // usually occur when SHT issued successfully
                System.exit(0);         // this will close all child threads
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));

            if(in.readLine().equals("1")){          // type 1 socket is the one interacts with the forum
                System.out.println("Client connected");
                Server s = new Server(newSocket);   // once client connects to server, the rest of the jobs are done by thread
                syncLock.lock();                    // common resource should not be changed by more than one thread at the same time
                activeClientNum += 1;
                syncLock.unlock();
                s.start();
            }else{                                  // type 2 socket checks server-client connection
                ServerConnectionChecker c = new ServerConnectionChecker(newSocket);
                c.start();
            }

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

            // serving requests from client
            boolean operationsComplete = false;
            while(!operationsComplete){
                operationsComplete = commandsHadler(userInfo, inFromClient, outToClient);
            }

            // client is done
            loggedInUsersSet.remove(userInfo.userName);
            if(!this.connectionSocket.isClosed()){
                this.connectionSocket.close();
            }

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

            System.out.println("authenticaion handler crashes");
            System.exit(1);

        }
        return false;
    }

    // handle all commands, if the server is asked to exit/shutdown then return true
    public static boolean commandsHadler(UserInformation userInfo, BufferedReader inFromClient, DataOutputStream outToClient){
        boolean retval = false;
        try{

            String[] operation = inFromClient.readLine().split(" ");
            String command = operation[0];

            if(commandsSet.contains(command)){
                System.out.println(userInfo.userName + " issued " + command + " command");
            }
            
            if(command.equals("CRT") && operation.length == 2){

                CRT_handler(operation, userInfo, outToClient);

            }else if(command.equals("LST") && operation.length == 1){

                LST_handler(outToClient);

            }else if(command.equals("MSG") && operation.length >=3){

                MSG_handler(operation, userInfo, outToClient);

            }else if(command.equals("DLT") && operation.length == 3 && isInteger(operation[2])){

                DLT_handler(operation, userInfo, outToClient);

            }else if(command.equals("RDT") && operation.length == 2){

                RDT_handler(operation, outToClient);

            }else if(command.equals("EDT") && operation.length >= 4 && isInteger(operation[2])){

                EDT_handler(operation, userInfo, outToClient);

            }else if(command.equals("UPD")){

                outToClient.writeBytes("UPD not implemented.\n");

            }else if(command.equals("DWN")){

                outToClient.writeBytes("DWNnot implemented.\n");

            }else if(command.equals("RMV") && operation.length == 2){

                RMV_handler(operation, userInfo, outToClient);

            }else if(command.equals("SHT") && operation.length == 2){

                retval = SHT_handler(operation, outToClient);

            }else if(command.equals("XIT") && operation.length == 1 ){

                retval = XIT_handler(userInfo, outToClient);

            }else{
                
                invalidCommand_handler(command, outToClient);

            }

        }catch(Exception e){ // exception will throw if another client close all sockets when shutting down the server, so readLine() crashes

            System.exit(1);

        }
        return retval;
    }

    public static boolean isInteger(String s){
        for(int i=0; i<s.length(); i+=1){
            if(s.charAt(i)<'0' || s.charAt(i)>'9'){
                return false;
            }
        }
        return true;
    }

    // if the file is a thread file then it must be .txt in the current working directory and not credentials.txt
    public static boolean fileIsThread(String fileName){
        return fileName.length()>4 && fileName.substring(fileName.length()-4,fileName.length()).equals(".txt") && !fileName.equals("credentials.txt");
    }

    // load user-password pair into the program
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

    // handle CRT commands from the client
    public static void CRT_handler(String[] operation, UserInformation userInfo, DataOutputStream outToClient){
        try{
            String threadTitle = operation[1];
            File myObj = new File(threadTitle + ".txt");
            if(myObj.exists()){
                System.out.println("Thread " + threadTitle + " exists");
                outToClient.writeBytes("Thread " + threadTitle + " exists\n");
                outToClient.writeBytes("\n"); //it tells multiple lines writing is end
            }else{
                System.out.println("Thread " + threadTitle + " created");
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

        }catch(Exception e){

            System.out.println("CRT_handler crashes");
            
        }
        
    }

    // handle LST command from the client
    public static void LST_handler(DataOutputStream outToClient){
        try{

            if(activeThreadsMap.isEmpty()){
                outToClient.writeBytes("No threads to list\n");
            }else{
                outToClient.writeBytes("The list of active threads:\n");
                Iterator<String> itr = activeThreadsMap.keySet().iterator();
                while(itr.hasNext()){
                    outToClient.writeBytes(itr.next() + "\n");
                }
            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(Exception e){

            System.out.println("LST_handler crashes");

        }
    }

    // handle MSG command from the client
    public static void MSG_handler(String[] operation, UserInformation userInfo, DataOutputStream outToClient){
        try{

            String threadTitle = operation[1];
            String message = operation[2];
            for(int i=3; i<operation.length; i+=1){ // message concatenate into one string
                message += " " + operation[i];
            }
            if(activeThreadsMap.containsKey(threadTitle)){
                ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                targetThread.appendToThread(userInfo.userName, message);
                System.out.println("Message posted to " + threadTitle + " thread");
            }else{
                System.out.println("Thread " + threadTitle + " does not exist");
                outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(Exception e){

            System.out.println("MSG_handler crashes");

        }
        
    }

    // handle DLT command from the client
    public static void DLT_handler(String[] operation, UserInformation userInfo, DataOutputStream outToClient){
        try{

            String threadTitle = operation[1];
            int messageNumber = Integer.parseInt(operation[2]);
            if(activeThreadsMap.containsKey(threadTitle)){ //check whether thread exist

                ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                if(messageNumber > 0 && messageNumber <= targetThread.size()){  //check whether messageNumber is valid

                    if(userInfo.userName.equals(targetThread.getAuthorAtLine(messageNumber))){ //check whether user has the right to delete
                        targetThread.deleteLine(messageNumber);
                        System.out.println("Messgae has been deleted");
                        outToClient.writeBytes("The message has been deleted\n");
                    }else{
                        System.out.println("Message cannot be deleted");
                        outToClient.writeBytes(userInfo.userName + " has no right to delete\n");
                    }

                }else{

                    System.out.println("Message number invalid");
                    outToClient.writeBytes("Message number invalid\n");

                } 

            }else{

                System.out.println("Thread " + threadTitle + " does not exist");
                outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");

            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(Exception e){
            
            System.out.println("DLT_handler crashes");

        }
    }

    // handle RDT command from the client
    public static void RDT_handler(String[] operation, DataOutputStream outToClient){
        try{

            String threadTitle = operation[1];
            if(activeThreadsMap.containsKey(threadTitle)){ //check whether thread exists

                ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                if(targetThread.size()==0){ //only contains header(thread creator)
                    System.out.println("Thread " + threadTitle + " is empty");
                    outToClient.writeBytes("Thread " + threadTitle + " is empty\n");
                }else{
                    for(int i=0; i<targetThread.size(); i+=1){ // header does not display to client
                        outToClient.writeBytes(targetThread.getLineContent(i+1));
                    }
                    System.out.println("Thread " + threadTitle + " read");
                }

            }else{

                outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");
                System.out.println("Incorrect thread specified");

            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(Exception e){

            System.out.println("RDT_handler crashes");

        }   
    }

    // handle EDT command from the client
    public static void EDT_handler(String[] operation, UserInformation userInfo, DataOutputStream outToClient){
        try{

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
                        outToClient.writeBytes("The message has been edited\n");
                        System.out.println("Message has been edited");
                    }else{
                        outToClient.writeBytes(userInfo.userName + " has no right to edit\n");
                        System.out.println("Message cannot be edited");
                    }

                }else{
                    System.out.println("Message number invalid");
                    outToClient.writeBytes("Message number invalid\n");
                }

            }else{

                outToClient.writeBytes("Thread " + threadTitle + " does not exist\n");

            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(Exception e){

            System.out.println("EDT_handler crashes");

        }
    }

    // handle RMV command from the client
    public static void RMV_handler(String[] operation, UserInformation userInfo, DataOutputStream outToClient){
        try{

            String threadTitle = operation[1];
            if(activeThreadsMap.containsKey(threadTitle)){ // check whether thread exist

                ThreadObj targetThread = activeThreadsMap.get(threadTitle);
                if(userInfo.userName.equals(targetThread.threadCreator)){ // check whether user can remove thread
                    targetThread.removeThreadFile();    // remove corresponding files of the thread
                    activeThreadsMap.remove(threadTitle); //remove the record from the map
                    outToClient.writeBytes("Thread " + threadTitle + " removed\n");
                    System.out.println("Thread " + threadTitle + " removed");
                }else{
                    outToClient.writeBytes("The thread was created by another user and cannot be removed\n");
                    System.out.println("Thread " + threadTitle + " cannot be removed");
                }

            }else{

                System.out.println("Thread " + threadTitle + " does not exist");
                outToClient.writeBytes("Thread " + threadTitle + " does not exist\n"); 

            }
            outToClient.writeBytes("\n");

        }catch(Exception e){

            System.out.println("RMV_hanlder crashes");

        }
        
    }

    // handle SHT command from the client
    public static boolean SHT_handler(String[] operation, DataOutputStream outToClient){
        try{

            if(operation[1].equals(adminPassword)){ // check password correct
                // before closing all sockets, we need to delete all active threads and associated files
                Iterator<String> itr = activeThreadsMap.keySet().iterator();
                while(itr.hasNext()){
                    String threadTitle = itr.next();
                    ThreadObj curr = activeThreadsMap.get(threadTitle);
                    curr.removeThreadFile(); 
                }

                // close client socket one by one 
                // since SHT will terminate this program directly, maybe this is optional?
                /*
                Iterator<Server> itr2 = activeClientsSet.iterator();
                while(itr2.hasNext()){
                    Server curr = itr2.next();
                    curr.connectionSocket.close();
                }
                */

                // close welcome socket
                welcomeSocket.close();
                return true;
            }else{
                System.out.println("Incorrect password");
                outToClient.writeBytes("Incorrect password\n");
                outToClient.writeBytes("\n");
                return false;
            }

        }catch(Exception e){
            System.out.println("SHT_handler crashes");
            return true;
        }

    }

    // handle XIT command from the client
    // since when this command is called the command handler must finish, so always return true
    public static boolean XIT_handler(UserInformation userInfo, DataOutputStream outToClient){
        try{

            syncLock.lock();
            activeClientNum -= 1;
            syncLock.unlock();
            System.out.println(userInfo.userName + " exit");
            if(activeClientNum == 0){
                System.out.println("Waiting for clients");
            }
            outToClient.writeBytes("Goodbye\n");
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end
            
        }catch(IOException e){

            System.out.println("XIT_handler crashes");

        }
        return true;
    }

    public static void invalidCommand_handler(String command, DataOutputStream outToClient){
        try{

            if(!commandsSet.contains(command)){ // send usage guide only if it is in the commands set
                System.out.println("Invalid command");
                outToClient.writeBytes("Invalid comand\n");
            }else{
                System.out.println("Wrong use of " + command);
                if(command.equals("CRT")){
    
                    outToClient.writeBytes("Usage: CRT <threadTitle>\n");
    
                }else if(command.equals("LST")){
    
                    outToClient.writeBytes("Usage: LST\n");
    
                }else if(command.equals("MSG")){
    
                    outToClient.writeBytes("Usage: MSG <threadTitle> <message>\n");
    
                }else if(command.equals("DLT")){
    
                    outToClient.writeBytes("Usage: DLT <threadTitle> <messageNumber>\n");
    
                }else if(command.equals("RDT")){
    
                    outToClient.writeBytes("Usage: RDT <threadTitle>\n");
    
                }else if(command.equals("EDT")){
                    
                    outToClient.writeBytes("Usage: EDT <threadTitle> <messageNumber> <message>\n");
    
                }else if(command.equals("UPD")){
    
                    outToClient.writeBytes("Usage: UPD <threadTitle> <fileName>\n");
    
                }else if(command.equals("DWN")){
    
                    outToClient.writeBytes("Usage: DWN <threadTitle> <fileName>\n");
    
                }else if(command.equals("RMV")){
    
                    outToClient.writeBytes("Usage: RMV <threadTitle>\n");
    
                }else if(command.equals("SHT")){
    
                    outToClient.writeBytes("Usage: SHT <adminPassword>\n");
    
                }else if(command.equals("XIT")){
                    
                    outToClient.writeBytes("Usage: XIT\n");
    
                }
            }
            outToClient.writeBytes("\n"); //it tells multiple lines writing is end

        }catch(IOException e){

            System.out.println("invalid command handler crashes");

        }
    }

}