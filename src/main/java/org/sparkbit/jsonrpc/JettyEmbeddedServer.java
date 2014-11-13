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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;

import org.eclipse.jetty.server.Handler;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Collections;

// BASIC AUTH
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;

// IP ADDRESS RESTRICTION
import org.eclipse.jetty.server.handler.IPAccessHandler;
import static org.sparkbit.jsonrpc.JSONRPCController.*;


/**
 * Example of configuring multiple connectors:
 * http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/examples/embedded/src/main/java/org/eclipse/jetty/embedded/ManyConnectors.java
 */
public class JettyEmbeddedServer {

    private JSONRPCController controller = null;
    private Server server = null;

    public boolean runServer;
    public boolean useDigest;
    public String user;
    public String password;
    public int timeout = DEFAULT_TIMEOUT;
    public int port = DEFAULT_PORT;
    public String allowIP = DEFAULT_ALLOW_IP_LOCALHOST;
    public String[] whiteListAllowedIP;
    public boolean useSSL;
    public boolean allowTLS10;
    public boolean allowTLS11;
    public String keystoreFilename;
    
     public String toString() {
	if (server==null) {
	    return "Jetty embedded server has not been instantiated yet\n";
	}
	StringBuilder sb = new StringBuilder();
	sb.append("Jetty Embedded Server:\n");
	sb.append(" run server = " + this.runServer + "\n");
	sb.append(" timeout = " + this.timeout + "\n");
	sb.append(" port = " + this.port + "\n");
	sb.append(" allowed ip addresses = " + allowIP + "\n");
	sb.append(" use digest authentication = " + this.useDigest + "\n");
	sb.append(" username = " + this.user + "\n");
	sb.append(" password = " + this.password + "\n");
	sb.append(" require SSL = " + this.useSSL + "\n");
	sb.append(" allow tls 1.0 = " + this.allowTLS10 + "\n");
	sb.append(" allow tls 1.1 = " + this.allowTLS11 + "\n");
	sb.append(" keystore filename = " + this.keystoreFilename + "\n");
	/*
		Connector[] connectors = server.getConnectors();
	for (int i=0; i<connectors.length; i++) {
	    ServerConnector connector = (ServerConnector) connectors[i];
	*/
	for (Connector con : server.getConnectors()) {
	    ServerConnector connector = (ServerConnector) con;
	    sb.append(" >connector: " + connector.toString() + "\n");
	    sb.append("\n  host = " + connector.getHost());
	    sb.append("\n  name = " + connector.getName());
	    sb.append("\n  state = " + connector.getState());
	    sb.append("\n  idle timeout = " + connector.getIdleTimeout());
	    sb.append("\n  local port = " + connector.getLocalPort());
	    sb.append("\n  port = " + connector.getPort());
	    sb.append("\n  so linger time = " + connector.getSoLingerTime());
	    sb.append("\n  stop timeout = " + connector.getStopTimeout());
	    sb.append("\n");
	}
	sb.append("\n");
	return sb.toString();
    }
    public Server getServer() {
	return server;
    }
    
    public JettyEmbeddedServer(JSONRPCController controller) {
	this.controller = controller;
	loadPreferences();
    }
    
    private void loadPreferences() {
	runServer = this.controller.getBoolPreference(RPC_SERVER);
	useSSL = this.controller.getBoolPreference(RPC_SSL);

	useDigest = this.controller.getBoolPreference(RPC_DIGEST);

	user = this.controller.getPreference(RPC_USER);
	if (user==null) user="";
	password = this.controller.getPreference(RPC_PASSWORD);
	if (password==null) password="";
	
	port = DEFAULT_PORT;
	int n = this.controller.getIntPreference(RPC_PORT);
	if (n!=0) port = n;
	
	timeout = (useSSL) ? DEFAULT_SSL_TIMEOUT : DEFAULT_TIMEOUT;
	n = this.controller.getIntPreference(RPC_TIMEOUT);
	if (n!=0) timeout = n;
	
	allowIP = DEFAULT_ALLOW_IP_LOCALHOST;
	if (useSSL) {
	    String s = this.controller.getPreference(RPC_ALLOW_IP);
	    if (s!=null) {
		allowIP = s;
		whiteListAllowedIP = s.split(",");
//		System.out.println( ">>>> allowIP = " + allowIP);
		//System.out.println( ">>>> whiteListAllowedIP = " + whiteListAllowedIP);
		for (int i=0; i<whiteListAllowedIP.length; i++) {
		    whiteListAllowedIP[i] = whiteListAllowedIP[i].trim();
		}
	    }
	}
	// FIXME: Check this is correct behaviour of bitcoind, that we ignore IP lists if non-SSL.
	if (!useSSL) {
	    allowIP = DEFAULT_ALLOW_IP_LOCALHOST;
	} 
	
	allowTLS10 = this.controller.getBoolPreference(RPC_SSL_ALLOW_TLS10);
	allowTLS11 = this.controller.getBoolPreference(RPC_SSL_ALLOW_TLS11);
	keystoreFilename = this.controller.getPreference(RPC_SSL_KEYSTORE_FILENAME);
	// java.io.FileNotFoundException gets thrown if keystore not found
    }
    
    private ServerConnector createServerConnectorHTTP() throws Exception {
	    // HTTP Configuration
	    // HttpConfiguration is a collection of configuration information appropriate for http and https. The default
	    // scheme for http is <code>http</code> of course, as the default for secured http is <code>https</code> but
	    // we show setting the scheme to show it can be done.  The port for secured communication is also set here.
	    HttpConfiguration http_config = new HttpConfiguration();
//	    http_config.setSecureScheme("https");
//	    http_config.setSecurePort(8081);
//	    http_config.setOutputBufferSize(32768);

	    // HTTP connector
	    // The first server connector we create is the one for http, passing in the http configuration we configured
	    // above so it can get things like the output buffer size, etc. We also set the port (8080) and configure an
	    // idle timeout.
	    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
	    http.setPort(this.port);
	    http.setIdleTimeout(this.timeout);
	    
	    if (useSSL) {
		http.setHost("0.0.0.0");    // bind to IP address
	    } else {
		http.setHost("localhost"); // restrict to local, just accept 127.0.0.1
	    }
	    
	    http.setAcceptQueueSize(10);
	    
	    return http;
    }
  
    private ServerConnector createServerConnectorHTTPS() throws Exception {
	    /*
	    	sslContextFactory.setTrustStore(TRUSTSTORE_LOCATION);
		sslContextFactory.setTrustStorePassword(TRUSTSTORE_PASS);
		sslContextFactory.setNeedClientAuth(true);
	    */
	    
	    // SSL Context Factory for HTTPS and SPDY
	    // SSL requires a certificate so we configure a factory for ssl contents with information pointing to what
	    // keystore the ssl connection needs to know about. Much more configuration is available the ssl context,
	    // including things like choosing the particular certificate out of a keystore to be used.
	    SslContextFactory sslContextFactory = new SslContextFactory();
	    String keystorePathString = controller.getPathOfKeystore(this.keystoreFilename);
	    System.out.println(">>>> keystorePathString = " + keystorePathString);
	    sslContextFactory.setKeyStorePath(keystorePathString);
	    sslContextFactory.setKeyStorePassword("password");
	    sslContextFactory.setKeyManagerPassword("password");
	    // FIXME: Java keystores must have a password. Can't be blank.
	    
	    	    /*
	     Ideally we want TLS 1.2 only, PFS only and 256 bits only.
	     Allowing TLS v1 is useful for command line tools (which have not been updated).

	     https://en.bitcoin.it/wiki/Enabling_SSL_on_original_client_daemon
	
	     http://www.eclipse.org/jetty/documentation/current/configuring-ssl.html
	
	     Table of cipher suites available in Java SDK:
	     http://docs.oracle.com/javase/7/docs/technotes/guides/security/SunProviders.html
	
	     Useful discussion:
	     http://stackoverflow.com/questions/26382540/how-to-disable-the-sslv3-protocol-in-jetty-to-prevent-poodle-attack
	     http://security.stackexchange.com/questions/51680/optimal-web-server-ssl-cipher-suite-configuration
	     https://community.qualys.com/blogs/securitylabs/2013/09/10/is-beast-still-a-threat
	
	     To scan port to see what ciphers and protocols are accepted:
	     https://github.com/DinoTools/sslscan
	     sslscan --no-failed localhost:8081
	     or use pysslscan:
	     pysslscan scan --scan=server.ciphers --report=term --tls12 127.0.0.1:8081
	     */
	    //String[] myProtocols = {"TLSv1.2", "TLSv1.1", "TLSv1"};
	    ArrayList<String> protocols = new ArrayList<String>();
	    protocols.add("TLSv1.2");
	    if (allowTLS11) protocols.add("TLSv1.1");
	    if (allowTLS10) protocols.add("TLSv1");
	    String[] myProtocols = protocols.toArray(new String[protocols.size()]);
	    
	    String[] badProtocols ={"SSL.*"}; // SSLv2Hello, SSLv3
	    String[] myCiphers = {
		"TLS_ECDHE_.*",
		"TLS_DHE_.*"
	    };
	    String[] badCiphers = {".*_DES_.*", ".*_RC4_.*", ".*_NULL_.*"};
	    sslContextFactory.setIncludeProtocols(myProtocols);
	    sslContextFactory.setExcludeProtocols(badProtocols);
	    sslContextFactory.setIncludeCipherSuites(myCiphers);
	    sslContextFactory.setExcludeCipherSuites(badCiphers);

        // HTTPS Configuration
	    // A new HttpConfiguration object is needed for the next connector and you can pass the old one as an
	    // argument to effectively clone the contents. On this HttpConfiguration object we add a
	    // SecureRequestCustomizer which is how a new connector is able to resolve the https connection before
	    // handing control over to the Jetty Server.
	    HttpConfiguration https_config = new HttpConfiguration();
	    https_config.setSecureScheme("https");
	    https_config.addCustomizer(new SecureRequestCustomizer());

        // HTTPS connector
	    // We create a second ServerConnector, passing in the http configuration we just made along with the
	    // previously created ssl context factory. Next we set the port and a longer idle timeout.
	    ServerConnector https = new ServerConnector(server,
		    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
		    new HttpConnectionFactory(https_config));
	    https.setPort(this.port);
	    https.setIdleTimeout(this.timeout);

	    if (useSSL) {
		https.setHost("0.0.0.0");    // bind to IP address
	    } else {
		https.setHost("localhost"); // restrict to local, just accept 127.0.0.1
	    }
	    https.setAcceptQueueSize(10);
	    
	    return https;
    }
    
    public void restartServer() throws Exception {
	if (this.server != null) {
	    this.server.start();
	}
    }

    public void startServer() throws Exception {
	try {
	    this.server = new Server();
 
	    // We can set multiple connectors at the same time
	    // server.setConnectors(new Connector[] { http, https });
	    
	    if (this.useSSL) {
		server.addConnector(createServerConnectorHTTPS());
	    } else {
		server.addConnector(createServerConnectorHTTP());
	    }

	    /*
	     ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	     context.setSecurityHandler(basicAuth("scott", "tiger", "Private!"));
	     context.setContextPath("/");
	     server.setHandler(context);
	
	     ServletHolder servletHolder = new ServletHolder(
	     DatePrintServlet.class);
	     context.addServlet(servletHolder, "/date");
	     */
	    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    //context.setContextPath("/app");
	    ServletHolder servletHolder = new ServletHolder(
		    DatePrintServlet.class);
	    context.addServlet(servletHolder, "/date");
	    
	    servletHolder = new ServletHolder(SparkBitServlet.class);
	    context.addServlet(servletHolder, "/api");

	    // We could create a second context at a different context path and add servlets..
//	    ServletContextHandler context2 = new ServletContextHandler(ServletContextHandler.SESSIONS);
//	    context2.setContextPath("/json2");
//	    ServletHolder servletHolder2 = new ServletHolder(HelloServlet.class);
//	    context2.addServlet(servletHolder2, "/hello");

	    
	    // Load users and passwords from /src/main/resources folder
	    /*
	    URL realmURL = getClass().getResource("/realm.properties");
	    String realmPathString = Paths.get(realmURL.toURI()).toString();
	    LoginService loginService = new HashLoginService("jsonrpc", realmPathString);
	    */
	    
	    // Programmatically create the user and password
	    HashLoginService loginService = new HashLoginService("jsonrpc");
//	    loginService.update("user", Credential.getCredential("password"), new String[]{"user"} );
	    loginService.update(this.user, Credential.getCredential(this.password), new String[]{"user"} );

	    server.addBean(loginService);

	    // A security handler is a jetty handler that secures content behind a particular portion of a url space. The
	    // ConstraintSecurityHandler is a more specialized handler that allows matching of urls to different
	    // constraints. The server sets this as the first handler in the chain,
	    // effectively applying these constraints to all subsequent handlers in the chain.
	    ConstraintSecurityHandler security = new ConstraintSecurityHandler();
 //       server.setHandler(security);

        // This constraint requires authentication and in addition that an authenticated user be a member of a given
	    // set of roles for authorization purposes.
	    Constraint constraint = new Constraint();
	    constraint.setName("auth");
	    constraint.setAuthenticate(true);
	    constraint.setRoles(new String[]{"user", "admin"});

        // Binds a url pattern with the previously created constraint. The roles for this constraing mapping are
	    // mined from the Constraint itself although methods exist to declare and bind roles separately as well.
	    ConstraintMapping mapping = new ConstraintMapping();
	    mapping.setPathSpec("/*");
	    mapping.setConstraint(constraint);

        // First you see the constraint mapping being applied to the handler as a singleton list,
	    // however you can passing in as many security constraint mappings as you like so long as they follow the
	    // mapping requirements of the servlet api. Next we set a BasicAuthenticator instance which is the object
	    // that actually checks the credentials followed by the LoginService which is the store of known users, etc.
	    security.setConstraintMappings(Collections.singletonList(mapping));
	    security.setAuthenticator( useDigest ? new DigestAuthenticator() : new BasicAuthenticator());
	    security.setLoginService(loginService);

	    context.setSecurityHandler(security);

	// normal way to have two contexts
//	ContextHandlerCollection contexts = new ContextHandlerCollection();
//	contexts.setHandlers(new Handler[]{context, context2});
//	server.setHandler(contexts);
	
	// If no IP filtering, we can just add this context as the handler
	//server.setHandler(context);
	
	// Add a handler to filter by IP address.
	// http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/handler/IPAccessHandler.html
	// http://stackoverflow.com/questions/20660919/jetty-how-to-only-allow-requests-from-a-specific-domain
	IPAccessHandler ipaccess = new IPAccessHandler();
	    ipaccess.addWhite("127.0.0.1|/*");
	    if (whiteListAllowedIP != null) {
		for (String ip : whiteListAllowedIP) {
		    String filter = ip + "|/*";
//		    System.out.println(">>>> IP FILTER: " + filter);
		    ipaccess.addWhite(filter);
		}
	    }
	    // examples:
	 //ipaccess.addWhite("192.168.1.1-255|/*");
	    // ipaccess.addBlack("192.168.1.132|/home/*");
	    
	    // make context a subordinate of ipaccess
	    ipaccess.setHandler(context);
	    server.setHandler(ipaccess);
	    server.start();
	} catch (Exception e) {
	    e.printStackTrace();
	    this.server = null;
	    throw e;
	}
    }

    public void stopServer() throws Exception {
	// do this in a new Thread() ?
	server.setStopAtShutdown(true);
	server.setStopTimeout(3000);
	try {
	    server.stop();
	} catch (Exception e) {
	    e.printStackTrace();
	    throw e;
	}
    }

}
