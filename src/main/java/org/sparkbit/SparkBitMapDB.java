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
import java.nio.file.Files;
import java.util.Map;
import org.mapdb.*;
import org.multibit.controller.bitcoin.BitcoinController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import org.h2.mvstore.*;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


/**
 * For H2 database connections, we should use server mode so non-embedded
 * connections are allowed.
 * http://stackoverflow.com/questions/12401991/h2-database-multiple-connections
 *
 * Multiple processes can access the same database without having to start the
 * server manually. To do that, append ;AUTO_SERVER=TRUE to the database URL.
 * You can use the same database URL independent of whether the database is
 * already open or not. This feature doesn't work with in-memory databases.
 */
public enum SparkBitMapDB {
    INSTANCE;
 
    private static final Logger log = LoggerFactory.getLogger(SparkBitMapDB.class);

    // Filename of old MapDB data file
    public static final String MAP_FILENAME = "sparkbit.mapdb";
    
    // Filename of new H2 key-value store
    public static final String H2_KVSTORE_FILENAME = "sparkbit_h2_kvstore";

    // Name of map storing txid->coinsparkAddress
    public static final String SPARKBITDB_SEND_TX_TO_COINSPARK_ADDRESS_MAP_NAME = "sendTxToCoinSparkAddressMap";
//    public static final String SPARKBITDB_WALLETINFODATA = "walletInfoDataMap";
//    public static final String SPARKBITDB_TXID_PAYMENTREF_MAP_NAME = "txidPaymentRefMap";
    
    private MVStore h2KVStore;
    private MVMap<String, String> h2Map;
    private Connection conn;
        
    private BitcoinController bitcoinController;

    // initialize this singleton
    public void initialize(BitcoinController controller) {
        this.bitcoinController = controller;

	String mapPath = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + MAP_FILENAME;

	File mapDBFile = new File(mapPath);
	boolean mapExists = mapDBFile.exists();
	
	String h2KVStorePath = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + H2_KVSTORE_FILENAME;
	File h2StoreFile = new File(h2KVStorePath);
	boolean h2KVStoreExists = h2StoreFile.exists();
	
	h2KVStore = MVStore.open(h2KVStorePath);
	h2Map = null;
	if (h2KVStore != null) {
	    h2Map = h2KVStore.openMap(SPARKBITDB_SEND_TX_TO_COINSPARK_ADDRESS_MAP_NAME);
	}
	
	// Migrate if necessary
	if (!h2KVStoreExists && mapExists) {
	    migrateMapDBtoH2DB();
	}
    }
    
    /**
     * Migrate from old .mapdb data file to new H2 store database.
     */
    private void migrateMapDBtoH2DB() {
	String mapPath = this.bitcoinController.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + MAP_FILENAME;
	DB mapDB = DBMaker.newFileDB(new File(mapPath)).closeOnJvmShutdown().make();
	log.debug("Found legacy mapDB file at: " + mapPath);
	Map<String, String> m = mapDB.getHashMap(SPARKBITDB_SEND_TX_TO_COINSPARK_ADDRESS_MAP_NAME);
	for (Map.Entry<String, String> entrySet : m.entrySet()) {
	    Object key = entrySet.getKey();
	    Object value = entrySet.getValue();
	    String txid = (String) key;
	    String csa = (String) value;
	    log.debug("...migrating to H2: txid=" + txid + ", address=" + csa);

	    h2Map.put(txid, csa);
	}

	h2KVStore.commit();
	try {
	    Files.move(Paths.get(mapPath), Paths.get(mapPath + ".deprecated"), StandardCopyOption.REPLACE_EXISTING);
	} catch (Exception e) {
	    // FIXME: Process error
	}
    }
    
    /**
     * Cleanup and shutdown keystore and database connections.
     */
    public void shutdown() {
	if (h2KVStore!=null) {
	    h2KVStore.commit();
	    h2KVStore.close();
	}
	
	if (conn!=null) {
	    try {
	    if (!conn.isClosed())
		conn.commit();
		conn.close();
	    }
	    catch (SQLException e) {
		e.printStackTrace();
	    }
	}
    }

  /**
   * Convenience method to record the CoinSpark address for a transaction's recipient.
   * @param txid
   * @param address 
   */
  public void putSendCoinSparkAddressForTxid(String txid, String address ) {
      h2Map.put(txid, address);
      h2KVStore.commit();
  }
  
  /**
   * Convenience method to get a transaction recipient's CoinSpark address
   * @param txid
   * @return 
   */
  public String getSendCoinSparkAddressForTxid(String txid) {
      return h2Map.get(txid);
  }
  
}
