/*
 * SparkBit-CLI
 *
 * Copyright 2014 Coin Sciences Ltd
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package main

import (
	// This is where Barrister creates sparkbit library files
	sparkbit "./autogen/sparkbit"

	"encoding/json"
	"fmt"
	barrister "github.com/coopernurse/barrister-go"
	"github.com/magiconair/goproperties"
	flag "github.com/ogier/pflag"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"log"
	"runtime"
	"path"
	"net"
)

/*
Cross-compile solutions
xgo with docker
https://groups.google.com/forum/#!topic/golang-nuts/YnYOXhdgnes
gonative
https://github.com/inconshreveable/gonative
https://inconshreveable.com/04-30-2014/cross-compiling-golang-programs-with-native-libraries/
goxc
https://github.com/laher/goxc
Dave Cheney
http://dave.cheney.net/2013/07/09/an-introduction-to-cross-compilation-with-go-1-1
https://github.com/davecheney/golang-crosscompile
*/
const (
	versionString = "0.9.3"
)

var flagConfigPath string

var flagUsername string
var flagPassword string
var flagSSL bool
var flagPort int
var flagAddress string

var flagSetMap map[string]bool

var flagVersion bool
var flagHelp bool
var flagInsecure bool

var defaultSSL bool
var defaultPort int
var defaultAddress string

var username string
var password string
var serverSSL bool
var serverPort int
var serverAddress string

var numArgs int
var numParams int
var verbose bool

/*
Cert prob.
RemoteClient
Call() is what sends the JSON RPC and it invokes send on trans variable.
NewRemoteClient takes trans Transport as a parameter.

// The Transport interface abstracts sending a serialized byte slice
type Transport interface {
Send(in []byte) ([]byte, error)
barrister.go line 702 creates http client and then sends data.
op
line 699 has a hook.
maybe we can set basic auth and pass in transport here?
no, would need to hack th elibrary.
can get serialized data and send it over our own transport?
}
*/
func NewSparkbitProxy(url string) sparkbit.Sparkbit {
	trans := &barrister.HttpTransport{Url: url}

	client := barrister.NewRemoteClient(trans, true)
	return sparkbit.NewSparkbitProxy(client)
}

func printVersion() {
	_, file := path.Split(os.Args[0])
	fmt.Printf("%s %s\n", file, versionString)
	fmt.Println("Copyright (C) 2014 Coin Sciences Ltd.")
	fmt.Println("Licensed under the MIT license <http://opensource.org/licenses/mit-license.php>.")
	fmt.Println("This is open source sofware; see the source for copying conditions.")
	fmt.Println("There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.")
}

func printUsage() {
	_, file := path.Split(os.Args[0])
	fmt.Printf("Usage: %s [options] command [params...]\n\n", file)
	fmt.Printf("Options:\n")
    flag.PrintDefaults()
    fmt.Printf(`
Available JSON-RPC commands:
 getstatus, stop
 listwallets, createwallet, deletewallet
 listbalances, listtransactions
 listaddresses, createaddresses, setaddresslabel
 addasset, setassetvisible, refreshasset, deleteasset
 sendbitcoin, sendasset
 listunspent, sendbitcoinwith, sendassetwith
`);
}

func init() {
	flag.Usage = printUsage

	// set the defaults
	defaultSSL = true
	defaultPort = 38332
	defaultAddress = "localhost"

	defaultConfigPath := computeDefaultConfigPath()

	flag.StringVarP(&flagConfigPath, "config", "c", defaultConfigPath, "config file path")
	flag.StringVarP(&flagUsername, "rpcuser", "u", "", "rpc user name")
	flag.StringVarP(&flagPassword, "rpcpassword", "p", "", "rpc password")
	flag.BoolVar(&flagSSL, "ssl", defaultSSL, "use secure sockets SSL")
	flag.IntVarP(&flagPort, "port", "n", defaultPort, "port number")
	flag.StringVarP(&flagAddress, "host", "a", defaultAddress, "server address")

	flag.BoolVarP(&verbose, "verbose", "v", false, "verbose output for debugging")
	// k , ignore server certificate errors (not recommended, only for testing self-signed certificates")

	flag.BoolVar(&flagHelp, "help", false, "Display this information")
	flag.BoolVar(&flagVersion, "version", false, "Display version information")
	flag.BoolVarP(&flagInsecure, "insecure", "k", false, "Allow connections to servers with 'insecure' SSL connections e.g. self-signed certificates. By default, this option is false, unless the server is localhost or an explicit IP address, in which case the option is true.")

	flag.Parse()

	flagSetMap = make(map[string]bool)
	flag.Visit(visitFlagSetMap)
}

// We want to know which flags were set
func visitFlagSetMap(f *flag.Flag) {
	flagSetMap[f.Name] = true
	//fmt.Printf("visit: %+v\n", *f)
}



// Cross-platform compile issues with Gox due to cgo internals
// https://code.google.com/p/go/issues/detail?id=6376
// http://stackoverflow.com/questions/7922270/obtain-users-home-directory

func computeDefaultConfigPath() string {
	var configPath string
//	usr, _ := user.Current()
//	homedir := usr.HomeDir
// use go-homedir (Darwin fails)
//	homePath, _ := homedir.Dir()

	// Let's just use $HOME which should be good enough
	homePath := os.Getenv("HOME")

	if runtime.GOOS == "darwin" {
		configPath = filepath.Join(homePath, "Library", "Application Support", "SparkBit-Data/jsonrpc.properties")
	} else if runtime.GOOS == "windows" {
		configPath = filepath.Join(os.Getenv("APPDATA"), "SparkBit-Data", "jsonrpc.properties")
	} else {
		// default: linux and bsd
		//if runtime.GOOS == "linux" {
		configPath = filepath.Join(homePath, "SparkBit-Data/jsonrpc.properties")
	}
	return configPath
}

func main() {
	if flagVersion {
		printVersion()
		os.Exit(0)
	}

	// get the config data
	var configPath string
	if flagConfigPath != "" {
		configPath = flagConfigPath
	} else {
		configPath = computeDefaultConfigPath()
	}

	if verbose {
		fmt.Println("Attempting to load config file from path: ", configPath)
	}

	props, err := properties.LoadFile(configPath, properties.UTF8)
	var didLoadConfig = err == nil
	if didLoadConfig {
		if verbose {
			fmt.Printf("Successfully loaded config file with the following properties:\n%v\n", props)
		}
		username = props.GetString("rpcuser", "")
		password = props.GetString("rpcpassword", "")
		serverSSL = props.GetBool("rpcssl", defaultSSL)
		serverPort = props.GetInt("rpcport", defaultPort)
		serverAddress = defaultAddress
	} else {
		if verbose {
			fmt.Printf("Problem loading config: %v\n", err)
		}
		serverSSL = defaultSSL
		serverPort = defaultPort
		serverAddress = defaultAddress
	}

	// Command line argument over-rides the config file.
	// We only want values which have been set
	if flagSetMap["rpcuser"] == true {
		username = flagUsername
	}
	if flagSetMap["rpcpassword"] == true {
		password = flagPassword
	}
	if flagSetMap["ssl"] == true {
		serverSSL = flagSSL
	}
	if flagSetMap["port"] == true {
		serverPort = flagPort
	}
	if flagSetMap["host"] == true {
		serverAddress = flagAddress
	}

	if verbose {
		fmt.Println("Using the following parameters where command line > config > defaults")
		fmt.Printf("rpcuser: %s\n", username)
		fmt.Printf("rpcpassword: %s\n", password)
		fmt.Println("ssl:", serverSSL)
		fmt.Println("port:", serverPort)
		fmt.Println("host:", serverAddress)
	}

	// After options and flags, everything else is json method and parameters
	args := flag.Args()
	//	fmt.Printf("JSON COMMAND: %+v\n", jsoncall)
	//	for index, element := range jsoncall {
	//		fmt.Printf("JSON COMMAND %d : %v\n", index, element)
	//	}

	numArgs = len(args)
	numParams = numArgs - 1

	if numArgs == 0 {
		flag.Usage()
		os.Exit(1)
	}

	method := args[0]
	params := args[1:]

	var buffer []byte

	protocol := "http"
	if serverSSL {
		protocol = "https"
	}
	connectionString := fmt.Sprintf("%s://%s:%s@%s:%d/api", protocol, username, password, serverAddress, serverPort)

	if verbose {
		fmt.Println("URL connection string to server is: ", connectionString)
	}

	// Set a global option on our patched version of barrister.go
	// to allow connecting to self-signed certs
	// If connecting to localhost, we don't require CA signed cert.
	// If the insecure flag was explicitly set, use that value, otherwise our defaults
	if flagSetMap["insecure"] == false {
		serverIP := net.ParseIP(serverAddress)
		if serverIP != nil || serverAddress=="127.0.0.1" || strings.ToLower(serverAddress)=="localhost" {
			flagInsecure = true
		}
	}
	barrister.Global_insecure_ssl = flagInsecure
	if verbose {
		fmt.Println("insecure ssl flag = " , flagInsecure)
		fmt.Print("\n");
	}

	sparkbit := NewSparkbitProxy(connectionString)

	var res interface{}

	/*
		command := strings.ToUpper(method[:1]) + strings.ToLower(method[1:])
	fmt.Println("command = ", command)

	// Oct 2014, still cannot get arguments of method via introspection
	// https://groups.google.com/forum/#!topic/golang-nuts/nM_ZhL7fuGc

		//	var foo Sparkbit{}
		fooType := reflect.TypeOf(sparkbit)

		var v reflect.Value
		v = fooType.MethodByName(command)
		if theMethod != nil {
			fmt.Println("FOUND THE METHOD")
		}

		for i := 0; i < fooType.NumMethod(); i++ {
	    	meth := fooType.Method(i)
	    	fmt.Println(meth.Name)
		}
	*/

	switch method {
	case "addasset":
		validateParams(2, method, "WALLETNAME ASSETREF")
		res, err = sparkbit.Addasset(params[0], params[1])
	case "createaddresses":
		validateParams(2, method, "WALLETNAME QUANTITY")
		i64, perr := strconv.ParseInt(params[1], 10, 64)
		if perr != nil {
			fmt.Println("Quantity parameter must be a number")
			log.Fatal(perr)
		}
		res, err = sparkbit.Createaddresses(params[0], i64)
	case "createwallet":
		validateParams(1, method, "WALLETNAME")
		res, err = sparkbit.Createwallet(params[0])
	case "deletewallet":
		validateParams(1, method, "WALLETNAME")
		res, err = sparkbit.Deletewallet(params[0])
	case "getstatus":
		validateParams(0, method, "")
		res, err = sparkbit.Getstatus()
	case "listaddresses":
		validateParams(1, method, "WALLETNAME")
		res, err = sparkbit.Listaddresses(params[0])
	case "listbalances":
		validateParams(2, method, "WALLETNAME ONLYVISIBLE")
		flag, perr := strconv.ParseBool(params[1])
		if perr != nil {
			fmt.Println("Visible must be true or false")
			os.Exit(1)
		}
		res, err = sparkbit.Listbalances(params[0], flag)
	case "listtransactions":
		validateParams(2, method, "WALLETNAME NUMOFTRANSACTIONS")
		i64, perr := strconv.ParseInt(params[1], 10, 64)
		if perr != nil {
			fmt.Println("Number of transactions must be a valid number")
			os.Exit(1)
		}
		res, err = sparkbit.Listtransactions(params[0], i64)
	case "listunspent":
		validateParams(4, method, "WALLETNAME MINCONF MAXCONF ADDRESSES\n* Set MINCONF or MAXCONF to 0 if you want to include unconfirmed transactions.\n* Set ADDRESSES to \"\" or - if you want all UTXOs")
		minconf, perr := strconv.ParseInt(params[1], 10, 64)
		maxconf, perr2 := strconv.ParseInt(params[2], 10, 64)
		if perr != nil {
			fmt.Println("Minimum number of confirmations must be a valid number")
		}
		if perr2 != nil {
			fmt.Println("Maximum number of confirmations must be a valid number")
		}
		if perr != nil || perr2 != nil {
			os.Exit(1)
		}
		z := strings.TrimSpace(params[3])
		var addresses []string
		if z == "-" {
			addresses = []string{}
		} else {
			addresses = strings.Split(z, ",")
		}
		res, err = sparkbit.Listunspent(params[0], minconf, maxconf, addresses)
	case "listwallets":
		validateParams(0, method, "")
		res, err = sparkbit.Listwallets()
	case "deleteasset":
		validateParams(2, method, "WALLETNAME ASSETREF")
		res, err = sparkbit.Deleteasset(params[0], params[1])
	case "refreshasset":
		validateParams(2, method, "WALLETNAME ASSETREF")
		res, err = sparkbit.Refreshasset(params[0], params[1])
	case "sendasset":
		validateParams(5, method, "WALLETNAME ADDRESS ASSETREF QUANTITY SENDERPAYS")
		f64, perr := strconv.ParseFloat(params[3], 64)
		if perr != nil {
			fmt.Println("Quantity must be a valid number")
			os.Exit(1)
		}
		flag, perr := strconv.ParseBool(params[4])
		if perr != nil {
			fmt.Println("Sender pays flag must be true or false")
			os.Exit(1)
		}
		res, err = sparkbit.Sendasset(params[0], params[1], params[2], f64, flag)
	case "sendbitcoin":
		validateParams(3, method, "WALLETNAME ADDRESS AMOUNT")
		f64, perr := strconv.ParseFloat(params[2], 64)
		if perr != nil {
			fmt.Println("Amount is not a valid number")
			os.Exit(1)
		}
		res, err = sparkbit.Sendbitcoin(params[0], params[1], f64)
	case "sendassetwith":
		validateParams(7, method, "WALLETNAME TXID VOUT ADDRESS ASSETREF QUANTITY SENDERPAYS")
		vout, perr := strconv.ParseInt(params[2], 10, 64)
		if perr != nil {
			fmt.Println("Vout must be a valid number")
			os.Exit(1)
		}
		f64, perr := strconv.ParseFloat(params[5], 64)
		if perr != nil {
			fmt.Println("Quantity must be a valid number")
			os.Exit(1)
		}
		flag, perr := strconv.ParseBool(params[6])
		if perr != nil {
			fmt.Println("Sender pays flag must be true or false")
			os.Exit(1)
		}
		res, err = sparkbit.Sendassetwith(params[0], params[1], vout, params[3], params[4], f64, flag)
	case "sendbitcoinwith":
		validateParams(5, method, "WALLETNAME TXID VOUT ADDRESS AMOUNT")
		vout, perr := strconv.ParseInt(params[2], 10, 64)
		if perr != nil {
			fmt.Println("Vout must be a valid number")
			os.Exit(1)
		}

		f64, perr := strconv.ParseFloat(params[4], 64)
		if perr != nil {
			fmt.Println("Amount is not a valid number")
			os.Exit(1)
		}
		res, err = sparkbit.Sendbitcoinwith(params[0], params[1], vout, params[3], f64)
	
	
	case "setaddresslabel":
		validateParams(3, method, "WALLETNAME ADDRESS LABEL")
		res, err = sparkbit.Setaddresslabel(params[0], params[1], params[2])
	case "setassetvisible":
		validateParams(3, method, "WALLETNAME ASSETREF ISVISIBLE")
		flag, perr := strconv.ParseBool(params[2])
		if perr != nil {
			fmt.Println("Visibility flag must be true or false")
			os.Exit(1)
		}
		res, err = sparkbit.Setassetvisible(params[0], params[1], flag)
	case "stop":
		validateParams(0, method, "")
		res, err = sparkbit.Stop()
	default:
		fmt.Println("ERROR: Unknown command: " + method)
		os.Exit(1)
	}

	if err != nil {
		fmt.Printf("ERROR: %v\n", err)
		os.Exit(1)
	}

	buffer, err = json.MarshalIndent(res, "", "  ")
	if err != nil {
		fmt.Printf("ERROR: %v\n", err)
		os.Exit(1)
	}

	bufferString := string(buffer)
	if (method=="sendbitcoin" || method=="sendasset") {
		bufferString = strings.Trim(bufferString, "\"")
	}

	//os.Stdout.Write(buffer)
	os.Stdout.WriteString(bufferString)
	os.Stdout.WriteString("\n")
}


// Basic validation that we have the correct number of arguments
func validateParams(n int, cmd string, s string) {
	if numParams == n {
		return
	}

	if numParams < n {
		fmt.Println("Error: Too few parameters")
	} else if numParams > n {
		fmt.Println("Error: Too many parameters")
	}

	fmt.Println("Correct Usage: " + strings.ToLower(cmd) + " " + s)
	os.Exit(1)
}
