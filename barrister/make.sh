barrister -j sparkbit_api.json sparkbit_api.idl
idl2java -j sparkbit_api.json -p org.sparkbit.jsonrpc.autogen -o ../src/main/java/
cp sparkbit_api.json ../src/main/resources/

