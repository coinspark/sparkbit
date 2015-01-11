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

import java.io.File;
import java.util.Map;
import org.mapdb.*;
import org.multibit.controller.bitcoin.BitcoinController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ruffnex
 */
public enum SparkBitMapDB {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(SparkBitMapDB.class);

    // Filename of main database
    public static final String MAP_FILENAME = "sparkbit.mapdb";
    public static final String SPARKBITDB_SEND_TX_TO_COINSPARK_ADDRESS_MAP_NAME = "sendTxToCoinSparkAddressMap";
    public static final String SPARKBITDB_WALLETINFODATA = "walletInfoDataMap";
    public static final String SPARKBITDB_TXID_PAYMENTREF_MAP_NAME = "txidPaymentRefMap";
    
    
    private DB mapDB;
    
    private BitcoinController bitcoinController;

    // initialize this singleton
    public void initialize(BitcoinController controller) {
        this.bitcoinController = controller;
	String path = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + MAP_FILENAME;
	mapDB = DBMaker.newFileDB(new File(path)).closeOnJvmShutdown().make();
	log.debug("mapDB file path is: " + path);
      
      Map infoMap = getWalletInfoDataMap();
      int n = infoMap.size();
      log.debug("wallet info map size = " + n);
      log.debug("... keys = " + infoMap.keySet());
    }
    
  public DB getMapDB() {
      return mapDB;
  }
  
  
  public Map getSendTransactionToCoinSparkAddressMap() {
      return mapDB.getHashMap(SPARKBITDB_SEND_TX_TO_COINSPARK_ADDRESS_MAP_NAME);
  }
  
    public Map getTransactionPaymentRefMap() {
	return mapDB.getHashMap(SPARKBITDB_TXID_PAYMENTREF_MAP_NAME);
    }
  
  public HTreeMap<String, byte[]> getWalletInfoDataMap() {
      return mapDB.getHashMap(SPARKBITDB_WALLETINFODATA);
  }
  
}
