/*
 * The MIT License
 *
 * Copyright 2014 Coin Sciences Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.sparkbit.jsonrpc;

import java.io.File;
import org.multibit.model.core.*;
import java.util.Properties;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.BaseModel;
import org.multibit.model.ModelEnum;
import org.eclipse.jetty.server.Server;
import org.multibit.file.FileHandler;
import org.sparkbit.ApplicationDataDirectoryLocator;
import java.util.Enumeration;

//import org.eclipse.jetty.server.Server;

/**
 * Controller for JSON RPC
 */
public enum JSONRPCController {
    INSTANCE;
    
    // netstat -lnptu  
    public static final int DEFAULT_PORT = 38332;
    
    public static final String DEFAULT_ALLOW_IP_LOCALHOST = "127.0.0.1";
    
    public static final int DEFAULT_TIMEOUT = 30000;
    public static final int DEFAULT_SSL_TIMEOUT = 500000;
    
    // Config properties
    public static final String RPC_SERVER = "rpcserver";
    public static final String RPC_DIGEST = "rpcdigest";
    public static final String RPC_USER = "rpcuser";
    public static final String RPC_PASSWORD = "rpcpassword"; // Credential
    public static final String RPC_TIMEOUT = "rpctimeout"; // in seconds
    public static final String RPC_PORT = "rpcport"; // default port is 38332
    public static final String RPC_ALLOW_IP = "rpcallowip";
    public static final String RPC_SSL = "rpcssl";
    public static final String RPC_SSL_ALLOW_TLS10 = "rpcsslallowtls10";
    public static final String RPC_SSL_ALLOW_TLS11 = "rpcsslallowtls11";
    public static final String RPC_SSL_KEYSTORE_FILENAME = "rpcsslkeystorefilename";
    
    private BitcoinController controller;
    private JettyEmbeddedServer jetty;
    private Properties config;
    
    // initialize this singleton
    public void initialize(BitcoinController controller) {
        this.controller = controller;
	this.config = FileHandler.loadJSONRPCConfig(new ApplicationDataDirectoryLocator());
	this.jetty = new JettyEmbeddedServer(this);
    }

    public BitcoinController getBitcoinController() {
	return this.controller;
    }
    
    public boolean shouldRunServer() {
	return this.jetty.runServer;
    }
    
    public boolean canStartServer() {
	Server server = this.jetty.getServer();
	return server==null || server.isStopped();
    }
    
    public boolean canStopServer() {
	Server server = this.jetty.getServer();
	return server!=null;
    }
    
    public boolean startServer() {	
	boolean result = false;
	try {
	    if (this.jetty.getServer()==null) {
		this.jetty.startServer();
	    } else {
		this.jetty.restartServer();
	    }
	    result = true;
	} catch (java.net.BindException bindexception) {
	    System.out.println(">>>> Port already in use");
	} catch (Exception e) {
	    System.out.println(">>>> Failed to start jsonrpc server");	    	    
	}
	return result;
    }
    
    public boolean stopServer() {
	boolean result = false;
	try {
	    if (jetty != null) {
		jetty.stopServer();
	    }
	    result = true;
	} catch (Exception e) {
	    System.out.println(">>>> Failed to stop jsonrpc server");	    
	}	
	return result;
    }
    
    public String getPreference(String key) {
	String value = this.config.getProperty(key);
	//String value = this.controller.getModel().getUserPreference(key);
	if (value!=null) {
	    value = value.trim();
	    if (value.length()==0) {
		value = null;
	    }
	}
	return value;
    }
    
    public boolean getBoolPreference(String key) {
	return Boolean.TRUE.toString().equals( getPreference(key));
    }
    
    public int getIntPreference(String key) {
	String s = getPreference(key);
	if (s==null || s.length()==0) return 0;
	int n = 0;
	try {
	    n = Integer.parseInt(s);
	} catch (NumberFormatException nfe) {
	}
	return n;
    }
    
    public String getPathOfKeystore(String filename) {
	ApplicationDataDirectoryLocator locator = new ApplicationDataDirectoryLocator();
	String path = locator.getApplicationDataDirectory() + File.separator + filename;
	return path;
    }
    
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("JSON-RPC User Configuration:\n");
	Enumeration e = config.propertyNames();
	while (e.hasMoreElements()) {
	    String key = (String) e.nextElement();
	    sb.append("   " + key + " = " + config.getProperty(key) + "\n");
	}
	sb.append("\n");
	sb.append(this.jetty.toString());
	return sb.toString();
    }
}
