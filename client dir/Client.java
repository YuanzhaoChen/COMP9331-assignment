import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.Scanner;

public class Client extends Thread{
    public static void main(String[] args) throws Exception {

        System.out.println("Executing client...");

        if(args.length != 2){
            System.out.println("Usage: java Client <server_ip> > <server_port>");
            return;
        }

        // get server ip and port
        InetAddress server_ip = InetAddress.getByName(args[0]);
        int server_port = Integer.parseInt(args[1]);
        Socket clientSocket = new Socket(server_ip, server_port);
        
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        boolean authenticationComplete = false;
        while(!authenticationComplete){
            
            // enter user name
            System.out.print("Enter username: ");
            BufferedReader inputUserName = new BufferedReader(new InputStreamReader(System.in));
            String userName = inputUserName.readLine();
            outToServer.writeBytes(userName + "\n"); // must terminate with '\n' otherwise server will continue reading
            outToServer.flush();

            // listem to server to check whether this is a new user
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
                authenticationComplete = true;
            }else if(passwordFeedback.equals("user already logged in")){
                System.out.println(userName + " has already logged in");
            }else{
                System.out.println("Invalid password");
            }

        }

        System.out.println("Welcome to the forum");

        boolean operationsComplete = false;
        while(!operationsComplete){
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

            if(operation.equals("XIT")){
                break;
            }

        }
        clientSocket.close();
    }

    @Override
    public void run(){
        
    }
}

