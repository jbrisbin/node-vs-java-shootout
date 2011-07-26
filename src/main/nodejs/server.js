var express = require("express"),
		fs = require("fs"),
		crypto = require("crypto");


var app = express.createServer();

// Handle upload
app.put("/upload", function(req, res) {

	res.header("Connection", "closed");
	res.header("Content-Type", "application/json");

	var md5 = crypto.createHash("md5");
	var ws = fs.createWriteStream("upload/incoming");

	req.on("data", function(ch) {
		// To see Node.js slow way, way down, uncomment this:
		// md5.update(ch);
		ws.write(ch);
	});

	req.on("end", function() {
		ws.end();
		ws.destroySoon();
		res.end();
	});

	ws.on("close", function() {
		console.log("Upload complete. Hash: " + md5.digest("hex"));
	});

});

app.listen(8080);