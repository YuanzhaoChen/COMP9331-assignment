## COMP9331 Assignment Report

> Student Name: Yuanzhao Chen
>
> Student ID: 5041686

### Table of contents

[Implemented function](#implemented-function)

[Introduction of program design](#introduction-of-program-design)

[Design trade-offs considered and made](#design-trade-offs-considered-and-made)

[Possible improvements and extensions](#possible-improvements-and-extensions)

[Code segments borrowed from external resources](#code-segments-borrowed-from-external-resources)

---

### Implemented function

**All** functions listed in the assignment spec are implemented:

- [x] Multithreading
- [x] CRT
- [x] MSG
- [x] DLT
- [x] EDT
- [x] LST
- [x] RDT
- [x] UPD
- [x] DWN
- [x] RMV
- [x] XIT
- [x] SHT

### Introduction of program design 

<img src="/Users/jasonchen/comp9331/assignment/report/Screen Shot 2020-11-10 at 4.45.56 pm.png" style="zoom:50%;" />

Figure above depicts the basic mechanism of the design in this assignment.

Generally speaking, the server follows the pattern of "listen and reply": the child thread of the server listens to a specific client and sends corresponding reply back to the client in the format of Strings except UPD and DWN commands (it will be bytes in this case). Meanwhile, the client would send commands consistently once previous feeback is recevied and processed until it is told to exit or shut down. This kind of mechanism can handle most of the commands in the assignment spec.

For SHT, since it would shut down every active client simultaneously once it is triggered, it is important to create a independent thread (ConnectionChecker) on both the server and the client side to communicate with each other constantly so as to let the client know the server is on at the moment. If the server is on, the client connection checker will receive message "OK" from the server connection checker every 100 milliseconds, if the server is down, it will just receive null. When null is detected, the client connection checker will set the global variable "isShutdown" to true, which is checked by the main thread of the client in a infinite loop. The inifinite loop will break thereafter and the program can exit.

In order to manage thread files more efficiently, a class called ThreadObj is created who major duty is store any information related to that thread (thread creator, thread files uploaded, messages posted to the thread, etc). threadTiltle.txt is updated by a method called "writeThreadFile" inside that class. Inside threadObj, there is a nested object called LineObj which gives flexibility to store either a posted message or a file upload record. A linked list is used to store different LineObj since post in the forum is ordered. The choice of using an linked list also makes it convenient to remove or append a target.

### Design trade-offs considered and made

In this design, the method to update threadTitle.txt is to wipe out all existing content in the text file first and then re-write the file from the very begining according to the updated information stored in the ThreadObj. This is unnecessary in many scenario for example for MSG command, all we need is just append a new line of content to threadTitle.txt. But the "append text" method is not implemented due to the time constraint and the hope of making the code more readable and easier to debug.

### Possible improvements and extensions

As discussed in the previous section, a possible improvement of the design is to introduce a method whose job is to append just one single line of content to the existing text file, so that we don't have to re-write everything in the text file everytime we update.

In the assignment, the user-password credential is stored as a text file. I believe most servers in the industry store those user-password pairs in a database (e.g., sql) instead in order to have a faster reaction speed. Hence, another possible extension for this assignment is to create a database to store user names and corresponding passswords, although this is beyond the scope of this course.

### Code segments borrowed from external resources

[Read a file in Java](https://www.w3schools.com/java/java_files_read.asp)

[How to append text to an existing file in Java](https://stackoverflow.com/questions/1625234/how-to-append-text-to-an-existing-file-in-java) 

[How to check if a socket is currently closed](https://stackoverflow.com/questions/1390024/how-do-i-check-if-a-socket-is-currently-connected-in-java)

[Send multiple files in one socket](https://stackoverflow.com/questions/10367698/java-multiple-file-transfer-over-socket)

