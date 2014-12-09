#!/bin/sh

# Use gox to build cross-platform
# auto generated files are put in autogen folder which is ignored by git
# sparkbit-cli binaries are in target folder

idl2go -d="autogen" -p=sparkbit ../barrister/sparkbit_api.json
mkdir -p target
gox -verbose -osarch="darwin/amd64 linux/386 linux/amd64 windows/386 windows/amd64"
mv sparkbit-cli_*_386* target/
mv sparkbit-cli_*_amd64* target/

