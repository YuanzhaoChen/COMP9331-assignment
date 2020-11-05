import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadObj {

    private class LineObj{
        public String author, message, fileName;
        boolean isMessage; //if it is not a message then it is uploaded file's info
        
        // use this constructor when posting message or adding uploaded files's info
        public LineObj(String author, String s, boolean isMessage){
            this.author = author;
            if(isMessage){
                this.message = s;
            }else{
                this.fileName = s;
            }
            this.isMessage = isMessage;
        }
    }

    private List<LineObj> lines = new LinkedList<>(); // message number not included
    private Set<String> attachedFilesSet = new HashSet<>(); // record file uploaded to this thread when UPD is called
    public String threadTitle;
    public String threadCreator;
    private static ReentrantLock syncLock = new ReentrantLock();

    public ThreadObj(String threadTitle, String threadCreator){
        this.threadTitle = threadTitle;
        this.threadCreator = threadCreator;
        writeThreadFile();
    }    

    // apped new message sent from the client to the thread
    public void appendToThread(String author, String message){
        LineObj newLine = new LineObj(author, message, true);
        lines.add(newLine);
        writeThreadFile(); // can be improved by implementing a new method to append new message to file
    }

    // delete a specific line
    public void deleteLine(int messageNumber){
        lines.remove(messageNumber-1); // the index of number lines is 1 less than the displayed messagenumber
        writeThreadFile();
    }

    // edit a specific line
    public void editLine(int messageNumber, String newMessage){
        lines.get(messageNumber-1).message = newMessage;
    }

    // hardcode the thread information to the .txt file, call it when a thread exist?
    public void writeThreadFile(){
        try{
            syncLock.lock(); //we don't want this section get interrupted
            FileWriter fw = new FileWriter(threadTitle + ".txt");
            fw.write(threadCreator + "\n"); // first line of the thread file is its creator
            for(int i=0; i<lines.size(); i+=1){
                fw.write(getLineContent(i+1));
            }
            fw.close();
            syncLock.unlock();
        }catch(Exception e){
            System.out.println("write thread file crashes");
            System.exit(1);
        }
    }

    // remove uploaded files and threadTitle.txt correspond to this thread
    public void removeThreadFile(){
        Iterator<String> itr = attachedFilesSet.iterator();
        while(itr.hasNext()){
            String attachedFileName = itr.next();
            File f = new File(attachedFileName);
            f.delete();
        }
        String threadFileName = threadTitle + ".txt";
        File f = new File(threadFileName);
        f.delete();
    }

    // this is the actual message content that will write to .txt file
    public String getLineContent(int messageNumber){
        return Integer.toString(messageNumber) + " " + lines.get(messageNumber-1).author + ": " + lines.get(messageNumber-1).message + "\n";
    }

    // return the author of a specific line
    public String getAuthorAtLine(int messageNumber){
        return lines.get(messageNumber-1).author; // the index of number lines is 1 less than the displayed messagenumber
    }

    // return the total number of lines of the thread (not include the header)
    public int size(){
        return lines.size();
    }
}
