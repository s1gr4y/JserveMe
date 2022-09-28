//var declarations
let FileTree = null;

//functions

function readTree(txtBlk, root, depth) {
	let tmpStr = root.name;
	root.name = root.name.replace("'", "\\'");
	if (root.isDir === "true") {
		txtBlk += "<li> <span onclick=\"RequestDocumentNumber("+root.val+ "," + "\'"+root.name+"\'"+")\" style=\"cursor: pointer; text-decoration: underline; white-space: nowrap;\">"	//onclick=RequestDocumentNumber("+root.val+"," + "\'"+root.name+"\'"+")>
	} else {
		txtBlk += "<li> <span onclick=\"RequestDocumentNumber("+root.val+ "," + "\'"+root.name+"\'"+")\" style=\"cursor: pointer; color:teal; text-decoration: underline; white-space: nowrap;\">"
	}
	let str = "";
	root.name = tmpStr;
	txtBlk += str + root.name + "</span>";
	//}
	//id.appendChild(node1);
	if (typeof root === 'object' && !Array.isArray(root) && root !== null && root.hasOwnProperty('list')) {
		txtBlk += "<ul>"
		for (let i = 0; i < root.list.length; i++) {
			depth += 1;
			txtBlk = readTree(txtBlk, root.list[i], depth);
			depth -= 1;
		}
		txtBlk += "</ul>"
	}
	txtBlk += "</li>";
	return txtBlk;
}

function request() {
	let xhr = new XMLHttpRequest();
	xhr.open('GET', '/reqval');
	xhr.onreadystatechange = function() {
		if (xhr.readyState == XMLHttpRequest.DONE) {
			let data = JSON.parse(xhr.responseText);
			console.log(data);
		}
	};
	xhr.send(200);
}

function sendFile(e) {
	let xhr = new XMLHttpRequest();
	let getid = document.getElementById("file");
	let f = getid.files[0];

	//listen for upload progress
	xhr.upload.onprogress = function(event) {
	let percent = Math.round(100 * event.loaded / event.total);
		console.log(`File is ${percent} uploaded.`);
	};

	//handle error
	xhr.upload.onerror = function() {
		console.log(`Error during the upload: ${xhr.status}.`);
	};

	//upload completed successfully
	xhr.onload = function() {
		console.log('Upload completed successfully.');
	};
	
	///*
	xhr.onreadystatechange = function() {
		if (xhr.readyState == XMLHttpRequest.DONE) {
			let img = document.getElementById("loadgif");
			img.style.display = "none";
			document.getElementById("file").value = "";
			let element = document.getElementById("fileBlock");
			element.innerHTML = "";
			RequestFileList(".");
		}
	}
	xhr.open("PUT", "/savefile:0");
	xhr.setRequestHeader('File-Name', f.name);
	xhr.setRequestHeader('Content-Type', f.type);
	xhr.send(f);	//weird err about "Failed to load resource: net::ERR_EMPTY_RESPONSE" but everything still works so :P
	let x = document.getElementById("loadgif");
	if (x.style.display === "none") {
		x.style.display = "block";
	} else {
		x.style.display = "none";
	}
}

function RequestDocument(fileName) {
	event.preventDefault();	//IMPORTATNT LINE: used so we don't redirect to page we request.
	if (fileName.length == 0) {
		return;
	}
	let xhr = new XMLHttpRequest();
	//set the request type to post and the destination url to '/convert'
	let reqFile = '/getfile:' + fileName;
	xhr.open('POST', reqFile);
	//set the reponse type to blob since that's what we're expecting back
	xhr.responseType = 'blob';
	xhr.setRequestHeader('Content-Type', 'application/json');
	let infoStr = {
        name: "helloworld",
        age: 123
    };

    var json = JSON.stringify(infoStr);

	xhr.onload = function() {
		if (this.status == 200) {
			// Create a new Blob object using the response data of the onload object
			var blob = new Blob([this.response]);
			//Create a link element, hide it, direct it towards the blob, and then 'click' it programatically
			let a = document.createElement("a");
			a.style = "display: none";
			document.body.appendChild(a);
			//Create a DOMString representing the blob and point the link element towards it
			let url = window.URL.createObjectURL(blob);
			a.href = url;
			a.download = fileName;
			//programatically click the link to trigger the download
			a.click();
			//release the reference to the file by revoking the Object URL
			window.URL.revokeObjectURL(url);
			a.remove();
		} else {	//deal with your error state here
			console.log("err");
			window.location.replace("/404.html");
		}
	};
	xhr.send();
}

function RequestDocumentNumber(number, fileName) {
	//event.preventDefault();	//IMPORTATNT LINE: used so we don't redirect to page we request.
	if (number.length == 0) {
		return;
	}
	let xhr = new XMLHttpRequest();
	//set the request type to post and the destination url to '/convert'
	let reqFile = '/getfileindex:' + number;
	xhr.open('POST', reqFile);
	//set the reponse type to blob since that's what we're expecting back
	xhr.responseType = 'blob';
	xhr.setRequestHeader('Content-Type', 'application/json');

	xhr.onload = function() {
		if (this.status == 200) {
			var blob = new Blob([this.response], {type: 'image/pdf'});
			let a = document.createElement("a");
			a.style = "display: none";
			document.body.appendChild(a);
			let url = window.URL.createObjectURL(blob);
			a.href = url;
			a.download = fileName;
			a.click();
			window.URL.revokeObjectURL(url);
			a.remove();
		} else {
			//var blob = new Blob([this.response], {type: 'text/html'});
			console.log("err");
			window.location.replace("/404.html");
			//let fileURL = window.URL.createObjectURL(blob);
			//window.location.href = fileURL;
		}
	};
	xhr.send();
}

function RequestFileList(fileName) {
	//event.preventDefault();	//IMPORTATNT LINE: used so we don't redirect to page we request.
	if (fileName.length == 0) {
		return;
	}
	let xhr = new XMLHttpRequest();
	//set the request type to post and the destination url to '/convert'
	let reqFile = '/getFileList:' + fileName;
	xhr.open('POST', reqFile);
	xhr.onreadystatechange = function() {
		if (xhr.readyState == XMLHttpRequest.DONE) {
			//console.log(xhr.responseText);
			let data = JSON.parse(xhr.responseText);
			for (let i = 0; i < data.length; i++) {
				AddLine("fileBlock", data[i])
			}
			console.log(data);
			FileTree = data;
			let deep = 0;
			let element = document.getElementById("fileBlock");
			let blk = document.createElement("div");
			let htmltxtblk = "";
			htmltxtblk = readTree(htmltxtblk, FileTree, deep);
			blk.innerHTML = htmltxtblk;
			//console.log(htmltxtblk);
			element.append(blk);
			JSLists.createTree("fileBlock");
		}
	};
	xhr.send(200);
}

let myTextBox = document.getElementById('user_inp');
myTextBox.addEventListener('keypress', function(key) {	//has passed in key so we know if it is 'enter'
	if (key.keyCode == 13) {	//keyCode for enter
		RequestDocument(myTextBox.value);
		myTextBox.value= '';
	}
});

function FileFolderPressed() {
	RequestFileList("fList/");
}


//code init
let onSetGif = document.getElementById("loadgif");
onSetGif.style.display = 'none';
RequestFileList(".");
