import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadObj {

    public class LineObj{
        public String author, message;
        public LineObj(String author, String message){
            this.author = author;
            this.message = message;
        }      
    }

    public List<LineObj> lines = new LinkedList<>(); // line number not included
    public String threadTitle;
    public String threadCreator;
    private static ReentrantLock syncLock = new ReentrantLock();

    public ThreadObj(String threadTitle, String threadCreator){
        this.threadTitle = threadTitle;
        this.threadCreator = threadCreator;
        loadThreadFile();
        writeThreadFile();
    }    

    // apped new message sent from the client to the thread
    public void appendToThread(String author, String message){
        LineObj newLine = new LineObj(author, message);
        lines.add(newLine);
        writeThreadFile(); // can be improved by implementing a new method to append new message to file
    }

    // load content in .txt file to thread, if it exists
    public void loadThreadFile(){
        try{
            File f = new File(this.threadTitle + ".txt");
            if(f.exists()){
                // if it already exists then we need to load existing content
                Scanner rdr = new Scanner(f);
                rdr.nextLine();
                while(rdr.hasNextLine()){
                    String s = rdr.nextLine();
                    String[] ss = s.split(" ");
                    String author = ss[1].substring(0, ss[1].length()-1); // remove ":"
                    String message = ss[2]; //concatenate message into one string
                    for(int i=3; i<ss.length ;i+=1){
                        message += " " + ss[i];
                    }
                    LineObj newLineObj = new LineObj(author, message);
                    lines.add(newLineObj);
                }
                rdr.close();
            }
        }catch(Exception e){
            System.out.println("load thread file crashes");
            System.exit(1);
        }
    }

    // hardcode the thread information to the .txt file, call it when a thread exist?
    public void writeThreadFile(){
        try{
            syncLock.lock();
            FileWriter fw = new FileWriter(threadTitle + ".txt");
            fw.write(threadCreator + "\n"); // first line of the thread file is its creator
            int lineNum = 1;
            for(LineObj line: lines){
                fw.write(Integer.toString(lineNum) + " " + line.author + ": " + line.message + "\n");
                lineNum += 1;
            }
            fw.close();
            syncLock.unlock();
        }catch(Exception e){
            System.out.println("write thread file crashes");
            System.exit(1);
        }
    }
}
