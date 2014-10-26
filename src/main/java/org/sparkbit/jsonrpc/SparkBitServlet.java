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
            server.addHandler(SparkBitJSONRPCService.class, new SparkBitJSONRPCServiceImpl());
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
            server.call(serializer, is, os);

            is.close();
            os.close();
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }


    
}