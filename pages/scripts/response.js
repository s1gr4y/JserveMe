//var declarations
let FileTree = null;

//functions

function readTree(txtBlk, root, depth) {
	if (root.isDir === "true") {
		txtBlk += "<li> <span onclick=\"RequestDocumentNumber("+root.val+ "," + "\'"+root.name+"\'"+")\" style=\"cursor: pointer; text-decoration: underline;\">"	//onclick=RequestDocumentNumber("+root.val+"," + "\'"+root.name+"\'"+")>
	} else {
		txtBlk += "<li> <span onclick=\"RequestDocumentNumber("+root.val+ "," + "\'"+root.name+"\'"+")\" style=\"cursor: pointer; color:teal; text-decoration: underline;\">"
	}
	let str = "";
	/*
	for (let x = 0; x < depth; x++) {
		str += "--";
	}
	*/
	//if (root.isDir === "true") {
		//txtBlk += str + root.name + "</span>";
	//} else {
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

function loadFiles(e) {
	let file = e.files[0];
	/*
	let result = null;
	const loaded = (e) => {
      const fr = e.target;
      result = fr.result;
		//result = "NAME:" + file.name + "\n" + result;
      console.log('Finished Loading!');
      console.log('Result:', result);
	  sendFile(result);
    }
	const processFile = (file) => {
      const fr = new FileReader();
      fr.readAsDataURL(file);
      fr.addEventListener('loadend', loaded);
    }
	if (file) {
		processFile(file);
	}
	*/
	sendFile(file);
}

function sendFile(e) {
	let xhr = new XMLHttpRequest();
	let formData = new FormData();
	//console.log(formData.files[0])
	let getid = document.getElementById("file");
	let f = getid.files[0];
	//xhr.open("POST", "/savefile:0"); 
	//formData.append("title", "---------------------------------------------");	//might not be needed
	//formData.append("name", file.fileName);	//might not be needed
	formData.append("file", f);
	//console.log(getid.file);
	//xhr.setRequestHeader('File-Name', file.name);
	//xhr.setRequestHeader('File-Type', file.type);

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
			let element = document.getElementById("fileBlock");
			element.innerHTML = "";
			RequestFileList(".");
		}
	}
	//fetch('/savefile:0', {method: "POST", body: e});
	xhr.open("POST", "/savefile:0");
	xhr.setRequestHeader('File-Name', f.name);
	xhr.setRequestHeader('Content-Type', f.type);
	//var blob = new Blob(e);
	xhr.send(f);	//formData
	//should have thing to indicate not fully downloaded by server with a loading icon or something until new header is sent. (aka in onreadystatechange, say to user it is done)
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
		}
	};
	xhr.send(200);
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
			console.log("err");
		}
	};
	xhr.send(200);
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
RequestFileList(".");
