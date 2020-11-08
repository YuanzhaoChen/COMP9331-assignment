import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.Scanner;

public class Client extends Thread{
    private Socket socket;
    private static boolean isExit = false;
    public static boolean isShutDown = false;

    public Client(Socket s){
        this.socket = s;
    }

    public static void main(String[] args) throws Exception {
        if(args.length != 2){
            System.out.println("Usage: java Client <server_ip> > <server_port>");
            return;
        }

        // get server ip and port
        InetAddress server_ip = InetAddress.getByName(args[0]);
        int server_port = Integer.parseInt(args[1]);

        // this is the socket gets feedback message from the server
        Socket clientSocket = new Socket(server_ip, server_port);
        DataOutputStream o1 =  new DataOutputStream(clientSocket.getOutputStream());
        o1.writeBytes("1\n");
        Client c = new Client(clientSocket); 
        c.start();

        // this is the socket to check connection with the server and detect shutdown
        Socket clientSocket2 = new Socket(server_ip, server_port);
        DataOutputStream o2 =  new DataOutputStream(clientSocket2.getOutputStream());
        o2.writeBytes("2\n");
        ClientConnectionChecker c2 = new ClientConnectionChecker(clientSocket2);
        c2.start();

        while(!isExit && !isShutDown){
            Thread.sleep(500); // wait for variable update
        }

        if(isShutDown){
            System.out.println("Goodbye, server shutting down.");
        }

        clientSocket.close();
        System.exit(0);
    }

    @Override
    public void run(){
        
        try{
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // begin user authentication
            while(true){  
                // enter user name
                System.out.print("Enter username: ");
                BufferedReader inputUserName = new BufferedReader(new InputStreamReader(System.in));
                String userName = inputUserName.readLine();
                outToServer.writeBytes(userName + "\n"); // must terminate with '\n' otherwise server will continue reading
                outToServer.flush();

                // listen to server to check whether this is a new user
                String userNameFeedback = inFromServer.readLine();
                if(userNameFeedback.equals("new user")){
                    System.out.print("Enter new password for "+userName+": ");
                }else if(userNameFeedback.equals("user already logged in")){
                    System.out.println(userName + " has already logged in");
                    continue;
                }else{
                    System.out.print("Enter password: ");
                }

                // enter password
                BufferedReader inputPassword = new BufferedReader(new InputStreamReader(System.in));
                String userPassword = inputPassword.readLine();
                outToServer.writeBytes(userPassword + "\n"); // must terminate with '\n' otherwise server will continue reading
                outToServer.flush();

                // read feedback from server
                String passwordFeedback = inFromServer.readLine();
                if(passwordFeedback.equals("password correct") || passwordFeedback.equals("new password set")){
                    break;
                }else if(passwordFeedback.equals("user already logged in")){
                    System.out.println(userName + " has already logged in");
                }else{
                    System.out.println("Invalid password");
                }
            }

            // user authentication finish
            System.out.println("Welcome to the forum");

            // interact with the forum
            while(true){
                // enter operation
                System.out.print("Enter one of the following commands: CRT, MSG, DLT, EDT, LST, RDT, UPD, DWN, RMV, XIT, SHT: ");
                BufferedReader inputOperation = new BufferedReader(new InputStreamReader(System.in));
                String operation = inputOperation.readLine();

                outToServer.writeBytes(operation + "\n");
                outToServer.flush();
                
                // receive feedback, feedback may contain multiple lines
                String operationFeedback;
                while((operationFeedback = inFromServer.readLine()) != null && !operationFeedback.equals("")){
                    System.out.println(operationFeedback);
                }

                if(operationFeedback == null){ // feedback is null when the server is shut down
                    isShutDown = true;
                    break;
                }

                if(operation.equals("XIT")){
                    isExit = true;
                    break;
                }
            }

        }catch(IOException e){
            System.out.println("client crash");
        }
    }
}

