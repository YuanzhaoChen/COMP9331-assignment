# COMP9331-assignment

Online discussion forums are widely used as a means for large groups of people to hold conversations on topics of mutual interest. A good example is the online forum used for this course. In this assignment, you will have the opportunity to implement your own version of an online discussion forum application. Your application is based on a client server model consisting of one server and multiple clients communicating either sequentially (i.e., one at a time) or concurrently. The client and server should communicate using TCP. Your application will support a range of functions that are typically found on discussion forums including authentication, creation and deletion of threads and messages, reading threads, uploading and downloading files. However, unlike typical online forums that are accessed through HTTP, you will be designing a custom application protocol.

## Usage

To launch the program, on server side:

```bash
javac Server.java
java Server server_port admin_passwd
```

and then on client side:

```
javac Client.java
java Client server_IP server_port
```

## Implemented functions

| Funcition | Command |
| ------- | ----- |
| Create thread | ```CRT <thread title>``` |
| Post message | ```MSG <thread title> <message>``` |
| Delet message | ```DLT <thread title> <message number>``` |
| Edit message | ```EDT <thread title> <message number> <message>``` |
| List thread | ```LST``` |
| Read thread | ```RDT <thread title>``` |
| Upload file|```UPD <thread title> <file name>```|
|Download file|```DWN <thread title> <file name>```|
| Remove thread | ```RMV <thread title>``` |
| Client exit | ```XIT``` |
| Shut down server | ```SHT <admin_password>``` |

