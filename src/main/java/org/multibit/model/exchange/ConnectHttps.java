/* 
 * SparkBit
 *
 * Copyright 2011-2014 multibit.org
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
package org.multibit.model.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import java.util.Arrays;

public final class ConnectHttps {
    private static final Logger log = LoggerFactory.getLogger(ConnectHttps.class);

    public static void main(String[] args) throws Exception {

        trustAllCerts();

//        httpGet("https://multibit.org/version.txt");
//        httpGet("https://data.mtgox.com/api/2/BTCUSD/money/ticker");
    }

    /**
     * Utility class should not have a public constructor
     */
    private ConnectHttps() {
    }

    public static void trustAllCerts() {
        /*
         * fix for Exception in thread "main"
         * javax.net.ssl.SSLHandshakeException:
         * sun.security.validator.ValidatorException: PKIX path building failed:
         * sun.security.provider.certpath.SunCertPathBuilderException: unable to
         * find valid certification path to requested target
         */
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                log.debug("checkServerTrusted authType = {}", authType);
            }

        } };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
	    
	    SSLContext context = SSLContext.getDefault();
	    SSLSocketFactory sf = context.getSocketFactory();
	    String[] cipherSuites = sf.getDefaultCipherSuites();
	    log.info("SSLSocketFactory default ciphersuites = " + Arrays.toString(cipherSuites));

            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    log.debug("hostname = " + hostname);
                    log.debug("SSLSession = " + session);
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyException e) {
            e.printStackTrace();
        }
    }

    private static void httpGet(String urlString) {
        URL url;
        try {
            System.out.println("\n" + urlString);
            url = new URL(urlString);

            URLConnection con = url.openConnection();
            Reader reader = new InputStreamReader(con.getInputStream());
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                System.out.print((char) ch);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
