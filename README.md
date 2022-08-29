# (JserveMe) HTTP-FileHosting
A small HTTP file hosting webserver made in Java.

#How it works
Users access the site and can download the files within the list provided.\
The folder the server gets its files from to give to the user is hardcoded in the "./data" folder (users can change the code to where they would prefer).\
By default the server opens on port 3000, I will make it changeable via commandline argument soon.\
Internally, there is a n-ary tree to represent the file system, each folder/file has a value associated with it and the tree is represented as FNodes (File Nodes).\

##What's the point?
It was a utility project I thought of so I can have some of my files "on the cloud" which does not depend on other cloud services.
It is primarily useful since there are no limits for how big the file sizes can be (while many cloud filesharing services restrict sizes or need payment for such features).
Users can make their own instance and have their own data to share with others or personal use when not on their main machine.

#To Build and Run
In your preferred terminal.
1. git clone this repository.
2. In the HTTP-FileHosting directory, run ```javac com/serv/*.java```
3. Finally, run the output with: ```java -cp ./com/serv;. com.serv.HTTPServer```
With that, the server is up and running! You can access it via entering ```localhost:3000``` in your preferred browser.\
To access the server from another computer on the same or different network enter in your browser: ```IPV4:3000``` or ```[IPV6]:3000```\
Alternatively, you can have it hosted and associated with a domain name.\

#Dependencies
Credit to TheTechy's small javascript library for having collapseable lists for the UI.\
[Link](https://github.com/TheTechy/jslists)