/* 
 * SparkBit
 *
 * Copyright 2015 Coin Sciences Ltd
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

import org.multibit.viewsystem.swing.*;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.model.bitcoin.WalletData;

import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.PeerGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum RetrieveMessagesService {

    INSTANCE;

    public static final int INTERVAL = 5000; // milliseconds

    private static Logger log = LoggerFactory.getLogger(RetrieveMessagesService.class);

    //private Controller controller;
    private BitcoinController bitcoinController;

//    private boolean updateTransactions = false;
//    private boolean isCurrentlyUpdating = false;
    //private ExecutorService pool; // single executor thread, tasks processed in sequence
    private ScheduledThreadPoolExecutor pool;

    public void initalize(BitcoinController bitcoinController) {
	//this.controller = bitcoinController;
	this.bitcoinController = bitcoinController;
	this.pool = new ScheduledThreadPoolExecutor(1); //Executors.newSingleThreadExecutor();

	try {
	    this.pool.scheduleWithFixedDelay(getRunnable(), INTERVAL, INTERVAL, TimeUnit.MILLISECONDS);
	} catch (Exception e) {
	    // uh-oh
	}
    }

    //@Override
//    public void cancel() {
//        //this.pool.shutdown();
//        this.pool.shutdownNow();
//        //return super.cancel();
//    }
    public boolean cancelAndAwaitTermination() {
	boolean result = false;
	try {
	    this.pool.shutdownNow();
	    result = this.pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
	} catch (InterruptedException e) {
	    //
	}
	return result;
    }

//    @Override
    public Runnable getRunnable() {
	return new Runnable() {
	    @Override
	    public void run() {
		log.debug("RetrieveMessagesService fired and executing");

		if (pool.isShutdown()) {
		    return;
		}

		try {
		    List<WalletData> perWalletModelDataList = bitcoinController.getModel().getPerWalletModelDataList();
		    if (perWalletModelDataList != null) {
			// iterator can throw ConcurrentModificationException so we iterate over copy
			List<WalletData> shallowCopy = new ArrayList<>(perWalletModelDataList);
			Iterator<WalletData> iterator = shallowCopy.iterator();
			while (iterator.hasNext()) {
			    if (pool.isShutdown()) {
				return;
			    }
			    try {
				WalletData wd = iterator.next();
				List<WalletData> latest = bitcoinController.getModel().getPerWalletModelDataList();
				if (latest == null || !latest.contains(wd)) {
				    continue; // this wallet data no longer exists in model so carry on looping 
				}
				if (wd != null) {

				    Wallet w = wd.getWallet();
				    if (w == null) {
					log.debug("getWallet() returned null");
				    } else {
					if (pool.isShutdown()) {
					    return;
					}
					w.CS.retrieveMessages();
				    }
				}
			    } catch (java.util.ConcurrentModificationException cme) {
//			    log.debug("Caught ConcurrentModificationException: " + cme.getMessage());
			    }
			}
		    }
		} catch (Exception e) {
		    // if uncaught, an exception will kill the scheduled executor
		    log.error("Retrieve messages service: Caught exception: " + e);
		    e.printStackTrace();
		}
	    }
	};
    }

}
