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
    private int totalMessageNum = 0;
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
        totalMessageNum += 1;
        writeThreadFile(); // can be improved by implementing a new method to append new message to file
    }

    // append uploaded file info to the lines, called by UPD_handler
    public void appendUploadedFileToThread(String author,String fileName){
        LineObj newLine = new LineObj(author, fileName, false);
        lines.add(newLine);
        attachedFilesSet.add(threadTitle + '-' + fileName);
        writeThreadFile();
    }

    // delete a specific line, only messsage line is considered
    public void deleteLine(int messageNumber){
        int index = messageNumToIndex(messageNumber);
        lines.remove(index); // the index of number lines is 1 less than the displayed messagenumber
        totalMessageNum -= 1;
        writeThreadFile();
    }

    // edit a specific line, only message line is considered
    public void editLine(int messageNumber, String newMessage){
        int index = messageNumToIndex(messageNumber);
        lines.get(index).message = newMessage;
        writeThreadFile();
    }

    // hardcode the thread information to the .txt file, call it when a thread exist?
    public void writeThreadFile(){
        try{
            syncLock.lock(); //we don't want this section get interrupted
            FileWriter fw = new FileWriter(threadTitle + ".txt");
            fw.write(threadCreator + "\n"); // first line of the thread file is its creator
            for(int i=0; i<lines.size(); i+=1){
                fw.write(getLineContent(i));
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
    // for each line, it can be either a message or a UPD info
    public String getLineContent(int index){
        if(lines.get(index).isMessage){
            int messageNumber = indexToMessageNum(index);
            return Integer.toString(messageNumber) + " " + lines.get(index).author + ": " + lines.get(index).message + "\n";
        }else{
            return lines.get(index).author + " uploaded " + lines.get(index).fileName + "\n";
        }
    }

    // return the author of a specific line, only message line is considered
    public String getAuthorAtLine(int messageNumber){
        return lines.get(messageNumToIndex(messageNumber)).author;
    }

    // map the corresponding index in lines of a message number
    public int messageNumToIndex(int messageNumber){ 
        int n = 1;
        int index = 0;
        while(index<lines.size() && n != messageNumber){
            if(lines.get(index).isMessage){
                n += 1;
            }
            index += 1;
        }
        return index;
    }

    // map the corresponding message num of a index
    public int indexToMessageNum(int index){
        int i = 0;
        int n = 1;
        while(i<index){
            if(lines.get(i).isMessage){
                n += 1;
            }
            i += 1;
        }
        return n;
    }

    // return the total number of lines of the thread (not include the header)
    public int size(){
        return lines.size();
    }

    // total number of message line
    public int totalMessageNum(){
        return totalMessageNum;
    }
}
