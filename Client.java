import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.Scanner;

public class Client{
    public static void main(String[] args) throws Exception {

        System.out.println("Executing client...");

        if(args.length != 2){
            System.out.println("Usage: java Client <server_ip> > <server_port>");
            return;
        }

        InetAddress server_ip = InetAddress.getByName(args[0]);
        int server_port = Integer.parseInt(args[1]);
        Socket clientSocket = new Socket(server_ip, server_port);
        
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        while(true){
            
            // enter user name
            System.out.print("Enter username: ");
            BufferedReader inputUserName = new BufferedReader(new InputStreamReader(System.in));
            String userName = inputUserName.readLine();
            outToServer.writeBytes(userName + "\n"); //must terminate with '\n' otherwise server will continue reading
            outToServer.flush();
    
            // enter password
            BufferedReader inputPassword = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter password: ");
            String password = inputPassword.readLine();
            outToServer.writeBytes(password + "\n");
            outToServer.flush();

            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String feedback = inFromServer.readLine();
            
            if(feedback.equals("password correct")){
                break;
            }else{
                System.out.println("Invalid password.");
            }

            //outToServer.

        }
        /** 
        // create read stream and receive from server
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String sentenceFromServer;
        sentenceFromServer = inFromServer.readLine();
        System.out.println(sentenceFromServer);


        String sentence;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        sentence = inFromUser.readLine();

        System.out.println("writing to server");
        // write to server
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        outToServer.writeBytes(sentence + '\n');
        */
        clientSocket.close();
    }
}

