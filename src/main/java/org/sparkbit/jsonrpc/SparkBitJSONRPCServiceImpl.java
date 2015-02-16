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

import org.multibit.controller.bitcoin.BitcoinController;
import org.sparkbit.jsonrpc.autogen.*;
import java.util.List;
import java.util.ArrayList;
import org.multibit.model.bitcoin.WalletData;
import com.google.bitcoin.core.*;
//import java.util.HashMap;
import java.util.Iterator;
//import org.apache.commons.codec.digest.DigestUtils;
import org.coinspark.protocol.CoinSparkAddress;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;
import org.multibit.utils.CSMiscUtils;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.model.bitcoin.WalletAddressBookData;
import org.multibit.model.bitcoin.WalletInfoData;
import org.multibit.file.FileHandler;
import java.io.*;
import org.multibit.file.BackupManager;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.wallet.DefaultCoinSelector;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import org.coinspark.protocol.CoinSparkGenesis;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.sparkbit.SBEvent;
import org.sparkbit.SBEventType;
import org.multibit.network.MultiBitService;
import org.multibit.store.MultiBitWalletVersion;
import java.util.Date;
import org.multibit.file.WalletSaveException;
import static org.multibit.model.bitcoin.WalletAssetComboBoxModel.NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
import java.text.SimpleDateFormat;
import static org.multibit.network.MultiBitService.WALLET_SUFFIX;
import org.sparkbit.utils.FileNameCleaner;
import org.apache.commons.io.FilenameUtils;
import java.util.TimeZone;
import org.multibit.viewsystem.swing.action.ExitAction;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import org.sparkbit.SparkBitMapDB;
import org.apache.commons.io.FileDeleteStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import org.coinspark.wallet.CSBalance;
import org.coinspark.wallet.CSTransactionOutput;
import java.util.HashSet;
import org.coinspark.core.CSExceptions;
import org.coinspark.protocol.*;


/**
 * For now, synchronized access to commands which mutate
 */
public class SparkBitJSONRPCServiceImpl implements sparkbit {
    
    private static final Logger log = LoggerFactory.getLogger(SparkBitJSONRPCServiceImpl.class);

    // Limit on the number of addresses you can create in one call
    public static final int CREATE_ADDRESSES_LIMIT = 100;
    
    // How many milliseconds before we shutdown via ExitAction
    public static final int SHUTDOWN_DELAY = JettyEmbeddedServer.GRACEFUL_SHUTDOWN_PERIOD + 1000;
    
    private BitcoinController controller;
//    private MultiBitFrame mainFrame;
//    private ConcurrentHashMap<String,String> walletFilenameMap;
    
    private Timer stopTimer;
    
    public SparkBitJSONRPCServiceImpl() {
	this.controller = JSONRPCController.INSTANCE.getBitcoinController();
//	this.mainFrame = JSONRPCController.INSTANCE.getMultiBitFrame();
//	walletFilenameMap = new ConcurrentHashMap<>();
//	updateWalletFilenameMap();
    }

    // Quit after a delay so we can return true to JSON-RPC client.
    @Override
    public Boolean stop() throws com.bitmechanic.barrister.RpcException {
	log.info("STOP");
	log.info("Shutting down JSON-RPC server, will wait " + SHUTDOWN_DELAY + " milliseconds before instructing SparkBit to exit.");
	
	final BitcoinController myController = this.controller;
	stopTimer = new Timer();
	stopTimer.schedule(new TimerTask() {
	    @Override
	    public void run() {
		ExitAction exitAction = new ExitAction(myController, null);
		exitAction.setBitcoinController(myController);
		exitAction.actionPerformed(null);
	    }
	}, SHUTDOWN_DELAY);

	// Signal the server to stop
	Executors.newSingleThreadExecutor().execute(new Runnable() {
	    @Override
	    public void run() {
		JSONRPCController.INSTANCE.stopServer();
	    }
	});
	return true;
    }
	
    /*
    Get status on a per wallet basis.
    */
    @Override
    public JSONRPCStatusResponse getstatus() throws com.bitmechanic.barrister.RpcException {
	log.info("GET STATUS");
//	boolean replayTaskRunning = ReplayManager.INSTANCE.getCurrentReplayTask()!=null ;
//	boolean regularDownloadRunning = ReplayManager.isRegularDownloadRunning();
//	boolean synced = !regularDownloadRunning && !replayTaskRunning;
	
	// A regular download can be running in the background, because there is a new block,
	// and thus we are behind by one block, but the UI will still show that we synced.
	// We will consider ourselves to be out of sync if we are two or more blocks behind our peers.
//	PeerGroup pg = controller.getMultiBitService().getPeerGroup();
//	if (pg!=null) {
//	    Peer peer = pg.getDownloadPeer();
//	    if (peer != null) {
//		int n = peer.getPeerBlockHeightDifference();
//		if (synced && n>=2) {
//		    synced = false;
//		}
//	    }
//	}
	int mostCommonChainHeight = controller.getMultiBitService().getPeerGroup().getMostCommonChainHeight();
	//int bestChainHeight = controller.getMultiBitService().getChain().getBestChainHeight();

	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
	List<JSONRPCWalletStatus> wallets = new ArrayList<JSONRPCWalletStatus>();
	if (perWalletModelDataList != null) {
	    for (WalletData wd : perWalletModelDataList) {
		Wallet w = wd.getWallet();
		long lastSeenBlock = w.getLastBlockSeenHeight();
		// getLastBlockSeenHeight() returns -1 if the wallet doesn't have this data yet
		if (lastSeenBlock==-1) lastSeenBlock = mostCommonChainHeight;
		
		boolean synced = (lastSeenBlock == mostCommonChainHeight);
		
//		log.debug(">>>> *** description = " + wd.getWalletDescription());
//		log.debug(">>>> last block = " + lastSeenBlock);
//		log.debug(">>>> mostCommonChainHeight = " + mostCommonChainHeight);
//		log.debug(">>>> replay UUID = " + wd.getReplayTaskUUID());
//		log.debug(">>>> busy key = " + wd.getBusyTaskKey());
		if (wd.getReplayTaskUUID() != null) {
		    synced = false;
		} else if (wd.isBusy()) {
		    String key = wd.getBusyTaskKey();
		    if (key.equals("multiBitDownloadListener.downloadingText") || key.equals("singleWalletPanel.waiting.text")) {
			synced = false;
		    }
		}
		String filename = wd.getWalletFilename();
		String base = FilenameUtils.getBaseName(filename);
		JSONRPCWalletStatus ws = new JSONRPCWalletStatus(base, synced, lastSeenBlock);
		wallets.add(ws);
	    }
	}
	
	String versionNumber = controller.getLocaliser().getVersionNumber();
	int numConnectedPeers = controller.getMultiBitService().getPeerGroup().numConnectedPeers();
	boolean isTestNet = controller.getMultiBitService().isTestNet3();
	JSONRPCStatusResponse resp = new JSONRPCStatusResponse();
	resp.setVersion(versionNumber);
	resp.setConnections((long)numConnectedPeers);
	resp.setTestnet(isTestNet);
	JSONRPCWalletStatus[] x = wallets.toArray(new JSONRPCWalletStatus[0]);
	resp.setWallets( x );
	return resp;
    }
    
    @Override
    public String[] listwallets() throws com.bitmechanic.barrister.RpcException {
	log.info("LIST WALLETS");
	
	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
	List<String> names = new ArrayList<String>();
	if (perWalletModelDataList != null) {
	    for (WalletData loopPerWalletModelData : perWalletModelDataList) {
		String filename = loopPerWalletModelData.getWalletFilename();
		String base = FilenameUtils.getBaseName(filename);
		names.add(base);
		
		// store/update local cache
		//walletFilenameMap.put(digest, filename);
	    }
	}

	String[] resultArray = names.toArray(new String[0]);
	return resultArray;
    }
    
    @Override
    public synchronized Boolean createwallet(String name) throws com.bitmechanic.barrister.RpcException {
	log.info("CREATE WALLET");
	log.info("wallet name = " + name);
	
	boolean isNameSane = sanityCheckName(name);
	if (!isNameSane) {
	    JSONRPCError.WALLET_NAME_BAD_CHARS.raiseRpcException();
	}
	if (name.startsWith("-") || name.startsWith("_")) {
	    JSONRPCError.WALLET_NAME_BEGINS_WITH_SYMBOL.raiseRpcException();
	}
	

	String newWalletFilename = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + name + MultiBitService.WALLET_SUFFIX;
	File newWalletFile = new File(newWalletFilename);
	if (newWalletFile.exists()) {
	    JSONRPCError.WALLET_ID_ALREADY_EXISTS.raiseRpcException();
	}
	
	LocalDateTime dt = new DateTime().toLocalDateTime();
	String description = name + " (jsonrpc " + dt.toString("YYYY-MM-DD HH:mm" + ")"); //.SSS");
	
	// Create a new wallet - protobuf.2 initially for backwards compatibility.
	try {
                Wallet newWallet = new Wallet(this.controller.getModel().getNetworkParameters());

                ECKey newKey = new ECKey();
                newWallet.addKey(newKey);
                WalletData perWalletModelData = new WalletData();
		/* CoinSpark START */
		// set default address label for first key
		WalletInfoData walletInfo = new WalletInfoData(newWalletFilename, newWallet, MultiBitWalletVersion.PROTOBUF);
		NetworkParameters networkParams = this.controller.getModel().getNetworkParameters();
		Address defaultReceivingAddress = newKey.toAddress(networkParams);
		walletInfo.getReceivingAddresses().add(new WalletAddressBookData("Default Address", defaultReceivingAddress.toString()));
	         perWalletModelData.setWalletInfo(walletInfo);
		/* CoinSpark END */
	
                perWalletModelData.setWallet(newWallet);
                perWalletModelData.setWalletFilename(newWalletFilename);
                perWalletModelData.setWalletDescription(description);
                this.controller.getFileHandler().savePerWalletModelData(perWalletModelData, true);

                // Start using the new file as the wallet.
                this.controller.addWalletFromFilename(newWalletFile.getAbsolutePath());
		// We could select the new wallet if we wanted to.
		//this.controller.getModel().setActiveWalletByFilename(newWalletFilename);
                //controller.getModel().setUserPreference(BitcoinModel.GRAB_FOCUS_FOR_ACTIVE_WALLET, "true");

                // Save the user properties to disk.
                FileHandler.writeUserPreferences(this.controller);
                //log.debug("User preferences with new wallet written successfully");

                // Backup the wallet and wallet info.
                BackupManager.INSTANCE.backupPerWalletModelData(controller.getFileHandler(), perWalletModelData);
                
                controller.fireRecreateAllViews(true);
                controller.fireDataChangedUpdateNow();
	} catch (Exception e) {
	    JSONRPCError.CREATE_WALLET_FAILED.raiseRpcException();
	    //JSONRPCError.throwAsRpcException("Could not create wallet", e);
	}

//	updateWalletFilenameMap();
	return true;
    }
	
    @Override
    public synchronized Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException {
	log.info("DELETE WALLET");
	log.info("wallet name = " + walletID);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w == null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	// Are there any asset or btc balances?  Do not allow deleting wallet if any balance exists?
	Map<Integer, Wallet.CoinSpark.AssetBalance> map = w.CS.getAllAssetBalances();
	for (Wallet.CoinSpark.AssetBalance ab : map.values()) {
	    if (ab.total.compareTo(BigInteger.ZERO)>0) {
		JSONRPCError.DELETE_WALLET_NOT_EMPTY.raiseRpcException();		
	    }
	}

	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
WalletInfoData winfo = wd.getWalletInfo();
	if (wd.isBusy()) {
	    JSONRPCError.WALLEY_IS_BUSY.raiseRpcException();
	}
	
	// Deleting a wallet even if not currently active, causes list of wallets to be unselected
        // so we need to keep track of what was active befor eremoval
	String activeFilename = this.controller.getModel().getActiveWalletFilename();
	if (wd.getWalletFilename().equals(activeFilename)) {
	    activeFilename = null;
	}
	
	// Unhook it from the PeerGroup.
	this.controller.getMultiBitService().getPeerGroup().removeWallet(w);

	// Remove it from the model.
      this.controller.getModel().remove(wd);


	// Perform delete if possible etc.
	FileHandler fileHandler = this.controller.getFileHandler();
	try {
	    fileHandler.deleteWalletAndWalletInfo(wd);
	    
	    // Delete .cs
	    String csassets = filename + ".csassets";
	    String csbalances = filename + ".csbalances";
	    File f = new File(csassets);
	    if (f.exists()) {
		if (!f.delete()) {
		    //log.error(">>>> Asset DB: Cannot delete");
		}
	    }
	    f = new File(csbalances);
	    if (f.exists()) {
		if (!f.delete()) {
		    //log.error(">>>> Balances DB: Cannot delete");
		}
	    }
	    String cslog = filename + ".cslog";
	    f = new File(cslog);
	    if (f.exists()) {
		if (!f.delete()) {
//                log.error(">>>> CS Log File: Cannot delete");
		}
	    }
	    
	    // Delete cached contracts
	    String csfiles = filename + ".csfiles";
	    f = new File(csfiles);
	    if (f.exists()) {
		FileDeleteStrategy.FORCE.delete(f);
	    }

	    // Delete the backup folder and cached contracts
	    String backupFolderPath = BackupManager.INSTANCE.calculateTopLevelBackupDirectoryName(new File(filename));
	    f = new File(backupFolderPath);
	    if (f.exists()) {
		FileDeleteStrategy.FORCE.delete(f);		
	    }
    
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Error deleting wallet files", e);
	}
	
	if (!winfo.isDeleted()) {
	    JSONRPCError.throwAsRpcException("Wallet was not deleted. Reason unknown.");
	}
	
    // Set the new Wallet to be the old active wallet, or the first wallet
      if (activeFilename!=null) {
	  this.controller.getModel().setActiveWalletByFilename(activeFilename);
      } else if (!this.controller.getModel().getPerWalletModelDataList().isEmpty()) {
        WalletData firstPerWalletModelData = this.controller.getModel().getPerWalletModelDataList().get(0);
        this.controller.getModel().setActiveWalletByFilename(firstPerWalletModelData.getWalletFilename());
      }
      controller.fireRecreateAllViews(true);

	
//	updateWalletFilenameMap();
	return true;
    }

    /*
    Synchronized access: clear and recreate the wallet filename map.
    */
//    private synchronized void updateWalletFilenameMap() {
//	walletFilenameMap.clear();
//	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
//	if (perWalletModelDataList != null) {
//	    for (WalletData loopPerWalletModelData : perWalletModelDataList) {
//		String filename = loopPerWalletModelData.getWalletFilename();
//		String id = loopPerWalletModelData.getWalletDescription();
//		walletFilenameMap. put(id, filename);
////
////		String filename = loopPerWalletModelData.getWalletFilename();
////		String digest = DigestUtils.md5Hex(filename);
////		walletFilenameMap. put(digest, filename);
//	    }
//	}
//	
//    }
    
    /*
	Get full path for wallet given it's name
    */
    private String getFullPathForWalletName(String name) {
	String filename = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator
                + name + WALLET_SUFFIX;
	return filename;
    }

    /*
    Check to see if a name gets cleaned or not because it contains characters bad for a filename
    */
    private boolean sanityCheckName(String name) {
	String cleaned = FileNameCleaner.cleanFileNameForWallet(name);
	return (cleaned.equals(name));
    }
	
    private Wallet getWalletForWalletName(String walletID) {
	Wallet w = null;
	String filename = getFullPathForWalletName(walletID);
	if (filename != null) {
	    WalletData wd = controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	    if (wd!=null) {
		w = wd.getWallet();
	    }
	}
	return w;
    }
    
    // Helper function
    private boolean isAssetRefValid(String s) {
	if (s!=null) {
	    s = s.trim();
	    CoinSparkAssetRef assetRef = new CoinSparkAssetRef();
	    if (assetRef.decode(s)) {
		return true;
	    }
	}
	return false;
    }
    
    private CSAsset getAssetForAssetRefString(Wallet w, String assetRef) {
	CSAssetDatabase db = w.CS.getAssetDB();
	int[] assetIDs = w.CS.getAssetIDs();
	if (assetIDs != null) {
	    for (int id : assetIDs) {
		CSAsset asset = db.getAsset(id);
		if (asset != null) {
		    //CoinSparkAssetRef ref = asset.getAssetReference();
		    String s = CSMiscUtils.getHumanReadableAssetRef(asset);
		    if (s.equals(assetRef)) {
			return asset;
		    }
		}
	    }
	}
	return null;
    }
    
    @Override
    public synchronized Boolean setassetvisible(String walletID, String assetRef, Boolean visibility) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SET ASSET VISIBILITY");
	log.info("wallet name = " + walletID);
	log.info("asset ref   = " + assetRef);
	log.info("visibility  = " + visibility);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset == null) {
	    if (isAssetRefValid(assetRef)) {
		JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	    } else {
		JSONRPCError.ASSETREF_INVALID.raiseRpcException();		
	    }
	} else {
	    asset.setVisibility(visibility);
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_VISIBILITY_CHANGED, asset.getAssetID());
	}
	return true;
    }
    
    @Override
    public synchronized Boolean addasset(String walletID, String assetRefString) throws com.bitmechanic.barrister.RpcException {
	log.info("ADD ASSET");
	log.info("wallet name = " + walletID);
	log.info("asset ref   = " + assetRefString);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w == null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}

	String s = assetRefString;
	if ((s != null) && (s.length() > 0)) {
	    s = s.trim();

	    // Does the asset already exist? If so, return true.
	    if (getAssetForAssetRefString(w, s) != null) {
		return true;
	    }

	    //System.out.println("asset ref detected! " + s);
	    CoinSparkAssetRef assetRef = new CoinSparkAssetRef();
	    if (assetRef.decode(s)) {
//		Wallet wallet = this.controller.getModel().getActiveWallet();
		CSAssetDatabase assetDB = w.CS.getAssetDB();
		if (assetDB != null) {
		    CSAsset asset = new CSAsset(assetRef, CSAsset.CSAssetSource.MANUAL);
		    if (assetDB.insertAsset(asset) != null) {
			//System.out.println("Inserted new asset manually: " + asset);
		    } else {
			JSONRPCError.throwAsRpcException("Internal error, assetDB.insertAsset() failed for an unknown reason");
		    }
		}
	    } else {
		JSONRPCError.ASSETREF_INVALID.raiseRpcException();
	    }
	}

	return true;
    }
    
    
    @Override
    public Boolean deleteasset(String walletname, String assetRef) throws com.bitmechanic.barrister.RpcException {
	log.info("DELETE ASSET");
	log.info("wallet name = " + walletname);
	log.info("asset ref   = " + assetRef);
	
	Wallet w = getWalletForWalletName(walletname);
	boolean success = false;
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset != null) {
	    int assetID = asset.getAssetID();
	    BigInteger x = w.CS.getAssetBalance(assetID).total;
	    boolean canDelete = x.equals(BigInteger.ZERO);
	    
	    // Delete invalid asset if property allows
	    String s = controller.getModel().getUserPreference(BitcoinModel.CAN_DELETE_INVALID_ASSETS);
	    boolean isAssetInvalid = asset.getAssetState() != CSAsset.CSAssetState.VALID;
	    boolean deleteInvalidAsset = false;
	    if (Boolean.TRUE.toString().equals(s) && isAssetInvalid) {
		deleteInvalidAsset = true;
	    }
		
	    if (canDelete || deleteInvalidAsset) {
		success = w.CS.deleteAsset(asset);
		if (success) {
		    // Note: the event can be fired, but the listener can do nothing if in headless mode.
		    // We want main asset panel to refresh, since there isn't an event fired on manual reset.
		    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_DELETED, assetID);
		} else {
		    JSONRPCError.DELETE_ASSET_FAILED.raiseRpcException();
		}
	    } else {
		if (isAssetInvalid) {
		    JSONRPCError.DELETE_INVALID_ASSET_FAILED.raiseRpcException();
		} else {
		    JSONRPCError.DELETE_ASSET_NONZERO_BALANCE.raiseRpcException();
		}
	    }
	} else {
	    if (isAssetRefValid(assetRef)) {
		JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	    } else {
		JSONRPCError.ASSETREF_INVALID.raiseRpcException();		
	    }
	}
	return success;
    }
    
	
    @Override
    public synchronized Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
	log.info("REFRESH ASSET");
	log.info("wallet name = " + walletID);
	log.info("asset ref   = " + assetRef);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset != null) {
	    asset.setRefreshState();
			// Note: the event can be fired, but the listener can do nothing if in headless mode.
	    // We want main asset panel to refresh, since there isn't an event fired on manual reset.
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_UPDATED, asset.getAssetID());
	} else {
	    if (isAssetRefValid(assetRef)) {
		JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	    } else {
		JSONRPCError.ASSETREF_INVALID.raiseRpcException();		
	    }
	}

	return true;
    }

     @Override
   public JSONRPCAddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException
    {
	log.info("LIST ADDRESSES");
	log.info("wallet name = " + walletID);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	List<JSONRPCAddressBookEntry> addresses = new ArrayList<JSONRPCAddressBookEntry>();
	
	String address, sparkAddress, label;
	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	final WalletInfoData addressBook = wd.getWalletInfo();
            if (addressBook != null) {
                ArrayList<WalletAddressBookData> receivingAddresses = addressBook.getReceivingAddresses();
                if (receivingAddresses != null) {
		    Iterator<WalletAddressBookData> iter = receivingAddresses.iterator();
                    while (iter.hasNext()) {
                        WalletAddressBookData addressBookData = iter.next();
                        if (addressBookData != null) {
                            address = addressBookData.getAddress();
                            label = addressBookData.getLabel();

			    sparkAddress = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(address);
			    if (sparkAddress != null) {
				JSONRPCAddressBookEntry entry = new JSONRPCAddressBookEntry(label, address, sparkAddress);
				addresses.add(entry);
			    }
                        }
                    }
                }
            }
	
	JSONRPCAddressBookEntry[] resultArray = addresses.toArray(new JSONRPCAddressBookEntry[0]);
	return resultArray;
    }
    
    // TODO: Should we remove limit of 100 addresses?
    @Override
    public synchronized JSONRPCAddressBookEntry[] createaddresses(String walletID, Long quantity) throws com.bitmechanic.barrister.RpcException {
	log.info("CREATE ADDRESSES");
	log.info("wallet name = " + walletID);
	log.info("quantity    = " + quantity);

	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}

	int qty = quantity.intValue();
	if (qty <= 0) {
	    JSONRPCError.CREATE_ADDRESS_TOO_FEW.raiseRpcException();
	}
	if (qty > CREATE_ADDRESSES_LIMIT) {
	    JSONRPCError.CREATE_ADDRESS_TOO_MANY.raiseRpcException();
	}

	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	if (wd.isBusy()) {
	    JSONRPCError.WALLEY_IS_BUSY.raiseRpcException();
	} else {
	    wd.setBusy(true);
	    wd.setBusyTaskKey("jsonrpc.busy.createaddress");
	    this.controller.fireWalletBusyChange(true);
	}

	List<JSONRPCAddressBookEntry> addresses = new ArrayList<JSONRPCAddressBookEntry>();

	try {
	    List<ECKey> newKeys = new ArrayList<ECKey>();
	    for (int i = 0; i < qty; i++) {
		ECKey newKey = new ECKey();
		newKeys.add(newKey);
	    }

	    FileHandler fileHandler = this.controller.getFileHandler();

	    synchronized (wd.getWallet()) {
		wd.getWallet().addKeys(newKeys);
	    }

	    // Recalculate the bloom filter.
	    if (this.controller.getMultiBitService() != null) {
		this.controller.getMultiBitService().recalculateFastCatchupAndFilter();
	    }

	    // Add keys to address book.
	    int n = 1, count = newKeys.size();
	    for (ECKey newKey : newKeys) {
		String lastAddressString = newKey.toAddress(this.controller.getModel().getNetworkParameters()).toString();
		LocalDateTime dt = new DateTime().toLocalDateTime();
		String label = "Created on " + dt.toString("d MMM y, HH:mm:ss z") + " (" + n++ + " of " + count + ")";
		// unlikely address is already present, we don't want to update the label
		wd.getWalletInfo().addReceivingAddress(new WalletAddressBookData(label, lastAddressString),
			false);

		// Create structure for JSON response
		String sparkAddress = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(lastAddressString);
		if (sparkAddress == null) sparkAddress = "Internal error creating CoinSparkAddress from this Bitcoin address";
		JSONRPCAddressBookEntry entry = new JSONRPCAddressBookEntry(label, lastAddressString, sparkAddress);
		addresses.add(entry);
	    }

	    // Backup the wallet and wallet info.
	    BackupManager.INSTANCE.backupPerWalletModelData(fileHandler, wd);
	} catch (KeyCrypterException e) {
	    JSONRPCError.throwAsRpcException("Create addresses failed with KeyCrypterException", e);
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Create addresses failed", e);
	} finally {
	    // Declare that wallet is no longer busy with the task.
	    wd.setBusyTaskKey(null);
	    wd.setBusy(false);
	    this.controller.fireWalletBusyChange(false);
	}

	
	CSEventBus.INSTANCE.postAsync(new SBEvent(SBEventType.ADDRESS_CREATED));
	wd.setDirty(true);
	
	JSONRPCAddressBookEntry[] resultArray = addresses.toArray(new JSONRPCAddressBookEntry[0]);
	return resultArray;
    }

    
    @Override
    public synchronized Boolean setaddresslabel(String walletID, String address, String label) throws com.bitmechanic.barrister.RpcException {
	log.info("SET ADDRESS LABEL");
	log.info("wallet name = " + walletID);
	log.info("address     = " + address);
	log.info("label       = " + label);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	if (address.startsWith("s")) {
	    address = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
	    if (address==null) {
		JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException();
	    }
	}
	
	if (label==null) label=""; // this shouldn't happen when invoked via barrister
	
	boolean success = false;

	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	final WalletInfoData addressBook = wd.getWalletInfo();
	if (addressBook != null) {
	    ArrayList<WalletAddressBookData> receivingAddresses = addressBook.getReceivingAddresses();
	    if (receivingAddresses != null) {
		Iterator<WalletAddressBookData> iter = receivingAddresses.iterator();
		while (iter.hasNext()) {
		    WalletAddressBookData addressBookData = iter.next();
		    if (addressBookData != null) {
			String btcAddress = addressBookData.getAddress();
			if (btcAddress.equals(address)) {
			    addressBookData.setLabel(label);
			    success = true;
			    break;
			}
		    }
		}
	    }
	}
  
	if (success) {
	    CSEventBus.INSTANCE.postAsync(new SBEvent(SBEventType.ADDRESS_UPDATED));
	    wd.setDirty(true);
	} else {
	    JSONRPCError.ADDRESS_NOT_FOUND.raiseRpcException();
	}
	
	return success;
    }
    
    @Override
    public JSONRPCTransaction[] listtransactions(String walletID, Long limit) throws com.bitmechanic.barrister.RpcException
    {
	log.info("LIST TRANSACTIONS");
	log.info("wallet name = " + walletID);
	log.info("limit #     = " + limit);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
//	if (limit>100) {
//	    JSONRPCError.LIST_TRANSACTIONS_TOO_MANY.raiseRpcException();
//	} else 
	    
	if (limit<0) {
	    JSONRPCError.LIST_TRANSACTIONS_TOO_FEW.raiseRpcException();
	}
	
	// if limit is 0 get them all
	
	List<JSONRPCTransaction> resultList = new ArrayList<JSONRPCTransaction>();
	
	
	int lastSeenBlock = controller.getMultiBitService().getChain().getBestChainHeight();

	List<Transaction> transactions = null;
	if (limit > 0) {
	    transactions = w.getRecentTransactions(limit.intValue(), false);
	} else {
	    transactions = w.getTransactionsByTime();
	}
	
	for (Transaction tx : transactions) {

	    Date txDate = controller.getModel().getDateOfTransaction(controller, tx);
	    long unixtime = txDate.getTime()/1000L; // unix epoch in seconds
	    long confirmations = 0;
	    try {
		confirmations = lastSeenBlock - tx.getConfidence().getAppearedAtChainHeight();
	    } catch (IllegalStateException e) {
	    }
	    boolean incoming = !tx.sent(w);
	    BigInteger feeSatoshis = tx.calculateFee(w);
	    Double fee = null;
	    if (!incoming) {
		BigDecimal feeBTC = new BigDecimal(feeSatoshis).divide(new BigDecimal(Utils.COIN));
		fee = -1.0 * feeBTC.doubleValue();
	    }
	    String txid = tx.getHashAsString();
	    ArrayList<JSONRPCTransactionAmount> amounts = getAssetTransactionAmounts(w, tx, true, false);
	    JSONRPCTransactionAmount[] amountsArray = amounts.toArray(new JSONRPCTransactionAmount[0]);

//	    int size = amounts.size();
//	    AssetTransactionAmountEntry[] entries = new AssetTransactionAmountEntry[size];
//	    int index = 0;
//	    for (JSONRPCTransactionAmount ata : amounts) {
//		entries[index++] = new AssetTransactionAmountEntry(ata.getAssetRef(), ata);
//	    }
	    
	    /* Get send and receive address */
	    TransactionOutput myOutput = null;
	    TransactionOutput theirOutput = null;
	    List<TransactionOutput> transactionOutputs = tx.getOutputs();
	    if (transactionOutputs != null) {
		for (TransactionOutput transactionOutput : transactionOutputs) {
		    if (transactionOutput != null && transactionOutput.isMine(w)) {
			myOutput = transactionOutput;
		    }
		    if (transactionOutput != null && !transactionOutput.isMine(w)) {
			// We have to skip the OP_RETURN output as there is no address and it result sin an exception when trying to get the destination address
			Script script = transactionOutput.getScriptPubKey();
			if (script != null) {
			    if (script.isSentToAddress() || script.isSentToP2SH()) {
				theirOutput = transactionOutput;
			    }
			}
		    }
		}
	    }
	    
	    // If this transaction was sent from addresses belonging to the same wallet
	    boolean toMyself = myOutput!=null && theirOutput==null;	    
	    boolean hasAssets = amounts.size()>1;
	    String myReceiveAddress = null;
	    String theirAddress = null;
	    
	    if (incoming || toMyself) {
		try {
		    if (myOutput != null) {
			Address toAddress = new Address(this.controller.getModel().getNetworkParameters(), myOutput.getScriptPubKey().getPubKeyHash());
			myReceiveAddress = toAddress.toString();
		    }
		    if (myReceiveAddress != null && hasAssets) {
			String s = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(myReceiveAddress);
			if (s != null) {
			    myReceiveAddress = s;
			}
		    }
		    // If to myself, lets see if CoinSpark or not
		    if (myReceiveAddress != null && toMyself) {
			String myCoinSparkAddress = SparkBitMapDB.INSTANCE.getSendCoinSparkAddressForTxid(tx.getHashAsString());
			if (myCoinSparkAddress!=null) {
			    myReceiveAddress = myCoinSparkAddress;
			}
		    }
		} catch (ScriptException e) {
		}
	    } else {
		// outgoing transaction
		 if (theirOutput != null) {
		    // First let's see if we have stored the recipient in our map
		    try {
			theirAddress = SparkBitMapDB.INSTANCE.getSendCoinSparkAddressForTxid(tx.getHashAsString());
		    } catch (Exception e) {
		    }

		     if (theirAddress == null) {
			 try {
			     theirAddress = theirOutput.getScriptPubKey().getToAddress(controller.getModel().getNetworkParameters()).toString();
			 } catch (ScriptException se) {
			 }
			 if (theirAddress != null & hasAssets) {
			     String s = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(theirAddress);
			     if (s != null) {
				 theirAddress = s;
			     }
			 }
		     }
		 }
	    }
	    
	    String address = "";
	    if ((incoming || toMyself) && myReceiveAddress!=null) {
		address = myReceiveAddress;
	    } else if (!incoming && theirAddress != null) {
		address = theirAddress;
	    }
	    
	    
	    String category = (incoming) ? "receive" : "send";

	    long paymentRef = CSMiscUtils.getPaymentRefFromTx(w, txid);
	    
	    JSONRPCTransaction atx = new JSONRPCTransaction(unixtime, confirmations, category, amountsArray, fee, txid, address, paymentRef);
	    resultList.add(atx);
	}
	
	JSONRPCTransaction[] resultArray = resultList.toArray(new JSONRPCTransaction[0]);
	return resultArray;
    }
    
    
    
    
    /*
    * Return array of asset transaction objects.
    * Original method is in CSMiscUtils, so any changes there, must be reflected here.
    */
    private ArrayList<JSONRPCTransactionAmount> getAssetTransactionAmounts(Wallet wallet, Transaction tx, boolean excludeBTCFee, boolean absoluteBTCFee) {
	if (wallet==null || tx==null) return null;
	
	Map<Integer, BigInteger> receiveMap = wallet.CS.getAssetsSentToMe(tx);
	Map<Integer, BigInteger> sendMap = wallet.CS.getAssetsSentFromMe(tx);

//	System.out.println(">>>> tx = " + tx.getHashAsString());
//	System.out.println(">>>>     receive map = " +  receiveMap);
//	System.out.println(">>>>     send map = " +  sendMap);
	
	//Map<String, String> nameAmountMap = new TreeMap<>();
	ArrayList<JSONRPCTransactionAmount> resultList = new ArrayList<>();
	
	boolean isSentByMe = tx.sent(wallet);
	Map<Integer, BigInteger> loopMap = (isSentByMe) ? sendMap : receiveMap;
	
//	Integer assetID = null;
	BigInteger netAmount = null;
	
//	for (Map.Entry<Integer, BigInteger> entry : loopMap.entrySet()) {
	for (Integer assetID : loopMap.keySet()) {
//	    assetID = entry.getKey();
	    if (assetID == null || assetID == 0) continue; // skip bitcoin

	    BigInteger receivedAmount = receiveMap.get(assetID); // should be number of raw units
	    BigInteger sentAmount = sendMap.get(assetID);
	    boolean isReceivedAmountMissing = (receivedAmount==null);
	    boolean isSentAmountMissing = (sentAmount==null);
	    
	    netAmount = BigInteger.ZERO;
	    if (!isReceivedAmountMissing) netAmount = netAmount.add(receivedAmount);
	    if (!isSentAmountMissing) netAmount = netAmount.subtract(sentAmount);
	    
	    if (isSentByMe && !isSentAmountMissing && sentAmount.equals(BigInteger.ZERO)) {
		// Catch a case where for a send transaction, the send amount for an asset is 0,
		// but the receive cmount is not 0.  Also the asset was not valid.
		continue;
	    }
	    
	    
	    CSAsset asset = wallet.CS.getAsset(assetID);
	    if (asset==null) {
		// something went wrong, we have asset id but no asset, probably deleted.
		// For now, we carry on, and we display what we know.
	    }	    
	    
	    if (netAmount.equals(BigInteger.ZERO) && isSentByMe) {
		// If net asset is 0 and this is our send transaction,
		// we don't need to show anything, as this probably due to implicit transfer.
		// So continue the loop.
		continue;
	    }
	    
	    if (netAmount.equals(BigInteger.ZERO) && !isSentByMe) {
		// Receiving an asset, where the value is 0 because its not confirmed yet,
		// or not known because asset files not uploaded so we dont know display format.
		// Anyway, we don't do anything here as we do want to display this incoming
		// transaction the best we can.
	    }
	    
//	    System.out.println(">>>>     isSentAmountMissing = " + isSentAmountMissing);
//	    System.out.println(">>>>     asset reference = " + asset.getAssetReference());
//	    System.out.println(">>>>     asset name = " + asset.getName());
	    
	    String name = null;
	    CoinSparkGenesis genesis = null;
	    boolean isUnknown = false;
	    if (asset!=null) {
		genesis = asset.getGenesis();
		name = asset.getNameShort(); // could return null?
	    }
	    if (name == null) {
		isUnknown = true;
		if (genesis!=null) {
		    name = "Asset from " + genesis.getDomainName();
		} else {
		    // No genesis block found yet
		    name = "Other Asset";
		}
	    }
	    
	    String s1 = null;
	    if (asset == null ||
		isUnknown==true ||
		(netAmount.equals(BigInteger.ZERO) && !isSentByMe)
		    ) {
		// We don't have formatting details since asset is unknown or deleted
		// If there is a quantity, we don't display it since we don't have display format info
		// Of if incoming asset transfer, unconfirmed, it will be zero, so show ... instead
		s1 = "...";
	    } else {
		BigDecimal displayUnits = CSMiscUtils.getDisplayUnitsForRawUnits(asset, netAmount);
		s1 = CSMiscUtils.getFormattedDisplayString(asset, displayUnits);
	    }
	    
	    //
	    // Create JSONRPCTransactionAmount and add it to list
	    // 
	    String fullName = "";
	    String assetRef = "";
	    if (asset!=null) {
		fullName = asset.getName();
		if (fullName==null) fullName = name; // use short name
		assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	    }
	    BigDecimal displayQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, netAmount);
	    JSONRPCTransactionAmount amount = new JSONRPCTransactionAmount();
	    amount.setAsset_ref(assetRef);
	    amount.setDisplay(s1);
	    amount.setName(fullName);
	    amount.setName_short(name);
	    amount.setQty(displayQty.doubleValue());
	    amount.setRaw(netAmount.longValue());
	    resultList.add(amount);
	}
	

	BigInteger satoshiAmount = receiveMap.get(0);
	satoshiAmount = satoshiAmount.subtract(sendMap.get(0));
	
	// We will show the fee separately so no need to include here.
	if (excludeBTCFee && isSentByMe) {
	    BigInteger feeSatoshis = tx.calculateFee(wallet);  // returns positive
	    if (absoluteBTCFee) {
		satoshiAmount = satoshiAmount.abs().subtract(feeSatoshis);
	    } else {
		satoshiAmount = satoshiAmount.add(feeSatoshis);
	    }
	}
	
	String btcAmount = Utils.bitcoinValueToFriendlyString(satoshiAmount) + " BTC";
	BigDecimal satoshiAmountBTC = new BigDecimal(satoshiAmount).divide(new BigDecimal(Utils.COIN));
	JSONRPCTransactionAmount amount = new JSONRPCTransactionAmount();
	    amount.setAsset_ref("bitcoin");
	    amount.setDisplay(btcAmount);
	    amount.setName("Bitcoin");
	    amount.setName_short("Bitcoin");
	    amount.setQty(satoshiAmountBTC.doubleValue());
	    amount.setRaw(satoshiAmount.longValue());
	    resultList.add(amount);
	
	return resultList;
    }
    
    /**
     * Returns an array of unspent transaction outputs, detailing the Bitcoin and
     * asset balances tied to that UTXO.
     * 
     * Behavior is modeled from bitcoin: https://bitcoin.org/en/developer-reference#listunspent
     * minconf "Default is 1; use 0 to count unconfirmed transactions."
     * maxconf "Default is 9,999,999; use 0 to count unconfirmed transactions."
     * As minconf and maxconf are not optional, the default values above are ignored.
     * 
     * @param walletname
     * @param minconf
     * @param maxconf
     * @param addresses	    List of CoinSpark or Bitcoin addresses
     * @return
     * @throws com.bitmechanic.barrister.RpcException 
     */
    @Override
    public JSONRPCUnspentTransactionOutput[] listunspent(String walletname, Long minconf, Long maxconf, String[] addresses) throws com.bitmechanic.barrister.RpcException
    {
	log.info("LIST UNSPENT TRANSACTION OUTPUTS");
	log.info("wallet name  = " + walletname);
	log.info("min number of confirmations = " + minconf);
	log.info("max number of confirmations = " + maxconf);
	log.info("addresses = " + Arrays.toString(addresses));
	
	Wallet w = getWalletForWalletName(walletname);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	if (minconf<0 || maxconf<0) {
	    JSONRPCError.CONFIRMATIONS_TOO_LOW.raiseRpcException();
	}
	
	NetworkParameters networkParams = this.controller.getModel().getNetworkParameters();
	int mostCommonChainHeight = this.controller.getMultiBitService().getPeerGroup().getMostCommonChainHeight();
	
	boolean skipAddresses = addresses.length == 0;
	
	// Parse the list of addresses
	// If CoinSpark, convert to BTC.
	Set<String> setOfAddresses = new HashSet<String>();
	for (String address: addresses) {
	    address = address.trim();
	    if (address.length()==0) {
		continue; // ignore empty string
	    }
	    String btcAddress = null;
	    String csAddress = null;
	    if (address.startsWith("s")) {
		csAddress = address;
		btcAddress = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
		if (btcAddress == null) {
		    // Invalid CoinSpark address, so throw exception
		    JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException(csAddress);
		}
	    } else {
		btcAddress = address;
	    }
	    boolean b = CSMiscUtils.validateBitcoinAddress(btcAddress, this.controller);
	    if (b) {
		setOfAddresses.add(btcAddress);
		// convenience to add coinspark address for lookup
//		if (csAddress != null) setOfAddresses.add(csAddress);
	    } else {
		if (csAddress != null) {
		    // Invalid Bitcoin address from decoded CoinSpark address so throw exception.
		    JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException(csAddress + " --> decoded btc address " + btcAddress);
		} else {
		    // Invalid Bitcoin address
		    JSONRPCError.BITCOIN_ADDRESS_INVALID.raiseRpcException(btcAddress);
		}
	    }
	}
	
	// If the set of addresses is empty, list of addresses probably whitespace etc.
	// Let's treat as skipping addresses.
	if (setOfAddresses.size()==0) {
	    skipAddresses = true;
	}
	
	ArrayList<JSONRPCUnspentTransactionOutput> resultList = new ArrayList<>();

	Map<CSTransactionOutput, Map<Integer,CSBalance>> map = w.CS.getAllAssetTxOuts();
//	Set<CSTransactionOutput> keys = map.keySet();
	Iterator it = map.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry entry = (Map.Entry) it.next();
	    CSTransactionOutput cstxout = (CSTransactionOutput) entry.getKey();

	    // Only process if txout belongs to our wallet and is unspent
	    Transaction tx = cstxout.getParentTransaction();
	    TransactionOutput txout = tx.getOutput(cstxout.getIndex());
	    if (!txout.isAvailableForSpending() || !txout.isMine(w)) {
		continue;
	    }

	    // If confidence is not building, don't list it as it is pending, dead or unknown.
	    TransactionConfidence.ConfidenceType confidenceType = tx.getConfidence().getConfidenceType();
	    if (confidenceType==TransactionConfidence.ConfidenceType.UNKNOWN || confidenceType==TransactionConfidence.ConfidenceType.DEAD) {
		continue;
	    }
	    
	    
	    int numConfirmations = 0;	// If confidence is PENDING, this will be zero
	    if (confidenceType==TransactionConfidence.ConfidenceType.BUILDING) {
		// getAppearedAtChainHeight() will throw illegalstate exception if confidence is not building
		int txAppearedAtChainHeight = tx.getConfidence().getAppearedAtChainHeight();
		numConfirmations = mostCommonChainHeight - txAppearedAtChainHeight + 1; // if same, then it means 1 confirmation
	    }
	    
	    // Only process if number of confirmations is within range.
	    if (minconf==0 || maxconf==0) {
		// Unconfirmed transactions are okay, so do nothing.
	    } else if (numConfirmations < minconf || numConfirmations > maxconf) {
		// Skip if outside of range
		continue;
	    }
	    
	    
	    // Only process if the BTC address for this tx is in the list of addresses
	    String btcAddress = txout.getScriptPubKey().getToAddress(networkParams).toString();
	    if (!skipAddresses && !setOfAddresses.contains(btcAddress)) {
		continue;
	    }
	    
	    // Get the bitcoin and asset balances on this utxo
	    Map<Integer,CSBalance> balances = (Map<Integer,CSBalance>) entry.getValue();
	    
	    boolean hasAssets = false;
	    ArrayList<JSONRPCBalance> balancesList = new ArrayList<>();

	    for (Map.Entry<Integer, CSBalance> balanceEntry : balances.entrySet()) {
		Integer assetID = balanceEntry.getKey();
		CSBalance balance = balanceEntry.getValue();
		BigInteger qty = balance.getQty();
		
		boolean isSelectable = DefaultCoinSelector.isSelectable(tx);

//		log.info(">>>> assetID = " + assetID + " , qty = " + qty);
		
		// Handle Bitcoin specially
		if (assetID==0) {
		    JSONRPCBalance bal = null;
		    if (isSelectable) {
			bal = createBitcoinBalance(w, qty, null);
		    } else {
			bal = createBitcoinBalance(w, qty, null);
		    }
		    System.out.println(">>> bal = " + bal.toString());
		    balancesList.add(bal);
		    continue;
		}
		
		// Other assets
		hasAssets = true;
		
		if (qty.compareTo(BigInteger.ZERO)>0) {
		    JSONRPCBalance bal = null;
		    bal = createAssetBalance(w, assetID, qty, null);
//		    if (isSelectable) {
//			bal = createAssetBalance(w, assetID, qty, qty);
//		    } else {
//			bal = createAssetBalance(w, assetID, qty, BigInteger.ZERO);
//		    }
		    balancesList.add(bal);
		}
	    }
	    JSONRPCBalance[] balancesArray = balancesList.toArray(new JSONRPCBalance[0]); 

	    String scriptPubKeyHexString = Utils.bytesToHexString( txout.getScriptBytes() );

	    
	    // Build the object to return
	    JSONRPCUnspentTransactionOutput utxo = new JSONRPCUnspentTransactionOutput();
	    utxo.setTxid(tx.getHashAsString());
	    utxo.setVout((long)cstxout.getIndex());
	    utxo.setScriptPubKey(scriptPubKeyHexString);
	    utxo.setAmounts(balancesArray); //new JSONRPCBalance[0]);
	    utxo.setConfirmations((long)numConfirmations);
	    
	    utxo.setBitcoin_address(btcAddress);
	    
	    if (hasAssets) {
		String sparkAddress = null;
	    // First let's see if we have stored the recipient in our map and use it instead
		// of generating a new one from bitcoin address
		try {
		    String spk = SparkBitMapDB.INSTANCE.getSendCoinSparkAddressForTxid(tx.getHashAsString());
		    String btc = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(spk);
		    if (btc.equals(btcAddress)) {
			sparkAddress = spk;
		    }
		} catch (Exception e) {
		}
		if (sparkAddress == null) {
		    sparkAddress = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(btcAddress);
		}
		utxo.setCoinspark_address(sparkAddress);
	    }
	    
	    utxo.setAmounts(balancesArray);
	    
	    resultList.add(utxo);
	}

	JSONRPCUnspentTransactionOutput[] resultArray = resultList.toArray(new JSONRPCUnspentTransactionOutput[0]);
	return resultArray;
    }

    /**
     * Create a JSONRPCBalance object for Bitcoin asset
     * @param w
     * @return 
     */
    private JSONRPCBalance createBitcoinBalance(Wallet w) {
	BigInteger rawBalanceSatoshi = w.getBalance(Wallet.BalanceType.ESTIMATED);
	BigInteger rawSpendableSatoshi = w.getBalance(Wallet.BalanceType.AVAILABLE);
	return createBitcoinBalance(w, rawSpendableSatoshi, rawBalanceSatoshi);
    }
    
    /**
     * Create a JSONRPCBalance object for Bitcoin asset
     * @param w
     * @param rawBalanceSatoshi   In BitcoinJ terms, this is the estimated total balance
     * @param rawSpendableSatoshi       In BitcoinJ terms, this is the available amount to spend
     *                                  If null, will set amount instead of total and spendable.
     * @return 
     */
    private JSONRPCBalance createBitcoinBalance(Wallet w, BigInteger rawBalanceSatoshi, BigInteger rawSpendableSatoshi) {
	JSONRPCBalance balance = new JSONRPCBalance();
	balance.setAsset_ref("bitcoin");

	BigDecimal rawBalanceBTC = new BigDecimal(rawBalanceSatoshi).divide(new BigDecimal(Utils.COIN));
	String rawBalanceDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";
	JSONRPCBalanceAmount bitcoinBalanceAmount = new JSONRPCBalanceAmount(rawBalanceSatoshi.longValue(), rawBalanceBTC.doubleValue(), rawBalanceDisplay);

	if (rawSpendableSatoshi != null) {
	    BigDecimal rawSpendableBTC = new BigDecimal(rawSpendableSatoshi).divide(new BigDecimal(Utils.COIN));
	    String rawSpendableDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";
	    JSONRPCBalanceAmount bitcoinSpendableAmount = new JSONRPCBalanceAmount(rawSpendableSatoshi.longValue(), rawSpendableBTC.doubleValue(), rawSpendableDisplay);
	    balance.setTotal(bitcoinBalanceAmount);
	    balance.setSpendable(bitcoinSpendableAmount);
	} else {
	    balance.setAmount(bitcoinBalanceAmount);
	}
	balance.setVisible(true);
	balance.setValid(true);
	balance.setRefreshing(false);
	return balance;
    }
    
    /**
     * Create and populate a JSONRPCBalance object given an assetID and balance
     * @param w
     * @param assetID
     * @param totalRaw
     * @param spendableRaw If null, we set the amount field, instead of total and spendable.
     * @return 
     */
    private JSONRPCBalance createAssetBalance(Wallet w, int assetID, BigInteger totalRaw, BigInteger spendableRaw) {
	//Wallet.CoinSpark.AssetBalance assetBalance;
	CSAsset asset = w.CS.getAsset(assetID);

	String name = asset.getName();
	String nameShort = asset.getNameShort();

	if (name == null) {
	    CoinSparkGenesis genesis = asset.getGenesis();
	    if (genesis != null) {
		name = "Asset from " + genesis.getDomainName();
		nameShort = name;
	    } else {
		// No genesis block found yet
		name = "Other Asset";
		nameShort = "Other Asset";
	    }
	}

	String assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	if (assetRef == null) {
	    assetRef = "Awaiting new asset confirmation...";
	}

	
	JSONRPCBalance ab = new JSONRPCBalance();
	ab.setAsset_ref(assetRef);
	
	// Compute total balance
	Double balanceQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, totalRaw).doubleValue();
	String balanceDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, totalRaw);
	JSONRPCBalanceAmount balanceAmount = new JSONRPCBalanceAmount(totalRaw.longValue(), balanceQty, balanceDisplay);
	
	if (spendableRaw != null) {
	    Double spendableQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, spendableRaw).doubleValue();
	    String spendableDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, spendableRaw);
	    JSONRPCBalanceAmount spendableAmount = new JSONRPCBalanceAmount(spendableRaw.longValue(), spendableQty, spendableDisplay);
	
	    ab.setTotal(balanceAmount);
	    ab.setSpendable(spendableAmount);
	} else {
	    ab.setAmount(balanceAmount);
	}

	ab.setName(name);
	ab.setName_short(nameShort);
	String domain = CSMiscUtils.getDomainHost(asset.getDomainURL());
	ab.setDomain(domain);
	ab.setUrl(asset.getAssetWebPageURL());
	ab.setIssuer(asset.getIssuer());
	ab.setDescription(asset.getDescription());
	ab.setUnits(asset.getUnits());
	ab.setMultiple(asset.getMultiple());
	boolean isValid = (asset.getAssetState() == CSAsset.CSAssetState.VALID);
	
	if (asset.getAssetState()==CSAsset.CSAssetState.REFRESH) {
	    ab.setRefreshing(true);
	    ab.setStatus(CSMiscUtils.getHumanReadableAssetState(asset.getAssetStateBeforeRefresh()));
	} else {
	    ab.setRefreshing(false);
	    ab.setStatus(CSMiscUtils.getHumanReadableAssetState(asset.getAssetState()));
	}
	
	ab.setValid(isValid);
	Date validCheckedDate = asset.getValidChecked();
	if (validCheckedDate != null) {
	    ab.setChecked_unixtime(validCheckedDate.getTime() / 1000L);
	}
	ab.setContract_url(asset.getContractUrl());

	String contractPath = asset.getContractPath();
	if (contractPath != null) {
	    String appDirPath = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory();
	    File file = new File(contractPath);
	    File dir = new File(appDirPath);
	    try {
		URI absolute = file.toURI();
		URI base = dir.toURI();
		URI relative = base.relativize(absolute);
		contractPath = relative.getPath();
	    } catch (Exception e) {
		// do nothing, if error, just use full contractPath
	    }
	}
	ab.setContract_file(contractPath);
	ab.setGenesis_txid(asset.getGenTxID());
	Date creationDate = asset.getDateCreation();
	if (creationDate != null) {
	    ab.setAdded_unixtime(creationDate.getTime() / 1000L);
	}
	// 3 October 2014, 1:47 am
	SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm");
	sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // CoinSpark asset web page shows GMT/UTC.
	SimpleDateFormat ampmdf = new SimpleDateFormat(" a");  // by default is uppercase and we need lower to match website
	Date issueDate = asset.getIssueDate();
	if (issueDate != null) {
	    ab.setIssue_date(sdf.format(issueDate) + ampmdf.format(issueDate).toLowerCase());
	    ab.setIssue_unixtime(issueDate.getTime() / 1000L);
	}

	// Never expires
	Date expiryDate = asset.getExpiryDate();
	if (expiryDate != null) {
	    ab.setExpiry_date(sdf.format(expiryDate) + ampmdf.format(expiryDate).toLowerCase());
	    ab.setExpiry_unixtime(expiryDate.getTime() / 1000L);
	}
	ab.setTracker_urls(asset.getCoinsparkTrackerUrls());
	ab.setIcon_url(asset.getIconUrl());
	ab.setImage_url(asset.getImageUrl());
	ab.setFeed_url(asset.getFeedUrl());
	ab.setRedemption_url(asset.getRedemptionUrl());
	ab.setVisible(asset.isVisible());
	return ab;
    }
    
    @Override
    public JSONRPCBalance[] listbalances(String walletID, Boolean onlyVisible) throws com.bitmechanic.barrister.RpcException
    {
	log.info("LIST BALANCES");
	log.info("wallet name  = " + walletID);
	log.info("only visible = " + onlyVisible);
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	ArrayList<JSONRPCBalance> resultList = new ArrayList<>();

	// Add entry for BTC balances
	JSONRPCBalance btcAssetBalance = createBitcoinBalance(w);
	resultList.add(btcAssetBalance);

	
		
	int[] assetIDs = w.CS.getAssetIDs();
	int n = 0;
	if (assetIDs!=null) {
	    n = assetIDs.length;
	}
	
	Wallet.CoinSpark.AssetBalance assetBalance;
	for (int i=0; i<n; i++) {
	    int id = assetIDs[i];
	    if (id==0) continue;
	    CSAsset asset = w.CS.getAsset(id);
	    if (asset==null) continue;	    
	    if (onlyVisible && !asset.isVisible()) continue;
	    
	    String name=asset.getName();
	    String nameShort=asset.getNameShort();

	    if (name == null) {
	    	CoinSparkGenesis genesis = asset.getGenesis();
		if (genesis!=null) {
		    name = "Asset from " + genesis.getDomainName();
		    nameShort = name;
		} else {
		    // No genesis block found yet
		    name = "Other Asset";
		    nameShort = "Other Asset";
		}
	    }
	    
	    String assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	    if (assetRef==null) assetRef = "Awaiting new asset confirmation...";
	    	    
	    assetBalance = w.CS.getAssetBalance(id);
	    
	    JSONRPCBalance ab = createAssetBalance(w, id, assetBalance.total, assetBalance.spendable);
/*
	    
	    Long spendableRaw = assetBalance.spendable.longValue();
	    Double spendableQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, assetBalance.spendable).doubleValue();
	    String spendableDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.spendable);
	    JSONRPCBalanceAmount spendableAmount = new JSONRPCBalanceAmount(spendableRaw, spendableQty, spendableDisplay);
	    Long balanceRaw = assetBalance.total.longValue();
	    Double balanceQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, assetBalance.total).doubleValue();
	    String balanceDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.total);
	    JSONRPCBalanceAmount balanceAmount = new JSONRPCBalanceAmount(balanceRaw, balanceQty, balanceDisplay);
	    JSONRPCBalance ab = new JSONRPCBalance();
	    ab.setAsset_ref(assetRef);
	    ab.setBalance(balanceAmount);
	    ab.setSpendable(spendableAmount);
	    
	    ab.setName(name);
	    ab.setName_short(nameShort);
	    String domain = CSMiscUtils.getDomainHost(asset.getDomainURL());
	    ab.setDomain(domain);
	    ab.setUrl(asset.getAssetWebPageURL());
	    ab.setIssuer(asset.getIssuer());
	    ab.setDescription(asset.getDescription());
	    ab.setUnits(asset.getUnits());
	    ab.setMultiple(asset.getMultiple());
	    ab.setStatus(CSMiscUtils.getHumanReadableAssetState(asset.getAssetState()));
	    boolean isValid = (asset.getAssetState()==CSAsset.CSAssetState.VALID);
	// FIXME: Check num confirms too?
	    ab.setValid(isValid);
	    Date validCheckedDate = asset.getValidChecked();
	    if (validCheckedDate!=null) {
		ab.setChecked_unixtime(validCheckedDate.getTime()/1000L);
	    }
	    ab.setContract_url(asset.getContractUrl());
	    
	    String contractPath = asset.getContractPath();
	    if (contractPath!=null) {
		 String appDirPath = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory();
		File file = new File(contractPath);
		File dir = new File(appDirPath);
		try {
		    URI absolute = file.toURI();
		    URI base = dir.toURI();
		    URI relative = base.relativize(absolute);
		    contractPath = relative.getPath();
		} catch (Exception e) {
		    // do nothing, if error, just use full contractPath
		}
	    }	     
	    ab.setContract_file(contractPath);
	    ab.setGenesis_txid(asset.getGenTxID());
	    Date creationDate = asset.getDateCreation();
	    if (creationDate != null) {
		ab.setAdded_unixtime(creationDate.getTime()/1000L);
	    }
	    // 3 October 2014, 1:47 am
	    SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, h:mm");
	    sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // CoinSpark asset web page shows GMT/UTC.
	    SimpleDateFormat ampmdf = new SimpleDateFormat(" a");  // by default is uppercase and we need lower to match website
	    Date issueDate = asset.getIssueDate();
	    if (issueDate != null) {
		ab.setIssue_date(sdf.format(issueDate) + ampmdf.format(issueDate).toLowerCase());
		ab.setIssue_unixtime(issueDate.getTime()/1000L);
	    }

	    // Never expires
	    Date expiryDate = asset.getExpiryDate();
	    if (expiryDate != null) {
		ab.setExpiry_date(sdf.format(expiryDate) + ampmdf.format(expiryDate).toLowerCase() );
		ab.setExpiry_unixtime(expiryDate.getTime()/1000L);
	    }
	    ab.setTracker_urls(asset.getCoinsparkTrackerUrls());
	    ab.setIcon_url(asset.getIconUrl());
	    ab.setImage_url(asset.getImageUrl());
	    ab.setFeed_url(asset.getFeedUrl());
	    ab.setRedemption_url(asset.getRedemptionUrl());
	    ab.setVisible(asset.isVisible());
	    */
	    resultList.add(ab);
	}
	
	JSONRPCBalance[] resultArray = resultList.toArray(new JSONRPCBalance[0]);
	return resultArray;
    }
    
    /**
     * Returns true if transaction output can be spent and belongs to the wallet.
     * 
     * @param w
     * @param hash
     * @param vout
     * @return
     * @throws com.bitmechanic.barrister.RpcException 
     */
    private synchronized boolean isTxOutSpendable(Wallet w, Sha256Hash hash, int vout) throws com.bitmechanic.barrister.RpcException {
	Transaction tx = w.getTransaction(hash);
	if (tx == null) {
	    // Transaction is not in wallet
	    JSONRPCError.TX_NOT_FOUND_IN_WALLET.raiseRpcException();
	}
	
	TransactionOutput txout = null;
	try {
	    txout = tx.getOutput(vout);
	} catch (IndexOutOfBoundsException e) {
	    // Output not found on transaction
	    JSONRPCError.TXOUT_INDEX_INVALID.raiseRpcException();
	}
	
	if (!txout.isMine(w)) {
	    // not my wallet - this should not happen if transaction is found in wallet.
	    JSONRPCError.TXOUT_NOT_MINE.raiseRpcException();
	}
	
	if (!txout.isAvailableForSpending()) {
	    // not available for spending
	    JSONRPCError.TXOUT_IS_NOT_AVAILABLE_FOR_SPENDING.raiseRpcException();
	}
	
	return true;
    }

    @Override
    public synchronized String sendbitcoin(String walletID, String address, Double amount) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND BITCOIN");
	log.info("wallet name = " + walletID);
	log.info("address     = " + address);
	log.info("amount      = " + amount);
	return sendbitcoinwith_impl(walletID, null, 0L, address, amount, null);
    }
    
    @Override
    public synchronized String sendbitcoinwith(String walletID, String txid, Long vout, String address, Double amount) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND BITCOIN WITH");
	log.info("wallet name = " + walletID);
	log.info("txid        = " + txid);
	log.info("vout        = " + vout);
	log.info("address     = " + address);
	log.info("amount      = " + amount);
	return sendbitcoinwith_impl(walletID, txid, vout, address, amount, null);
    }
    
    private synchronized String sendbitcoinwith_impl(String walletID, String txid, Long vout, String address, Double amount, String message) throws com.bitmechanic.barrister.RpcException
    {    
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	if (amount<=0.0) {
	    JSONRPCError.SEND_BITCOIN_AMOUNT_TOO_LOW.raiseRpcException();
	}
	
	// Check send with txid and vout
	Sha256Hash sendWithTxidHash = null;
	boolean canSpendSendWithTxOut = false;
	if (txid != null) {
	    try {
		sendWithTxidHash = new Sha256Hash(txid);
	    } catch (IllegalArgumentException e) {
		// Not a valid tx hash string
		JSONRPCError.INVALID_TXID_HASH.raiseRpcException();
	    }
	    canSpendSendWithTxOut = isTxOutSpendable(w, sendWithTxidHash, vout.intValue());
	}
	
	CoinSparkPaymentRef paymentRef = null;
	
	String bitcoinAddress = address;
	if (address.startsWith("s")) {
	    bitcoinAddress = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
	    if (bitcoinAddress==null) {
		JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException();
	    } else {
		CoinSparkAddress csa = new CoinSparkAddress();
		csa.decode(address);
		int flags = csa.getAddressFlags();
		if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS) > 0) {
		    paymentRef = csa.getPaymentRef();
		    log.debug(">>>> CoinSpark address has payment refs flag set: " + paymentRef.toString());
		}
	    }
	}
	boolean isValid = CSMiscUtils.validateBitcoinAddress(bitcoinAddress, controller);
	if (!isValid) {
	    JSONRPCError.BITCOIN_ADDRESS_INVALID.raiseRpcException();
	}
	
	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	if (wd.isBusy()) {
	    JSONRPCError.WALLEY_IS_BUSY.raiseRpcException();
	} else {
	    wd.setBusy(true);
	    wd.setBusyTaskKey("jsonrpc.busy.sendbitcoin");
	    this.controller.fireWalletBusyChange(true);
	}

	
	Transaction sendTransaction = null;
	boolean sendValidated = false;
	boolean sendSuccessful = false;
	String sendTxHash = null;
	try {
	    String sendAmount = amount.toString();
	    // Create a SendRequest.
	    Address sendAddressObject;

	    sendAddressObject = new Address(controller.getModel().getNetworkParameters(), bitcoinAddress);
	    Wallet.SendRequest sendRequest = Wallet.SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));
//                SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount), 6, new BigInteger("10000"),1);
	    sendRequest.ensureMinRequiredFee = true;
	    sendRequest.fee = BigInteger.ZERO;
	    sendRequest.feePerKb = BitcoinModel.SEND_FEE_PER_KB_DEFAULT;

	    // Send with txout vout
	    if (canSpendSendWithTxOut) {
		boolean addedInput = sendRequest.addInput(w, new CSTransactionOutput(sendWithTxidHash, vout.intValue()));
		if (!addedInput) {
		    // Failed to add input, so throw exception
		    JSONRPCError.SEND_WITH_TXID_VOUT_FAILED.raiseRpcException();
		}
	    }
	    
	    // Send with payment ref - if it exists and is not 0 which SparkBit treats semantically as null
	    if (paymentRef != null && paymentRef.getRef() != 0) {
		sendRequest.setPaymentRef(paymentRef);
	    }

	    
	    // Set up message if one exists
	    boolean isEmptyMessage = false;
	    if (message == null || message.isEmpty() || message.trim().length() == 0) {
		isEmptyMessage = true;
	    }
	    if (!isEmptyMessage) {
		CoinSparkMessagePart[] parts = {CSMiscUtils.createPlainTextCoinSparkMessagePart(message)};
		String[] serverURLs = CSMiscUtils.getMessageDeliveryServersArray(this.controller);
		sendRequest.setMessage(parts, serverURLs);
//			log.debug(">>>> Messaging servers = " + ArrayUtils.toString(serverURLs));
//			log.debug(">>>> parts[0] = " + parts[0]);
//			log.debug(">>>> parts[0].fileName = " + parts[0].fileName);
//			log.debug(">>>> parts[0].mimeType = " + parts[0].mimeType);
//			log.debug(">>>> parts[0].content = " + new String(parts[0].content, "UTF-8"))
	    }

	    
	    // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
	    // Complete it (which works out the fee) but do not sign it yet.
	    log.info("Just about to complete the tx (and calculate the fee)...");

	    w.completeTx(sendRequest, false);
	    sendValidated = true;
	    log.info("The fee after completing the transaction was " + sendRequest.fee);
	    // Let's do it for real now.

	    sendTransaction = this.controller.getMultiBitService().sendCoins(wd, sendRequest, null);
	    if (sendTransaction == null) {
		    // a null transaction returned indicates there was not
		// enough money (in spite of our validation)
		JSONRPCError.SEND_BITCOIN_INSUFFICIENT_MONEY.raiseRpcException();
	    } else {
		sendSuccessful = true;
		sendTxHash = sendTransaction.getHashAsString();
		log.info("Sent transaction was:\n" + sendTransaction.toString());
	    }

	    if (sendSuccessful) {
		// There is enough money.
		
		/* If sending assets or BTC to a coinspark address, record transaction id --> coinspark address, into hashmap so we can use when displaying transactions */
		if (address.startsWith("s")) {
		    SparkBitMapDB.INSTANCE.putSendCoinSparkAddressForTxid(sendTxHash, address);
		}
	    } else {
		// There is not enough money
	    }
//      } catch (WrongNetworkException e1) {
//      } catch (AddressFormatException e1) {
//      } catch (KeyCrypterException e1) {
	} catch (InsufficientMoneyException e) {
	    JSONRPCError.SEND_BITCOIN_INSUFFICIENT_MONEY.raiseRpcException();
	} catch (CSExceptions.CannotEncode e) {
	    JSONRPCError.SEND_MESSAGE_CANNOT_ENCODE.raiseRpcException(e.getMessage());
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Could not send bitcoin due to error", e);
	} finally {
	    // Save the wallet.
	    try {
		this.controller.getFileHandler().savePerWalletModelData(wd, false);
	    } catch (WalletSaveException e) {
//        log.error(e.getMessage(), e);
	    }
	    
	    if (sendSuccessful) {
		// This returns immediately if rpcsendassettimeout is 0.
		JSONRPCController.INSTANCE.waitForTxSelectable(sendTransaction);
//		JSONRPCController.INSTANCE.waitForTxBroadcast(sendTxHash);
	    }
	    
	    // Declare that wallet is no longer busy with the task.
	    wd.setBusyTaskKey(null);
	    wd.setBusy(false);
	    this.controller.fireWalletBusyChange(false);
	}

	if (sendSuccessful) {
	    controller.fireRecreateAllViews(false);
	}
	return sendTxHash;
    }

    @Override
    public synchronized String sendasset(String walletID, String address, String assetRef, Double quantity, Boolean senderPays) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND ASSET");
	log.info("wallet name = " + walletID);
	log.info("address     = " + address);
	log.info("asset ref   = " + assetRef);
	log.info("quantity    = " + quantity);
	log.info("sender pays = " + senderPays);
	return sendassetwith_impl(walletID, null, 0L, address, assetRef, quantity, senderPays, null, null);
    }
    
    @Override
    public synchronized String sendassetwith(String walletID, String txid, Long vout, String address, String assetRef, Double quantity, Boolean senderPays) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND ASSET WITH");
	log.info("wallet name = " + walletID);
	log.info("txid        = " + txid);
	log.info("vout        = " + vout);
	log.info("address     = " + address);
	log.info("asset ref   = " + assetRef);
	log.info("quantity    = " + quantity);
	log.info("sender pays = " + senderPays);
	return sendassetwith_impl(walletID, txid, vout, address, assetRef, quantity, senderPays, null, null);
    }
    
    private synchronized String sendassetwith_impl(String walletID, String txid, Long vout, String address, String assetRef, Double quantity, Boolean senderPays, String message, Double btcAmount) throws com.bitmechanic.barrister.RpcException
    {
	String sendTxHash = null;
	boolean sendValidated = false;
	boolean sendSuccessful = false;
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	// Check send with txid and vout
	Sha256Hash sendWithTxidHash = null;
	boolean canSpendSendWithTxOut = false;
	if (txid != null) {
	    try {
		sendWithTxidHash = new Sha256Hash(txid);
	    } catch (IllegalArgumentException e) {
		// Not a valid tx hash string
		JSONRPCError.INVALID_TXID_HASH.raiseRpcException();
	    }
	    canSpendSendWithTxOut = isTxOutSpendable(w, sendWithTxidHash, vout.intValue());
	}
	
	
	if (quantity<=0.0) {
	    JSONRPCError.SEND_ASSET_AMOUNT_TOO_LOW.raiseRpcException();
	}
	
	// BTC send amount, if null, use default amount of 10,000 satoshis.
	String sendAmount;
	if (btcAmount==null) {
	    sendAmount = Utils.bitcoinValueToPlainString(BitcoinModel.COINSPARK_SEND_MINIMUM_AMOUNT);	    
	} else {
	    double d = btcAmount.doubleValue();
	    if (d<=0.0) {
		JSONRPCError.SEND_BITCOIN_AMOUNT_TOO_LOW.raiseRpcException();
	    }
	    sendAmount = btcAmount.toString();
	}
	
	CoinSparkPaymentRef paymentRef = null;
	String bitcoinAddress = address;
	if (!address.startsWith("s")) {
	    JSONRPCError.ADDRESS_NOT_COINSPARK_ADDRESS.raiseRpcException();
	} else {
	    bitcoinAddress = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
	    if (bitcoinAddress==null) {
		JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException();
	    }
	    
	    CoinSparkAddress csa = CSMiscUtils.decodeCoinSparkAddress(address);
	    if (!CSMiscUtils.canSendAssetsToCoinSparkAddress(csa)) {
		JSONRPCError.COINSPARK_ADDRESS_MISSING_ASSET_FLAG.raiseRpcException();
	    }
	    
	    // payment ref?
	    int flags = csa.getAddressFlags();
	    if ((flags & CoinSparkAddress.COINSPARK_ADDRESS_FLAG_PAYMENT_REFS) > 0) {
		paymentRef = csa.getPaymentRef();
		log.debug(">>>> CoinSpark address has payment refs flag set: " + paymentRef.toString());
	    }
	}
	boolean isValid = CSMiscUtils.validateBitcoinAddress(bitcoinAddress, controller);
	if (!isValid) {
	    JSONRPCError.BITCOIN_ADDRESS_INVALID.raiseRpcException();
	}
	
	String filename = getFullPathForWalletName(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	if (wd.isBusy()) {
	    JSONRPCError.WALLEY_IS_BUSY.raiseRpcException();
	} else {
	    wd.setBusy(true);
	    wd.setBusyTaskKey("jsonrpc.busy.sendasset");
	    this.controller.fireWalletBusyChange(true);
	}
	
	Transaction sendTransaction = null;
	
	try {
	    // -- boilerplate ends here....

	    CSAsset asset = getAssetForAssetRefString(w, assetRef);
	    if (asset==null) {
		if (isAssetRefValid(assetRef)) {
		    JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
		} else {
		    JSONRPCError.ASSETREF_INVALID.raiseRpcException();		
		}
	    }
	    
	    if (asset.getAssetState()!=CSAsset.CSAssetState.VALID) {
		if (!CSMiscUtils.canSendInvalidAsset(controller)) {
		    JSONRPCError.ASSET_STATE_INVALID.raiseRpcException();
		}
	    }
	    
	    // Check number of confirms
	    int lastHeight = w.getLastBlockSeenHeight();
	    CoinSparkAssetRef assetReference = asset.getAssetReference();
	    if (assetReference != null) {
		final int blockIndex = (int) assetReference.getBlockNum();
		final int numConfirmations = lastHeight - blockIndex + 1; // 0 means no confirmation, 1 is yes for sa
		int threshold = NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
		// FIXME: REMOVE/COMMENT OUT BEFORE RELEASE?
		String sendAssetWithJustOneConfirmation = controller.getModel().getUserPreference("sendAssetWithJustOneConfirmation");
		if (Boolean.TRUE.toString().equals(sendAssetWithJustOneConfirmation)) {
		    threshold = 1;
		}
		//System.out.println(">>>> " + CSMiscUtils.getHumanReadableAssetRef(asset) + " num confirmations " + numConfirmations + ", threshold = " + threshold);
		if (numConfirmations < threshold) {
		    JSONRPCError.ASSET_NOT_CONFIRMED.raiseRpcException();
		}
	    }
	    
	    

	    String displayQtyString = new BigDecimal(quantity).toPlainString();
	    BigInteger assetAmountRawUnits = CSMiscUtils.getRawUnitsFromDisplayString(asset, displayQtyString);
	    int assetID = asset.getAssetID();
	    BigInteger spendableAmount =  w.CS.getAssetBalance(assetID).spendable;
	    
	    log.info("Want to send: " + assetAmountRawUnits + " , AssetID=" + assetID + ", total="+w.CS.getAssetBalance(assetID).total + ", spendable=" + w.CS.getAssetBalance(assetID).spendable );       
            
//	    String sendAmount = Utils.bitcoinValueToPlainString(BitcoinModel.COINSPARK_SEND_MINIMUM_AMOUNT);	    
	    	    CoinSparkGenesis genesis = asset.getGenesis();

	    	    long desiredRawUnits = assetAmountRawUnits.longValue();
	    short chargeBasisPoints = genesis.getChargeBasisPoints();
	    long rawFlatChargeAmount = genesis.getChargeFlat();
	    boolean chargeExists = ( rawFlatChargeAmount>0 || chargeBasisPoints>0 );
	    if (chargeExists) {
		if (senderPays) {
		    long x = genesis.calcGross(desiredRawUnits);
		    assetAmountRawUnits = new BigInteger(String.valueOf(x));
		} else {
		    // We don't have to do anything if recipient pays, just send gross amount.
		    // calcNet() returns what the recipient will receive, but it's not what we send. 
		}
	    }
	    
	    
	    if (assetAmountRawUnits.compareTo(spendableAmount) > 0) {
		JSONRPCError.ASSET_INSUFFICIENT_BALANCE.raiseRpcException();
	    }
	    

	    // Create a SendRequest.
                Address sendAddressObject;
		String sendAddress = bitcoinAddress;
                sendAddressObject = new Address(controller.getModel().getNetworkParameters(), sendAddress);
                //SendRequest sendRequest = SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));

		//public static SendRequest to(Address destination,BigInteger value,int assetID, BigInteger assetValue,int split) {
		//BigInteger assetAmountRawUnits = new BigInteger(assetAmount);
		BigInteger bitcoinAmountSatoshis = Utils.toNanoCoins(sendAmount);
		
		Wallet.SendRequest sendRequest = Wallet.SendRequest.to(sendAddressObject, bitcoinAmountSatoshis, assetID, assetAmountRawUnits, 1);
                sendRequest.ensureMinRequiredFee = true;
                sendRequest.fee = BigInteger.ZERO;
                sendRequest.feePerKb = BitcoinModel.SEND_FEE_PER_KB_DEFAULT;

                // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
		
		// Send with txout vout
		if (canSpendSendWithTxOut) {
		    boolean addedInput = sendRequest.addInput(w, new CSTransactionOutput(sendWithTxidHash, vout.intValue()));
		    if (!addedInput) {
			// Failed to add input, so throw exception
			JSONRPCError.SEND_WITH_TXID_VOUT_FAILED.raiseRpcException();
		    }
		}

	    // Send with payment ref - if it exists and is not 0 which SparkBit treats semantically as null
	    if (paymentRef != null && paymentRef.getRef() != 0) {
		sendRequest.setPaymentRef(paymentRef);
	    }

	    
	    // Set up message if one exists
	    boolean isEmptyMessage = false;
	    if (message == null || message.trim().isEmpty()) {
		isEmptyMessage = true;
	    }
	    if (!isEmptyMessage) {
		CoinSparkMessagePart[] parts = {CSMiscUtils.createPlainTextCoinSparkMessagePart(message)};
		String[] serverURLs = CSMiscUtils.getMessageDeliveryServersArray(this.controller);
		sendRequest.setMessage(parts, serverURLs);
	    }

	    
                // Complete it (which works out the fee) but do not sign it yet.
                log.info("Just about to complete the tx (and calculate the fee)...");

		// there is enough money, so let's do it for real now
		    w.completeTx(sendRequest, false);
	    sendValidated = true;
	    log.info("The fee after completing the transaction was " + sendRequest.fee);
	    // Let's do it for real now.

	    sendTransaction = this.controller.getMultiBitService().sendCoins(wd, sendRequest, null);
	    if (sendTransaction == null) {
		    // a null transaction returned indicates there was not
		// enough money (in spite of our validation)
		JSONRPCError.ASSET_INSUFFICIENT_BALANCE.raiseRpcException();
	    } else {
		sendSuccessful = true;
		sendTxHash = sendTransaction.getHashAsString();		
	    }

	    if (sendSuccessful) {
		// There is enough money.
		
		/* If sending assets or BTC to a coinspark address, record transaction id --> coinspark address, into hashmap so we can use when displaying transactions */
		if (address.startsWith("s")) {
		    SparkBitMapDB.INSTANCE.putSendCoinSparkAddressForTxid(sendTxHash, address);
		}		
	    } else {
		// There is not enough money
	    }
	    
	    //--- bolilerplate begins...
	} catch (InsufficientMoneyException ime) {
	    JSONRPCError.ASSET_INSUFFICIENT_BALANCE.raiseRpcException();
	} catch (com.bitmechanic.barrister.RpcException e) {
	    throw(e);
	} catch (CSExceptions.CannotEncode e) {
	    JSONRPCError.SEND_MESSAGE_CANNOT_ENCODE.raiseRpcException(e.getMessage());
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Could not send asset due to error: " , e);
	} finally {
	    // Save the wallet.
	    try {
		this.controller.getFileHandler().savePerWalletModelData(wd, false);
	    } catch (WalletSaveException e) {
//        log.error(e.getMessage(), e);
	    }
	    	    		
	    if (sendSuccessful) {
		// This returns immediately if rpcsendassettimeout is 0.
		JSONRPCController.INSTANCE.waitForTxSelectable(sendTransaction);
//		JSONRPCController.INSTANCE.waitForTxBroadcast(sendTxHash);
	    }
	    
	    // Declare that wallet is no longer busy with the task.
	    wd.setBusyTaskKey(null);
	    wd.setBusy(false);
	    this.controller.fireWalletBusyChange(false);
	}

	if (sendSuccessful) {
	    controller.fireRecreateAllViews(false);
	}
	return sendTxHash;
    }
    
//    @Override
//    public Boolean test(JSONRPCTestParams params) throws com.bitmechanic.barrister.RpcException {
//	log.info("TEST");
//	log.info("walletname = " + params.getWalletname());
//	log.info("address = " + params.getAddress());
//	log.info("amount = " + params.getAmount());
//	log.info("txid = " + params.getTxid());
//	log.info("vout = " + params.getVout());
//	log.info("message = " + params.getMessage());
//	return true;
//    }

    //
    // 0.9.4 JSON-RPC Commands
    //
    
    @Override
    public String sendbitcoinasset(String walletname, String address, Double btc_amount, String assetref, Double asset_qty, Boolean senderpays) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND BITCOIN ASSET");
	log.info("wallet name = " + walletname);
	log.info("address     = " + address);
	log.info("btc amount  = " + btc_amount);
	log.info("asset ref   = " + assetref);
	log.info("asset qty   = " + asset_qty);
	log.info("sender pays = " + senderpays);
	return sendassetwith_impl(walletname, null, 0L, address, assetref, asset_qty, senderpays, null, btc_amount);
    }
    
    @Override
    public String sendassetmessage(String walletname, String address, String assetref, Double quantity, Boolean senderpays, String message) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND ASSET");
	log.info("wallet name = " + walletname);
	log.info("address     = " + address);
	log.info("asset ref   = " + assetref);
	log.info("quantity    = " + quantity);
	log.info("sender pays = " + senderpays);
	log.info("message     = " + message);
	return sendassetwith_impl(walletname, null, 0L, address, assetref, quantity, senderpays, message, null);
    }
    
    @Override
    public String sendbitcoinmessage(String walletname, String address, Double amount, String message) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND BITCOIN MESSAGE");
	log.info("wallet name = " + walletname);
	log.info("address     = " + address);
	log.info("amount      = " + amount);
	log.info("message     = " + message);
	return sendbitcoinwith_impl(walletname, null, 0L, address, amount, message);
    }
    
    @Override
    public String sendbitcoinassetmessage(String walletname, String address, Double btc_amount, String assetref, Double asset_qty, Boolean senderpays, String message) throws com.bitmechanic.barrister.RpcException
    {
	log.info("SEND BITCOIN ASSET");
	log.info("wallet name = " + walletname);
	log.info("address     = " + address);
	log.info("btc amount  = " + btc_amount);
	log.info("asset ref   = " + assetref);
	log.info("asset qty   = " + asset_qty);
	log.info("sender pays = " + senderpays);
	log.info("message     = " + message);
	return sendassetwith_impl(walletname, null, 0L, address, assetref, asset_qty, senderpays, message, btc_amount);
	
    }
    

    
}
