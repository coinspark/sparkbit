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
package org.sparkbit;

//import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/* CoinSpark START */
// We want to be able to turn off logging, when given a parameter.
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import org.coinspark.core.CSLogger;
/* CoinSpark END */

/**
 * Main SparkBitInExecutableJar entry class for when running in an executable jar - put console
 output to a file
 */
public final class SparkBitInExecutableJar {

    public static final String OUTPUT_DIRECTORY = "log";
    public static final String CONSOLE_OUTPUT_FILENAME = "sparkbit.log";

    private static org.slf4j.Logger log = LoggerFactory.getLogger(SparkBitInExecutableJar.class);

    /**
     * Utility class should not have a public constructor
     */
    private SparkBitInExecutableJar() {
    }

    /**
     * Start multibit user interface when running in a jar.
     * This will adjust the logging framework output to ensure that console output is sent
     * to a file appender in the client.
     * @param args The optional command line arguments ([0] can be a Bitcoin URI
     */
    public static void main(String args[]) {
        // Redirect the console output to a file.
        PrintStream fileStream;
        try {
            // Get the current data directory
            ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator();
            
            String outputDirectory;
            String consoleOutputFilename;
            
            if ("".equals(applicationDataDirectoryLocator.getApplicationDataDirectory())) {
                // Use defaults
                outputDirectory = OUTPUT_DIRECTORY;
                consoleOutputFilename = OUTPUT_DIRECTORY + File.separator + CONSOLE_OUTPUT_FILENAME;
            } else {
                // Use defined data directory as the root
                outputDirectory = applicationDataDirectoryLocator.getApplicationDataDirectory() + File.separator
                        + OUTPUT_DIRECTORY;
                consoleOutputFilename = applicationDataDirectoryLocator.getApplicationDataDirectory() + File.separator
                        + OUTPUT_DIRECTORY + File.separator + CONSOLE_OUTPUT_FILENAME;
            }
            
            log = LoggerFactory.getLogger(SparkBitInExecutableJar.class);

            // create output directory
            (new File(outputDirectory)).mkdir();

            // create output console log
            (new File(consoleOutputFilename)).createNewFile();

            fileStream = new PrintStream(new FileOutputStream(consoleOutputFilename, true));

            if (fileStream != null) {
                // Redirecting console output to file
                System.setOut(fileStream);
                // Redirecting runtime exceptions to file
                System.setErr(fileStream);
            }
	    
	    
	    /* CoinSpark START */
	    // We do this after all the other setup, so that if there is an error, it does get logged.
	    /*
	     Logback.xml defines the level of messages which be processed for the root logger
	     and other subsystems.  We can change this at runtime via an argument.
	     This beast of logging was tamed by referring to:
	     http://stackoverflow.com/questions/3837801/how-to-change-root-logging-level-programmatically
	     http://logback.qos.ch/manual/architecture.html#LoggerContext
	     http://stackoverflow.com/questions/2621701/setting-log-level-of-message-at-runtime-in-slf4j
	     Official docs: http://logback.qos.ch/manual/configuration.html
	     Who knew logging could be so fun...?!
	     */
	    // Iterate over args, find log=xxx and set log (last one wins), and build arg list without log args.
	    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	    List<String> listArgs = new ArrayList<String>(Arrays.asList(args));
	    List<String> cleanArgs = new ArrayList<>();
	    boolean customLevel = false;
	    for (String arg : listArgs) {
		String s = arg.toLowerCase();
		if (!s.startsWith("log=")) {
		    cleanArgs.add(arg);
		}
		if (s.equals("log=off")) {
		    root.setLevel(Level.OFF);
		    customLevel = true;
		} else if (s.equals("log=error")) {
		    root.setLevel(Level.ERROR);
		    customLevel = true;
		} else if (s.equals("log=warn")) {
		    root.setLevel(Level.WARN);
		    customLevel = true;
		} else if (s.equals("log=info")) {
		    root.setLevel(Level.INFO);
		    customLevel = true;
		} else if (s.equals("log=debug")) {
		    root.setLevel(Level.DEBUG);
		    customLevel = true;
		}
	    }

	    /*
	     Override logback.xml settings to whatever root is
	    
	     <logger name="org.multibit" level="DEBUG" />
	     <logger name="com.google.bitcoin" level="INFO" />
	     <logger name="com.google.bitcoin.core.Wallet" level="DEBUG" />
	     <logger name="com.google.bitcoin.utils.Threading" level="ERROR" />
	     */
	    if (customLevel == true) {
		Logger l = (Logger) LoggerFactory.getLogger("org.multibit");
		l.setLevel(root.getLevel());
		l = (Logger) LoggerFactory.getLogger("com.google.bitcoin");
		l.setLevel(root.getLevel());
		l = (Logger) LoggerFactory.getLogger("com.google.bitcoin.core.Wallet");
		l.setLevel(root.getLevel());
		l = (Logger) LoggerFactory.getLogger("com.google.bitcoin.utils.Threading");
		l.setLevel(root.getLevel());
		l = (Logger) LoggerFactory.getLogger("org.coinspark.core.CSUtils");
		l.setLevel(root.getLevel());
		// If CSLogger has a null filename, we can set the level and filter
		// otherwise it just writes to a specified file.
//		l = (Logger) LoggerFactory.getLogger(CSLogger.class);
//		l.setLevel(root.getLevel());
	    }
	    
	    args = cleanArgs.toArray(new String[cleanArgs.size()]);
	    /* CoinSpark END */

        } catch (FileNotFoundException e) {
            if (log != null) {
                log.error("Error in IO Redirection", e);
            }
        } catch (Exception e) {
            // Gets printed in the file.
            if (log != null) {
                log.debug("Error in redirecting output & exceptions to file", e);
            }
        } finally {
            // call the main SparkBitInExecutableJar code
            SparkBit.main(args);
        }
    }
}
