let FileTree = null;

function AddLine(id, text) {
	let element = document.getElementById(id);
	let node = document.createElement("p");
	node.innerText = text;
	element.appendChild(node);
}

function readTree(txtBlk, root, depth) {
	//let element = document.getElementById(id);
	//let node1 = document.createElement("li");
	txtBlk += "<li onclick=RequestDocumentNumber("+root.val+"," + "\""+ root.name+"\""+")>"
	let str = "";
	/*
	for (let x = 0; x < depth; x++) {
		str += "--";
	}
	*/
	txtBlk += str + root.name;
	//id.appendChild(node1);
	if (typeof root === 'object' && !Array.isArray(root) && root !== null && root.hasOwnProperty('list')) {
		for (let i = 0; i < root.list.length; i++) {
			depth += 1;
			txtBlk += "<ul>"
			txtBlk = readTree(txtBlk, root.list[i], depth);
			depth -= 1;
			txtBlk += "</ul>"
			
		}
	}
	txtBlk += "</li>"
	return txtBlk;
}

function request() {
	let xhr = new XMLHttpRequest();
	xhr.open('GET', '/reqval');
	xhr.onreadystatechange = function() {
		if (xhr.readyState == XMLHttpRequest.DONE) {
			let data = JSON.parse(xhr.responseText);
			//console.log(data);
			val = data
			console.log(val);
		}
	};
	xhr.send(200);
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
			var blob = new Blob([this.response], {type: 'image/pdf'});
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
		} else {
			console.log("err");
			//deal with your error state here
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
			// Create a new Blob object using the response data of the onload object
			var blob = new Blob([this.response], {type: 'image/pdf'});
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
		} else {
			console.log("err");
			//deal with your error state here
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
			console.log(xhr.responseText);
			let data = JSON.parse(xhr.responseText);
			for (let i = 0; i < data.length; i++) {
				AddLine("fileBlock", data[i])
			}
			val = data
			console.log(val);
			FileTree = data;
			let deep = 0;
			let element = document.getElementById("fileBlock");
			let blk = document.createElement("div");
			let htmltxtblk = "";
			htmltxtblk = readTree(htmltxtblk, FileTree, deep);
			blk.innerHTML = htmltxtblk;
			console.log(htmltxtblk);
			element.append(blk);
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
RequestFileList(".");
//RequestFileList("data/");
//request();
//requestResume();
//request();
