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

import com.google.common.eventbus.Subscribe;
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
import org.coinspark.wallet.CSEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparkbit.SBEvent;
import org.sparkbit.SBEventType;
import com.google.bitcoin.core.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.locks.*;
import org.joda.time.DateTime;
import java.util.Date;
import org.apache.commons.lang3.time.StopWatch;
import org.multibit.network.MultiBitService;

//import org.eclipse.jetty.server.Server;

/**
 * Controller for JSON RPC
 */
public enum JSONRPCController {
    INSTANCE;
    
    private static final Logger log = LoggerFactory.getLogger(JSONRPCController.class);

    // netstat -lnptu  
    public static final int DEFAULT_PORT = 38332;
    
    public static final String DEFAULT_ALLOW_IP_LOCALHOST = "127.0.0.1";
    
    public static final int DEFAULT_TIMEOUT = 30000;
    public static final int DEFAULT_SSL_TIMEOUT = 500000;
    public static final int DEFAULT_SEND_ASSET_TIMEOUT = 20000;
    
    // Config properties
    public static final String RPC_SERVER = "rpcserver";
    public static final String RPC_DIGEST = "rpcdigest";
    public static final String RPC_USER = "rpcuser";
    public static final String RPC_PASSWORD = "rpcpassword"; // Credential
    public static final String RPC_TIMEOUT = "rpctimeout"; // in milliseconds
    public static final String RPC_PORT = "rpcport"; // default port is 38332
    public static final String RPC_ALLOW_IP = "rpcallowip";
    public static final String RPC_SSL = "rpcssl";
    public static final String RPC_SSL_ALLOW_TLS10 = "rpcsslallowtls10";
    public static final String RPC_SSL_ALLOW_TLS11 = "rpcsslallowtls11";
    public static final String RPC_SSL_KEYSTORE_FILENAME = "rpcsslkeystorefilename";
    public static final String RPC_SEND_ASSET_TIMEOUT = "rpcsendassettimeout"; // in milliseconds
    
    private BitcoinController controller;
    private JettyEmbeddedServer jetty;
    private Properties config;
    private boolean subscribedToEventBus;
    private Lock txBroadcastLock = new ReentrantLock();
    private ConcurrentHashMap<String,Condition> txBroadcastMap;

    public ConcurrentHashMap<String, Condition> getTxBroadcastMap() {
	return txBroadcastMap;
    }
   
    // initialize this singleton
    public void initialize(BitcoinController controller) {
        this.controller = controller;
	this.config = FileHandler.loadJSONRPCConfig(new ApplicationDataDirectoryLocator());
	this.jetty = new JettyEmbeddedServer(this);
    }
  
    // This method is only called once
    private void subscribeToEvents() {
	if (!subscribedToEventBus) {
	    txBroadcastMap = new ConcurrentHashMap<>();
	    CSEventBus.INSTANCE.registerAsyncSubscriber(this);
	    subscribedToEventBus = true;
	}
    }
    
    @Subscribe
    public void listen(SBEvent event) throws Exception {
	SBEventType t = event.getType();
	if (t == SBEventType.TX_CONFIDENCE_CHANGED) {
	    Transaction tx = (Transaction) event.getInfo();
	    int count = tx.getConfidence().getBroadcastByCount();
	    if (count > 0) {
		String txid = tx.getHashAsString();
		if (txBroadcastMap.containsKey(txid)) {
		    Condition c = txBroadcastMap.remove(txid);
		    txBroadcastLock.lock();
		    try {
			c.signalAll();
			log.debug("Peer broadcast count " + count + " for txid " + txid);
		    } catch (Exception e) {
			log.error("Condition signalAll() failed: " + e);
		    } finally {
			txBroadcastLock.unlock();
		    }
		}
	    }
	}
    }
    
    
    /*
    Return true if done (or skipped because timeout value is 0)
    Return false if timeout (deadline passed) or exception
    */
    public boolean waitForTxBroadcast(String txid) {
	int n = jetty.sendAssetTimeout;
	if (n==0) return true; // don't wait if timeout period is 0
	
	Date deadline = new DateTime().plusMillis(n).toDate();
	final StopWatch stopwatch = new StopWatch();
	stopwatch.start();
	boolean notYetElapsed;

	txBroadcastLock.lock();
	try {
	    Condition c = txBroadcastLock.newCondition();
	    txBroadcastMap.put(txid, c);
	    while (txBroadcastMap.containsKey(txid)) {
		notYetElapsed = c.awaitUntil(deadline);
		if (!notYetElapsed) {
		    log.debug("Peer broadcast waiting timed out for txid " + txid);
		    return false;
		}
	    }
	} catch (InterruptedException e) {
	    return false;
	} finally {
	    stopwatch.stop();
	    txBroadcastLock.unlock();
	    txBroadcastMap.remove(txid);  // remove txid, we are done with it whatever happens.
	}
	log.debug("Peer broadcast waiting took " + stopwatch + " for txid " + txid);
	return true;
    }
    
    public BitcoinController getBitcoinController() {
	return this.controller;
    }
    
    public boolean shouldRunServer() {
	if (this.jetty==null) return false;
	return this.jetty.runServer;
    }
    
    public boolean canStartServer() {
	if (this.jetty==null) return false;
	Server server = this.jetty.getServer();
	return server==null || server.isStopped();
    }
    
    public boolean canStopServer() {
	if (this.jetty==null) return false;
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

	    // Register ourselves as a listener the CSEventBus
	    subscribeToEvents();
	    
	} catch (java.net.BindException bindexception) {
	    log.error(">>>> Port already in use");
	} catch (Exception e) {
	    log.error(">>>> Failed to start jsonrpc server due to exception: " + e.getMessage());	    	    
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
	    log.error("Failed to stop jsonrpc server, exception: " + e.getMessage());	    
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
	if (config==null) return "Empty configuration";
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
