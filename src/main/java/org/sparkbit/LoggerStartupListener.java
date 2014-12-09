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
package org.sparkbit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import java.io.File;

/**
 * Setting java system property did not work in dynamically setting log file location.
 * Using a listener to set the context property does work, as mentioned here:
 * http://stackoverflow.com/questions/1975939/read-environment-variables-from-logback-configuration-file?rq=1
 * Setting this programmatically without using configuration file did not work.
 * http://stackoverflow.com/questions/7824620/logback-set-log-file-name-programatically
 * http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically
 * http://stackoverflow.com/questions/2011929/switch-from-log4j-to-logback
 * Java logging is painful.
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private static final String JSONRPC_LOG_FILENAME_PROPERTY = "jsonrpc_server_log_file";
    private static final String JSONRPC_LOG_FILENAME = "jsonrpc.log";

    private boolean started = false;

    @Override
    public void start() {
        if (started) return;
	ApplicationDataDirectoryLocator applicationDataDirectoryLocator = new ApplicationDataDirectoryLocator();
	String filename = applicationDataDirectoryLocator.getApplicationDataDirectory() + File.separator
                        + SparkBitInExecutableJar.OUTPUT_DIRECTORY + File.separator + JSONRPC_LOG_FILENAME;
        Context context = getContext();
        context.putProperty(JSONRPC_LOG_FILENAME_PROPERTY, filename);
        started = true;
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public void onReset(LoggerContext context) {
    }

    @Override
    public void onStop(LoggerContext context) {
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }
    
}