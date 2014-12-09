SparkBit-CLI

SparBit-cli is a command-line tool to send JSON-RPC commands to a SparkBit application which has enabled it's built-in JSON-RPC service.

Requirements:

Go version 1.3.3 or greater

Barrister-Go
We use a patched version of Barrister-Go to generate stub files from sparkbit json api.
We want to let the user decide whether or not to allow insecure ssl connections e.g. to a server running a self-signed certificate.
https://github.com/bitcartel/barrister-go/tree/tlsconfig

Cross Platform
Install gox to build cross-platform.  Gox is used in the make shell script below.

Gitignore
We ignore the autogen folder which Barrister-Go creates sparkbit.go.
We ignore the target folder where we move Gox produced binaries to.

To build:
./make.sh

To run:
./target/sparkbit-cli_linux_amd64

