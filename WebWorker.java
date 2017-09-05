
/**
 * Programmer: Kathleen Near
 * Course: CS371 (Software Development)
 * Date: 08/28/17
 * 
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable {
 
String serverName = "Kat's WebServer";
String streamStatus = "HTTP/1.1 200 OK\n";
String HTMLcontent = "";
boolean fileReadSuccessful = false;
boolean noFileRequested = false;
boolean Windows = false;
private Socket socket;
 
/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s) {
  socket = s;
}
 
/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run() {
  System.err.println("Handling connection...");
  try {
     InputStream  is = socket.getInputStream();
     OutputStream os = socket.getOutputStream();
     readHTTPRequest(is);
     writeHTTPHeader(os,"text/html");
     writeContent(os);
     os.flush();
     socket.close();
  } catch (Exception e) {
     System.err.println("Output error: "+e);
  }
  System.err.println("Done handling connection.");
  return;
}
 
/**
* Appends the requested file path to the root directory path, creating a complete path
* @param fullRequest is the user-initiated file request
* null is returned if there is no file request made
**/
private String toCompletePath(String fullRequest) {
 
///Parse request for file path
String requestLine[] = new String[] {" ", " ", " "};
   requestLine = fullRequest.split(" ");
String partialPath = requestLine[1];
 
//If requested path is just "/" or "", don't return any path
   if(partialPath.length() <= 1) {
    noFileRequested = true;
    return null;
   }
 
String completePath;
//If using my Windows machine, the path is different than the school Linux machines
if (Windows)
    completePath = "C:/Users/Michelle/eclipse-workspace/P1" + partialPath;
else
    completePath = "." + partialPath;
 
   return completePath;
}
 
/**
* Reads the HTTP request header and returns the request String
* @param is the InputStream object to read from
**/
private void readHTTPRequest(InputStream is) {
  BufferedReader requestReader = null;
  
  while (true) {
     try {
    requestReader = new BufferedReader(new InputStreamReader(is));
    
       while (!requestReader.ready()) Thread.sleep(1);
       String line = requestReader.readLine();
       System.err.println("Request line: ("+line+")");
        
       
/* Get the complete path from the root directory to the file
* If no file requested: display default content
*/
   String rootPath = toCompletePath(line);
   if(noFileRequested) {
    HTMLcontent = "<html><h1>Welcome to " + serverName + "</h1><body>\n" +
   "<p>You may request a file in the URL path.</p>\n" +
   "</body></html>\n";
    break;
   }//end
   
   /* Check if file exists
    * If not: change the status and exit
    * If so:  read the contents of the file
    */
   File fileRequested = new File(rootPath);
   if(!fileRequested.exists()) {
streamStatus = "HTTP/1.1 404 NOT FOUND\n";
HTMLcontent = "<html><h1>404 Error.</h1><body>\n" +
   "<p>Page not found.</p>\n" +
   "</body></html>\n";
   break;
   }
   else {
    HTMLcontent = readFile(rootPath);
    break;
   }//end
 
   
     } catch (Exception e) {
        System.err.println("Request error: " + e);
        break;
     //close BufferedReader if initialized
     }//end catch
     
  }//end while loop
}
 
 
/**
* Checks a line of a HTML file for certain tags and replaces them with the corresponding content
* @param line is one line of an HTML file
**/
private String replaceTags(String line) {
Date d = new Date();
DateFormat df = DateFormat.getDateTimeInstance();
line = line.replaceAll("<cs371date>", df.format(d));
line = line.replaceAll("<cs371server>", serverName);
 
return line;
}
 
/**
* Reads an HTML file and returns its content in a String
* @param fileName is the name of the file that was requested
**/
private String readFile(String fileName) {
//Initialize
String line = null;
String allContent = "";
BufferedReader fileReader = null;
 
while (true) {
try {
fileReader = new BufferedReader (new FileReader (fileName));
 
//Replace tags with indicated content
while((line = fileReader.readLine()) != null) {
line = replaceTags(line);
allContent += line;
}
} catch (Exception e) {
System.err.println("Read error: "+e);
break;
//Close BufferedReader if initialized
}//end catch
break;
}//end while loop
 
fileReadSuccessful = true;
return allContent;
}
 
/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception {
  Date d = new Date();
  DateFormat df = DateFormat.getDateTimeInstance();
  df.setTimeZone(TimeZone.getTimeZone("GMT"));
  os.write(streamStatus.getBytes());
  os.write("Date: ".getBytes());
  os.write((df.format(d)).getBytes());
  os.write("\n".getBytes());
  os.write("Server: Jon's very own server\n".getBytes());
  //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
  //os.write("Content-Length: 438\n".getBytes()); //num characters in HTML line
  os.write("Connection: close\n".getBytes());
  os.write("Content-Type: ".getBytes());
  os.write(contentType.getBytes());
  os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
  return;
}
 
/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception {
  os.write(HTMLcontent.getBytes());
}

} // end class
