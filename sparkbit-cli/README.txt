### SparkBit-CLI

SparBit-cli is a command-line tool to send JSON-RPC commands to a SparkBit application which has enabled it's built-in JSON-RPC service.

Requirements:

* Go version 1.3.3 or greater
* Barrister-Go (patched version)
* Misc Go libraries

### Misc Go libraries

go get github.com/magiconair/goproperties
go get github.com/ogier/pflag


### Barrister-Go
We use Barrister-Go to generate stub files for the SparkBit JSON API.  You need to use a patched version of Barrister-Go which provides the option of insecure ssl connections, for example, to a development server running a self-signed certificate.

Install barrister-go as normal:
```
go get github.com/coopernurse/barrister-go
```
In $GOPATH where the src has been downloaded, you want to create a branch, for example, tlsconfig.
```
git checkout -b tlsconfig
```
Add the patched version as a remote source e.g.:
```
git remote set-url bitcartelpatch https://github.com/bitcartel/barrister-go
```
Get the code for branch tlsconfig
```
git pull bitcartelpatch
```
When you git status on $GOPATH/src/github.com/coopernurse/barrister-go you should be on this commit (or a later one if this document is old):
```
973854065b6d7430450b745ee0eb0072979ff6ea
```
Clean and build Barrister-Go again with this new patched version:
```
go clean github.com/coopernurse/barrister-go
go build github.com/coopernurse/barrister-go
```

### Cross Platform
Install gox to build cross-platform.  Gox is used in the make shell script below.

### Gitignore
We ignore the autogen folder which Barrister-Go creates sparkbit.go.
We ignore the target folder where we move Gox produced binaries to.

### To build
```
./make.sh
```

### To run
```
./target/sparkbit-cli_linux_amd64
```

