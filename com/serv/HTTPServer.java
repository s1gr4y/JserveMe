package com.serv;

import com.serv.FNode;
import java.io.*;
import java.net.ServerSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;	//https
import java.net.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HTTPServer implements Runnable { 
	static final File ROOT = new File(".");
	static final File WEB_ROOT = new File("./pages");
	static final File DATA_ROOT = new File("./data");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	static final int PORT = 3000;	//3000 //443
	public int fileCount = 0;
	public int counter = 0;
	static FNode fileList = null;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket Class
	//public SSLSocket connect;	//for ssl: SSLSocket
	private Socket connect;
	
	public HTTPServer(Socket c) {	//Socket c	//SSLSocket
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);	//SSLServerSocket(PORT);
			/*
			if (System.getProperty("javax.net.ssl.keyStore") == null || System.getProperty("javax.net.ssl.keyStorePassword") == null) {
				// set keystore store location
				System.setProperty("javax.net.ssl.keyStore", "server.jks");
				System.setProperty("javax.net.ssl.keyStorePassword", "password");
			}
			SSLServerSocketFactory sslfac = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverConnect = (SSLServerSocket) sslfac.createServerSocket(PORT);
			*/
			System.out.println("Server started.\nListening for connections on port: " + PORT);
			
			//listen until user halts server execution
			while (true) {
				HTTPServer myServer = new HTTPServer(serverConnect.accept());	//(SSLSocket)
				//HTTPServer myServer = new HTTPServer((SSLSocket) serverConnect.accept());	//(SSLSocket)
				
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
					
					SocketAddress socketAddress = myServer.connect.getRemoteSocketAddress();
					if (socketAddress instanceof InetSocketAddress) {
						InetAddress inetAddress = ((InetSocketAddress)socketAddress).getAddress();
						if (inetAddress instanceof Inet4Address) {
							System.out.println("IPv4: " + inetAddress);
						} else if (inetAddress instanceof Inet6Address) {
							System.out.println("IPv6: " + inetAddress);
						} else {
							System.err.println("Not an IP address.");
						}
					} else {
						System.err.println("Not an internet protocol socket.");
					}
				}
				
				// create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} /*catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}*/
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; PrintWriter jsonOut = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		InputStream inputStream = null;
		OutputStream outputStream = null;
		//InputStream is1 = null;
		
		try {
			inputStream = connect.getInputStream();
			outputStream = connect.getOutputStream();
			connect.setSoTimeout(0);	//inf timeout. A 10 sec timeout on reads was moved bc it ends if user merely connects and does nothing else, hence it is moved to savefile:
			//read characters from the client via input stream on the socket (changed in favor of scanner)
			//in = new BufferedReader(new InputStreamReader(inputStream));
			//get character output stream to client (for headers)
			out = new PrintWriter(outputStream);
			// get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(outputStream);
			//json out
			jsonOut = new PrintWriter(outputStream);
			
			//copying stream...
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			/* another way to copy to baos.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			inputStream.transferTo(baos);
			System.out.println("still reading?");
			//copied stream:
			is1 = new ByteArrayInputStream(baos.toByteArray());
			*/
			
			//get header and body reading all.
			String s = read(inputStream, baos);	//convert inp stream to baos
			Scanner scanner = new Scanner(s);
			//System.out.println("got: " + s);
			
			
			//get first line of the request from the client
			String input = scanner.nextLine();
			//we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			System.out.println("Input was: " + input);
			String method = parse.nextToken().toUpperCase();
			System.out.println("method was: " + method);
			// we get file requested
			fileRequested = parse.nextToken();
			System.out.println("req/file was: " + fileRequested);
			
			
			if (!method.equals("POST") && !method.equals("GET") && !method.equals("HEAD") && !method.equals("DELETE") && !method.equals("PUT")) {
				if (verbose) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				//send HTTP Headers with data to client
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: JserveMe : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();
				//file send
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
			} else {
				// GET or HEAD method
				//System.out.println(fileRequested);
				if (fileRequested.equals("/reqval")) {	//sudo xhr request and sent json response.
					System.out.println(fileRequested);
					if (method.equals("GET")) { // GET method so we return content
						//send HTTP Headers
						writeBaseHeader(out);
						
						//send json.
						jsonOut.println("{\"name\":\"xyz\",\"age\":\"20\"}");
						jsonOut.println();
						jsonOut.flush();
					}
				} else if (fileRequested.startsWith("/getfilelist:")) {
					if (method.equals("POST")) { // GET method so we return content
						//send HTTP Headers
						writeBaseHeader(out);
						
						String fod = fileRequested.substring(13, fileRequested.length());
						System.out.println("reading folder: " + fod);
						this.fileCount = 0;
						this.fileList = ReadFilesInDir(fod);	//this builds the n-ary tree file list recursively. it fod is always '.'
						this.counter = 0;
						//printFNode(this.fileList);
						String JsonParsedTree = "";
						JsonParsedTree += FNodetoJson(this.fileList);
						//System.out.println(JsonParsedTree);
						
						jsonOut.println(JsonParsedTree);
						jsonOut.println();
						jsonOut.flush();
					}
				} else if (fileRequested.contains("/getfile:")) {	//if this matches, it sends any file via path
					System.out.println("in req for file.....");
					String f = fileRequested.substring(9, fileRequested.length());
					
					String dir = ROOT + "/data";
					File file = new File(dir, f);
					long fileLength = (long) file.length();
					//String content = getContentType(fileRequested);
					if (method.equals("POST") || method.equals("GET")) { // GET method so we return content
						//send HTTP Headers
						writeBaseHeader(out);
						OutputStream outputTmp = new FileOutputStream(file);
						outputTmp.write("Hello World".getBytes());
						outputTmp.close();
						///*
						
						
						//send file.
						long start = fileLength;	//avoid overflow, breaks up file in chunks of ~1GB
						FileInputStream fileIn = null;
						try {
							fileIn = new FileInputStream(file);
							while (start >= 0) {
								//System.out.println("start is now: " + start);
								byte[] fileData = null;
								if (start >= Integer.MAX_VALUE/2) {
									fileData = new byte[Integer.MAX_VALUE/2];
								} else {
									fileData = new byte[(int)start];
								}
								fileIn.read(fileData);
								start -= Integer.MAX_VALUE/2;
								dataOut.write(fileData);
								//dataOut.flush();
							}
						} finally {
							if (fileIn != null) {
								fileIn.close();
							}
						}

						//send file.
						//readFileData(file, fileLength);
						//dataOut.write(fileData);
						dataOut.flush();
						//*/
					}
				} else if (fileRequested.contains("/getfileindex:")) {	//if this matches, it sends the req file for download
					String f = fileRequested.substring(14, fileRequested.length());
					int searchIndex = Integer.parseInt(f);
					String path = searchFNodes(this.fileList, searchIndex);	//also might consider having a list of FNodes alongside the tree for quick indexing.
					//System.out.println("here " + path);
					
					//if null throw no file err.
					if (path == null) {
						System.out.println("nulled");
						throw new FileNotFoundException();
					}
					File file = new File(path);
					long fileLength = (long) file.length();	//must be big so overflow is avoided
					if (method.equals("POST") || method.equals("GET")) { //Post/get method so we return content
					
						if (file.isDirectory()) {
							throw new FileNotFoundException();	//forces 404 on client side.
						}
						
						//System.out.println("if folder, readFileData fails");
						//byte[] fileData = readFileData(file, fileLength);
						//send HTTP Headers
						writeBaseHeader(out);
						
						//send file.
						long start = fileLength;	//avoid overflow, breaks up file in chunks of ~1GB
						FileInputStream fileIn = null;
						try {
							fileIn = new FileInputStream(file);
							while (start >= 0) {
								//System.out.println("start is now: " + start);
								byte[] fileData = null;
								if (start >= Integer.MAX_VALUE/2) {
									fileData = new byte[Integer.MAX_VALUE/2];
								} else {
									fileData = new byte[(int)start];
								}
								fileIn.read(fileData);
								start -= Integer.MAX_VALUE/2;
								dataOut.write(fileData);
								//dataOut.flush();
							}
						} finally {
							if (fileIn != null) {
								fileIn.close();
							}
						}

						
						//send file.
						//readFileData(file, fileLength);
						//dataOut.write(fileData);
						dataOut.flush();
						//*/
					}
				} else if (fileRequested.contains("/savefile:")) {
					connect.setSoTimeout(10000);		//set timeout of 10s here since we don't have a buffer to read from but a stream so we don't have a dead thread.
					String f = fileRequested.substring(10, fileRequested.length());
					int searchIndex = Integer.parseInt(f);	//inp check? might not matter but might consider.
					String path = searchFNodes(this.fileList, searchIndex);
					if (path == null) {
						System.out.println("nulled");
						throw new FileNotFoundException();
					}
					File file = new File(path);
					if (!file.isDirectory()) {
						throw new FileNotFoundException();	//not correct exception but close enough.
					}
					long fileLength = (long) file.length();
					//String content = getContentType(fileRequested);
					if (method.equals("POST") || method.equals("PUT")) { //get method doesn't apply since we send data
						readBodyToFile(baos, file);	//pass folder where to save it
						//send HTTP Headers
						writeBaseHeader(out);
						
						//nothing to send back.
						//dataOut.write(fileData, 0, fileLength);
						//dataOut.flush();
					}
				} else if (fileRequested.contains("/deletefileindex:")) {	//delete file
					String f = fileRequested.substring(17, fileRequested.length());
					int searchIndex = Integer.parseInt(f);
					String path = searchFNodes(this.fileList, searchIndex);	//also might consider having a list of FNodes alongside the tree for quick indexing.
					//System.out.println("here " + path);
					
					//if null throw no file err.
					if (path == null || searchIndex == 0) {	//can't delete the root data folder
						System.out.println("nulled");
						throw new FileNotFoundException();
					}
					
					File file = new File(path);
					boolean res = false;
					if (file.isDirectory()) {
						res = deleteFolder(file);
						file.delete();
					} else {
						res = deleteFile(file);
					}
					
					if (res) {
						out.println("HTTP/1.1 204 No Content");
						out.println("Server: JserveMe : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
					} else {	//couldn't delete for whatever reason so err code.
						out.println("HTTP/1.1 500 Internal Server Error");
						out.println("Server: JserveMe : 1.0");
						out.println("Date: " + new Date());
						out.println();
						out.flush();
					}
					
				} else {
					if (fileRequested.endsWith("/")) {
						fileRequested += DEFAULT_FILE;
					} else {
						fileRequested = fileRequested.substring(1, fileRequested.length());
					}
					try {
						File file = new File(WEB_ROOT, fileRequested);
						int fileLength = (int) file.length();
						String content = getContentType(fileRequested);
						
						if (method.equals("GET")) { // GET method so we return content
							byte[] fileData = readFileData(file, fileLength);
							
							//send HTTP Headers
							out.println("HTTP/1.1 200 OK");
							out.println("Server: JserveMe : 1.0");
							out.println("Date: " + new Date());
							out.println("Content-type: " + content);
							out.println("Content-length: " + fileLength);
							out.println(); 	//newline between headers and content
							out.flush(); 	//flush character output stream buffer
							
							dataOut.write(fileData, 0, fileLength);
							dataOut.flush();
						}
						
						if (verbose) {
							System.out.println("File " + fileRequested + " of type " + content + " returned");
						}
					} catch (FileNotFoundException fnfe) {
						try {
							fileNotFound(out, dataOut, fileRequested);
						} catch (IOException ioe) {
							System.err.println("Error with file not found exception : " + ioe.getMessage());
						}
					}
				}
			}
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
		} catch (SocketTimeoutException s) {
			System.err.println("Socket timed out for more than 10 seconds!");
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				//in.close();
				//is1.close();
				out.close();
				jsonOut.close();
				dataOut.close();
				connect.close(); //close socket connection
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
	
	//9/4 finally figured out that the inputstream has not finished, need to keep a reading from inputstream again until completely done, set timeout in case they lose internet/etc.
	private void readBodyToFile(ByteArrayOutputStream baos, File folder) throws IOException {	//ByteArrayInputStream baos
		boolean breakpoint = false;
		String body = "";
		
		InputStream is = new ByteArrayInputStream(baos.toByteArray()); //new ByteArrayInputStream(strBlk.getBytes());
		StringBuilder result = new StringBuilder();
		StringBuilder result2 = new StringBuilder();
		long index1 = 0;
		boolean newline = false;
		String fileName = "tmp";
		String fileType = "";
		int lengthBody = 0;
		
		while (is.available() > 0) {	//read one char at a time to check for linebreak of header and body
			byte[] b = new byte[1];
			//b = (byte) is.read();
			int didRead = is.read(b, 0, 1);
			if (didRead == 0) {
				continue;
			}
			result.append((char) b[0]);
			result2.append((char) b[0]);
			
			if (((char) b[0]) == '\n' && result.toString().isBlank()) {	//checks if we broke from header to body. result.toString().isBlank()	result.toString().contains("------WebKitFormBoundary")
				System.out.println("bp point!!!!!!");
				result.setLength(0);
				newline = true;
				System.out.println("\n\n\n\n\nread until: \n" + result2 + "\n\n\n\n\n");
				result2.setLength(0);
				break;
			} else if ((char) b[0] == '\n') {
				System.out.println("checking " + result.toString());
				if (result.toString().contains("File-Name:")) {
					int indexSpace = result.toString().indexOf(' ');
					String str = result.toString();
					fileName = str.substring(indexSpace+1, result.length()-2);	//-2 for newline we appended...
				} else if (result.toString().contains("Content-Length:")) {
					int indexSpace = result.toString().indexOf(' ');
					String str = result.toString();
					lengthBody = Integer.parseInt(str.substring(indexSpace+1, result.length()-2));	//-2 for newline we appended...
				}
				result.setLength(0);
			}
			
		}

		System.out.println("name: \"" + fileName + "\"");
		int lastdot = fileName.lastIndexOf('.');
		//String name = fileName.substring(0, lastdot);
		//fileType = fileName.substring(lastdot, fileName.length());
		//System.out.println(fileType);
		//File file = new File(DATA_ROOT, fileName);
		File file = new File(folder, fileName);	//DATA_ROOT.getAbsolutePath() + '/' + name + fileType);
		//System.out.println("path is: " + file.getAbsolutePath());
        try {
			boolean istrue = file.createNewFile();
		} catch(Exception e) {
			System.out.println(e);
		}

		long totalRead = 0;
		try (OutputStream output = new FileOutputStream(file, false)) {
			byte[] data = new byte[65536];	//2^14==16484, 2^16==65536
			int nRead = 0;
			while ((nRead = is.read(data, 0, data.length)) != -1) {	//for future, might need to change so we don't get timeout error since it may not throw -1 since no eof sent
				output.write(data, 0, nRead);
				totalRead += nRead;
				/*
				if (totalRead >= lengthBody) {	//solves prev comment
					break;
				}
				*/
			}
			if (totalRead < lengthBody) {	//might still have data being written to it, read rest.
				System.out.println("still have some stuff to read || read " + totalRead + " out of " + lengthBody + ".");
				while (totalRead < lengthBody) {
					while (((nRead = is.read(data, 0, data.length)) != -1)) {	//|| totalRead < lengthBody	//irrelevant to add as it is dictated by is.read...
						output.write(data, 0, nRead);
						totalRead += nRead;
						if (verbose) {
							System.out.println("read " +  nRead + " and total read is now " + totalRead);
						}
						if ((totalRead >= lengthBody)) {	//required as I don't send a eof or end of stream "is.read() == -1", ergo it waits forever regardless of while loop. (connect.setSoTimeout helps by breaking if 10 sec of nothing sent)
							if (verbose) {
								System.out.println("read all " + totalRead + " bytes");
							}
							break;	//done reading.
						}
					}
					//System.out.println("if false we done. " + (totalRead < lengthBody));
					if ((totalRead >= lengthBody)) {
						output.close();
						is.close();
						break;
					}
					is = connect.getInputStream();
				}
			}
        } catch(Exception e) {
			System.out.println(e);
			return;	//unsure what else to do, should just fail do nothing else.
		}
	}
	
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html")) {
			return "text/html";
		}
		if (fileRequested.endsWith(".css")) {
			return "text/css";
		}
		if (fileRequested.endsWith(".jpg") || fileRequested.endsWith(".gif") || fileRequested.endsWith(".png")) {
			return "image/pdf";
		}
		return "text/plain";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: JserveMe : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println();	//blank line between header and body, important for browser to parse
		out.flush();	//flush buffer to output stream
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	
	private void writeBaseHeader(PrintWriter out) throws IOException {	//does not finish header, simply reduces amount of copy pastes
		out.println("HTTP/1.1 200 OK");
		out.println("Server: JserveMe : 1.0");
		out.println("Date: " + new Date());
		out.println();		//blank line between header and body, important for browser to parse
		out.flush();		//flush buffer to the output stream
	}
	
	private FNode ReadFilesInDir(String path) {	//set fileCount to 0 on first run to index all files
		File folder = null;
		if (path.equals(".") || path.equals("/")) {
			folder = DATA_ROOT;
		} else {
			folder = new File(path);
		}
		boolean isDir = false;
		if (folder.isDirectory()) {
			isDir = true;
		}
		//System.out.println(path + " is " + isDir);
		FNode root = new FNode(folder.getName(), this.fileCount++, folder.getAbsolutePath(), isDir);
		//List<String> FileList = new ArrayList<String>();
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				//System.out.println("File " + listOfFiles[i].getName());
				//FileList.add(listOfFiles[i].getName());
				root.appendStr(listOfFiles[i].getName(), this.fileCount++, listOfFiles[i].getAbsolutePath(), false);
			} else if (listOfFiles[i].isDirectory()) {
				//System.out.println("Directory " + listOfFiles[i].getName());
				//System.out.println("Directory path: " + listOfFiles[i].getAbsolutePath());
				//this case we need to recurse call since we need to go into file.
				root.appendFNode(ReadFilesInDir(listOfFiles[i].getAbsolutePath()));
			}
		}
		return root;
	}
	
	private void printFNode(FNode root) {
		System.out.println(root.name + " is it a dir? " + root.isDir);
		for (int i = 0; i < root.children.size(); i++) {
			printFNode(root.children.get(i));
		}
	}
	
	private String searchFNodes(FNode root, int index) {
		//System.out.println("searching in " + root.path + " it has index " + root.index);
		//System.out.println("want the index " + index);
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
	
	private String FNodetoJson(FNode root) {	//parse n-ary tree to a json format.
		String listed = "";
		listed += "{\"name\":\"" + root.name + "\"," + "\"isDir\":\"" + root.isDir + "\"," + "\"val\":\"" + (this.counter++) + "\"," + "\"list\":[";
		if (root.children.size() != 0) {
			for (int i = 0; i < root.children.size()-1; i++) {
				FNode Node = root.children.get(i);
				if (Node.hasChildren) {
					listed += FNodetoJson(Node) + ", ";
				} else {
					listed += "{\"name\":" + "\"" + Node.name + "\"," + "\"isDir\":\"" + Node.isDir + "\"," + "\"val\":\"" + (this.counter++) + "\"" + "},";
				}
			}
			//last one
			FNode lastNode = root.children.get(root.children.size()-1);
			if (lastNode.hasChildren) {
				listed += FNodetoJson(lastNode);
			} else {
				listed += "{\"name\":" + "\"" + lastNode.name + "\"," + "\"isDir\":\"" + lastNode.isDir + "\"," + "\"val\":\"" + (this.counter++) + "\"}";
			}
			listed += "]}";
		}
		return listed;
	}
	
	private String read(InputStream inputStream, ByteArrayOutputStream baos) throws IOException {
		StringBuilder result = new StringBuilder();
		do {
			byte[] b = new byte[1];
			int didRead = inputStream.read(b, 0, 1);
			result.append((char) b[0]);
			baos.write(b, 0, 1);
		} while (inputStream.available() > 0);
		return result.toString();
	}
	
	private boolean deleteFile(File file) {
		if (file.delete()) { 
		  System.out.println("Deleted file: " + file.getName());
		  return true;
		} else {
		  System.out.println("Failed to delete file.");
		  return false;
		} 
		
	}
	
	private boolean deleteFolder(File folder) {
		for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                deleteFolder(file);
            }
            if (!file.delete()) {
				return false;
			}
        }
		return true;
	}
	
}
