/*
 * SparkBit
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
package org.sparkbit.jsonrpc;

import java.io.IOException;

import com.bitmechanic.barrister.*;
import com.bitmechanic.barrister.Contract;
import com.bitmechanic.barrister.Server;
import com.bitmechanic.barrister.JacksonSerializer;
import org.sparkbit.jsonrpc.autogen.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class SparkBitServlet extends HttpServlet {
    private Contract contract;
    private Server server;
    private JacksonSerializer serializer;

    public SparkBitServlet() {
        // Serialize requests/responses as JSON using Jackson
        serializer = new JacksonSerializer();
    }

    public void init(ServletConfig config) throws ServletException {
        try {
	    // SIMON: EDIT JSON FILE NAME
            // Load the contract from the IDL JSON file
            contract = Contract.load(getClass().getResourceAsStream("/sparkbit_api.json"));
            server = new Server(contract);

	    // SIMON: EDIT NAME OF CLASS
            // Register our service implementation
            server.addHandler(sparkbit.class, new SparkBitJSONRPCServiceImpl());
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            InputStream is = req.getInputStream();
            OutputStream os = resp.getOutputStream();
            resp.addHeader("Content-Type", "application/json");

            // This will deserialize the request and invoke
            // our ContactServiceImpl code based on the method and params
            // specified in the request. The result, including any
            // RpcException (if thrown), will be serialized to the OutputStream
        //    server.call(serializer, is, os);
	    
	    // We use a modified version of the above call so that we can filter 'stop' for localhost
	    mycall(req.getRemoteHost(), server, serializer, is, os);

            is.close();
            os.close();
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Reads a RpcRequest from the input stream, deserializes it, invokes the
     * matching handler method, serializes the result, and writes it to the
     * output stream.
     *
     * @param remoteHost the host of the caller
     * @param server The jetty server we will pass the call onto
     * @param ser Serializer to use to decode the request from the input stream,
     * and serialize the result to the the output stream
     * @param is InputStream to read the request from
     * @param os OutputStream to write the response to
     * @throws IOException If there is a problem reading or writing to either
     * stream, or if the request cannot be deserialized.
     */
    @SuppressWarnings("unchecked")
    public void mycall(String remoteHost, Server server, Serializer ser, InputStream is, OutputStream os)
	    throws IOException {
	Object obj = null;
	try {
	    obj = ser.readMapOrList(is);
	} catch (Exception e) {
	    String msg = "Unable to deserialize request: " + e.getMessage();
	    ser.write(new RpcResponse(null, RpcException.Error.PARSE.exc(msg)).marshal(), os);
	    return;
	}
	if (obj instanceof List) {
	    List list = (List) obj;
	    List respList = new ArrayList();
	    for (Object o : list) {
		RpcRequest rpcReq = new RpcRequest((Map) o);
		System.out.println(">>>> LIST CAST: (Map) o = " + (Map)o );
		respList.add(server.call(rpcReq).marshal()); // modified
	    }
	    ser.write(respList, os);
	} else if (obj instanceof Map) {
	    // Modified: only allow stop method if remote host is actually localhost 127.0.0.1
	    String method = (String)((Map)obj).get("method");
	    if (method.equals("sparkbit.stop") && !remoteHost.equals("127.0.0.1")) {
		ser.write(new RpcResponse(null, RpcException.Error.INVALID_REQ.exc("Invalid Request - You can only invoke stop from localhost")).marshal(), os);	
	    } else {		
		RpcRequest rpcReq = new RpcRequest((Map) obj);
		ser.write(server.call(rpcReq).marshal(), os); // modified
	    }
	} else {
	    ser.write(new RpcResponse(null, RpcException.Error.INVALID_REQ.exc("Invalid Request")).marshal(), os);
	}
    }

}