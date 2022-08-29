package com.serv;

import com.serv.FNode;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.*;

public class HTTPServer implements Runnable { 
	static final File ROOT = new File(".");
	static final File WEB_ROOT = new File("./pages");
	static final File DATA_ROOT = new File("./data");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	static final int PORT = 3000;
	static int fileCount = 0;
	static int counter = 0;
	static FNode fileList = null;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	private Socket connect;
	
	public HTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			
			// we listen until user halts server execution
			while (true) {
				HTTPServer myServer = new HTTPServer(serverConnect.accept());
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				
				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; PrintWriter jsonOut = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		
		try {
			inputStream = connect.getInputStream(); 
			outputStream = connect.getOutputStream();
			// we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(inputStream));
			// we get character output stream to client (for headers)
			out = new PrintWriter(outputStream);
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(outputStream);
			//json out
			jsonOut = new PrintWriter(outputStream);
			
			//get header and body reading all.
			String s = read(inputStream);
			Scanner scanner = new Scanner(s);
			System.out.println("got: " + s);
			
			
			// get first line of the request from the client
			String input = scanner.nextLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			System.out.println("Input was: " + input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			System.out.println("method was: " + method);
			// we get file requested
			fileRequested = parse.nextToken().toLowerCase();
			System.out.println("file was: " + fileRequested);
			
			
			// we support only GET and HEAD methods, we check
			if (!method.equals("POST") && !method.equals("GET") && !method.equals("HEAD")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				// we send HTTP Headers with data to client
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: silen serveme : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();
				// file
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
			} else {
				// GET or HEAD method
				//System.out.println(fileRequested);
				if (fileRequested.equals("/reqval")) {	//sudo xhr request and sent json response.
					System.out.println(fileRequested);
					if (method.equals("GET")) { // GET method so we return content
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: silen serveme : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
						
						//send json.
						jsonOut.println("{\"name\":\"xyz\",\"age\":\"20\"}");
						jsonOut.println();
						jsonOut.flush();
					}
				} else if (fileRequested.startsWith("/getfilelist:")) {
					if (method.equals("POST")) { // GET method so we return content
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: silen serveme : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
						String fod = fileRequested.substring(13, fileRequested.length());
						System.out.println("reading folder: " + fod);
						HTTPServer.fileCount = 0;
						this.fileList = ReadFilesInDir(fod);
						HTTPServer.counter = 0;
						//printFNode(this.fileList);
						String JsonParsedTree = "";
						//JsonParsedTree += "{";
						JsonParsedTree += FNodetoJson(this.fileList);
						//JsonParsedTree += "}";
						System.out.println(JsonParsedTree);
						jsonOut.println(JsonParsedTree);
						jsonOut.println();
						jsonOut.flush();
					}
				} else if (fileRequested.contains("/getfile:")) {	//if this matches, it sends a pdf file to download.
					System.out.println("in req for file.....");
					String f = fileRequested.substring(9, fileRequested.length());
					System.out.println("here " + f);
					String dir = ROOT + "/data";
					File file = new File(dir, f);
					int fileLength = (int) file.length();
					//String content = getContentType(fileRequested);
					if (method.equals("POST") || method.equals("GET")) { // GET method so we return content
						byte[] fileData = readFileData(file, fileLength);
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: silen serveme : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
						
						//send file.
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
				} else if (fileRequested.contains("/getfileindex:")) {	//if this matches, it sends a pdf file to download.
					System.out.println("in req for file index!!");
					String f = fileRequested.substring(14, fileRequested.length());
					int searchIndex = Integer.parseInt(f);
					String path = searchFNodes(this.fileList, searchIndex);
					System.out.println("here " + searchIndex);
					//if null throw no file err.
					if (path == null) {
						throw new FileNotFoundException();
					}
					File file = new File(path);
					System.out.println("trying to send " + file.getName());
					System.out.println("including path " + file.getAbsolutePath());
					int fileLength = (int) file.length();
					//String content = getContentType(fileRequested);
					if (method.equals("POST") || method.equals("GET")) { // GET method so we return content
						byte[] fileData = readFileData(file, fileLength);
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: silen serveme : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
						
						//send file.
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
				} else {
					if (fileRequested.endsWith("/")) {
						fileRequested += DEFAULT_FILE;
					} else {
						fileRequested = fileRequested.substring(1, fileRequested.length());
					}
					
					File file = new File(WEB_ROOT, fileRequested);
					int fileLength = (int) file.length();
					String content = getContentType(fileRequested);
					
					if (method.equals("GET")) { // GET method so we return content
						byte[] fileData = readFileData(file, fileLength);
						
						// send HTTP Headers
						out.println("HTTP/1.1 200 OK");
						out.println("Server: silen serveme : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println(); // newline between headers and content.
						out.flush(); // flush character output stream buffer
						
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
					
					if (verbose) {
						System.out.println("File " + fileRequested + " of type " + content + " returned");
					}
				}
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				jsonOut.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
		
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html")) {
			return "text/html";
		}
		if (fileRequested.endsWith(".css")) {
			return "text/css";
		}
		return "text/plain";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: silen serveme : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	
	private FNode ReadFilesInDir(String path) {	//set fileCount to 0 on first run to index all files
		File folder = null;
		if (path.equals(".") || path.equals("/")) {
			folder = DATA_ROOT;
		} else {
			folder = new File(path);
		}
		FNode root = new FNode(folder.getName(), HTTPServer.fileCount++, folder.getAbsolutePath());
		//List<String> FileList = new ArrayList<String>();
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("File " + listOfFiles[i].getName());
				//FileList.add(listOfFiles[i].getName());
				root.appendStr(listOfFiles[i].getName(), HTTPServer.fileCount++, listOfFiles[i].getAbsolutePath());
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
				System.out.println("Directory path: " + listOfFiles[i].getAbsolutePath());
				//this case we need to recurse call since we need to go into file.
				root.appendFNode(ReadFilesInDir(listOfFiles[i].getAbsolutePath()));
			}
		}
		return root;
	}
	
	private void printFNode(FNode root) {
		System.out.println(root.name);
		for (int i = 0; i < root.children.size(); i++) {
			printFNode(root.children.get(i));
		}
	}
	
	private String searchFNodes(FNode root, int index) {
		System.out.println("searching in " + root.path + " it has index " + root.index);
		System.out.println("want the index " + index);
		if (root.index == index) {
			System.out.println("FOUND " + index + " with path of " + root.path +"/"+ root.name);
			return root.path;
		}
		for (int i = 0; i < root.children.size(); i++) {
			String tmp = searchFNodes(root.children.get(i), index);
			if(tmp != null) {
				return tmp;
			}
		}
		return null;
	}
	
	private String FNodetoJson(FNode root) {
		String listed = "";
		listed += "{\"name\":\"" + root.name + "\"," + "\"val\":\"" + (HTTPServer.counter++) + "\"," + "\"list\":[";
		if (root.children.size() != 0) {
			for (int i = 0; i < root.children.size()-1; i++) {
				FNode Node = root.children.get(i);
				if (Node.hasChildren) {
					listed += FNodetoJson(Node) + ", ";
				} else {
				listed += "{\"name\":" + "\"" + Node.name + "\"," + "\"val\":\"" + (HTTPServer.counter++) + "\"" + "},";
				}
			}
			//last one
			if (root.children.get(root.children.size()-1).hasChildren) {
				listed += FNodetoJson(root.children.get(root.children.size()-1));
			} else {
				listed += "{\"name\":" + "\"" + root.children.get(root.children.size()-1).name + "\"," + "\"val\":\"" + (HTTPServer.counter++) + "\"}";
			}
			listed += "]}";
		}
		return listed;
	}
	
	private String read(InputStream inputStream) throws IOException {
		StringBuilder result = new StringBuilder();
		do {
			result.append((char) inputStream.read());
		} while (inputStream.available() > 0);
		return result.toString();
	}
	
}
