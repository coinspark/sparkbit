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

import com.bitmechanic.barrister.RpcException;
import org.multibit.controller.bitcoin.BitcoinController;
import org.sparkbit.jsonrpc.autogen.*;
import java.util.List;
import java.util.ArrayList;
import org.multibit.model.bitcoin.WalletData;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.model.core.StatusEnum;
import org.multibit.network.ReplayManager;
import com.google.bitcoin.core.*;
//import java.util.HashMap;
import java.util.Iterator;
//import org.apache.commons.codec.digest.DigestUtils;
import org.coinspark.protocol.CoinSparkAddress;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;
import org.multibit.utils.CSMiscUtils;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.multibit.model.bitcoin.BitcoinModel;
import org.multibit.model.bitcoin.WalletAddressBookData;
import org.multibit.model.bitcoin.WalletInfoData;
import com.google.bitcoin.crypto.KeyCrypter;
import org.multibit.file.FileHandler;
import java.io.*;
import org.multibit.file.BackupManager;
import com.google.bitcoin.crypto.KeyCrypterException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.coinspark.protocol.CoinSparkGenesis;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.multibit.exchange.CurrencyConverter;
import org.sparkbit.SBEvent;
import org.sparkbit.SBEventType;
import org.multibit.network.MultiBitService;
import org.multibit.store.MultiBitWalletVersion;
import java.util.Date;
import org.multibit.file.WalletSaveException;
import static org.multibit.model.bitcoin.WalletAssetComboBoxModel.NUMBER_OF_CONFIRMATIONS_TO_SEND_ASSET_THRESHOLD;
import java.util.concurrent.ConcurrentHashMap;
import org.coinspark.core.CSUtils;
import java.text.SimpleDateFormat;
import static org.multibit.network.MultiBitService.WALLET_SUFFIX;
import static org.multibit.network.MultiBitService.getFilePrefix;
import org.sparkbit.utils.FileNameCleaner;
import org.apache.commons.io.FilenameUtils;
import java.util.TimeZone;

/**
 * For now, synchronized access to commands which mutate
 */
public class SparkBitJSONRPCServiceImpl implements sparkbit {
    
    // Limit on the number of addresses you can create in one call
    public static final int CREATE_ADDRESSES_LIMIT = 100;
    
    
    private BitcoinController controller;
//    private MultiBitFrame mainFrame;
//    private ConcurrentHashMap<String,String> walletFilenameMap;
    
    public SparkBitJSONRPCServiceImpl() {
	this.controller = JSONRPCController.INSTANCE.getBitcoinController();
//	this.mainFrame = JSONRPCController.INSTANCE.getMultiBitFrame();
//	walletFilenameMap = new ConcurrentHashMap<>();
//	updateWalletFilenameMap();
    }
    
    /*
    Get status on a per wallet basis.
    */
    @Override
    public JSONRPCStatusResponse getstatus() throws com.bitmechanic.barrister.RpcException {
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
	int bestChainHeight = controller.getMultiBitService().getChain().getBestChainHeight();

	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
	List<JSONRPCWalletStatus> wallets = new ArrayList<JSONRPCWalletStatus>();
	if (perWalletModelDataList != null) {
	    for (WalletData wd : perWalletModelDataList) {
		Wallet w = wd.getWallet();
		long lastSeenBlock = w.getLastBlockSeenHeight();
		boolean synced = (lastSeenBlock == bestChainHeight);
		if (wd.isBusy()) {
		    String key = wd.getBusyTaskKey();
		    if (key.equals("multiBitDownloadListener.downloadingText") ||
			    key.equals("singleWalletPanel.waiting.text")) {
			synced = false;
		    }
		}
		String filename = wd.getWalletFilename();
		String base = FilenameUtils.getBaseName(filename);
		JSONRPCWalletStatus ws = new JSONRPCWalletStatus(base, synced, lastSeenBlock);
		wallets.add(ws);
	    }
	}
	
	boolean connected = controller.getMultiBitService().getPeerGroup().numConnectedPeers() > 0;
	
	JSONRPCStatusResponse resp = new JSONRPCStatusResponse();
	resp.setConnected(connected);
	JSONRPCWalletStatus[] x = wallets.toArray(new JSONRPCWalletStatus[0]);
	resp.setWallets( x );
	return resp;
    }
    
    @Override
    public String[] listwallets() throws com.bitmechanic.barrister.RpcException {

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
	
	boolean isNameSane = sanityCheckName(name);
	if (!isNameSane) {
	    JSONRPCError.WALLET_NAME_BAD_CHARS.raiseRpcException();
	}
	if (name.startsWith(".") || name.endsWith(".")) {
	    JSONRPCError.WALLET_NAME_PERIOD_START_END.raiseRpcException();
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
                //FileHandler.writeUserPreferences(this.controller);
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
	String cleaned = FileNameCleaner.cleanFileName(name);
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
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset == null) {
	    JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	} else {
	    asset.setVisibility(visibility);
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_VISIBILITY_CHANGED, asset.getAssetID());
	}
	return true;
    }
    
    @Override
    public synchronized Boolean addasset(String walletID, String assetRefString) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	String s = assetRefString;
	if ((s != null) && (s.length() > 0)) {
	    s  = s.trim();
	    //System.out.println("asset ref detected! " + s);
	    CoinSparkAssetRef assetRef = new CoinSparkAssetRef();
	    if (assetRef.decode(s)) {
		Wallet wallet = this.controller.getModel().getActiveWallet();
		CSAssetDatabase assetDB = wallet.CS.getAssetDB();
		if (assetDB != null) {
		    CSAsset asset = new CSAsset(assetRef, CSAsset.CSAssetSource.MANUAL);
		    if (assetDB.insertAsset(asset) != null) {
			//System.out.println("Inserted new asset manually: " + asset);
		    } else {
			JSONRPCError.throwAsRpcException("Internal error, assetDB.insertAsset == null");
		    }
		}
	    } else {
		JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	    }
	}
	
	return true;
    }
    
    @Override
    public synchronized Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
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
	    JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	}

	return true;
    }

     @Override
   public JSONRPCAddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	List<JSONRPCAddressBookEntry> addresses = new ArrayList<JSONRPCAddressBookEntry>();
	
	String address, sparkAddress, label;
	    WalletInfoData addressBook = this.controller.getModel().getActiveWalletWalletInfo();
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
    public synchronized JSONRPCAddressBookEntry[] createaddress(String walletID, Long quantity) throws com.bitmechanic.barrister.RpcException {
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
	    wd.setBusyTaskKey("createaddress_jsonrpc");
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
	
	WalletInfoData addressBook = this.controller.getModel().getActiveWalletWalletInfo();
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
	    String filename = getFullPathForWalletName(walletID);
	    final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	    wd.setDirty(true);
	} else {
	    JSONRPCError.ADDRESS_NOT_FOUND.raiseRpcException();
	}
	
	return success;
    }
    
    @Override
    public JSONRPCTransaction[] listtransactions(String walletID, Long limit) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	if (limit>100) {
	    JSONRPCError.LIST_TRANSACTIONS_TOO_MANY.raiseRpcException();
	} else if (limit<=0) {
	    JSONRPCError.LIST_TRANSACTIONS_TOO_FEW.raiseRpcException();
	}
	
	List<JSONRPCTransaction> resultList = new ArrayList<JSONRPCTransaction>();
	
	
	int lastSeenBlock = controller.getMultiBitService().getChain().getBestChainHeight();

	List<Transaction> transactions = w.getRecentTransactions(limit.intValue(), false);
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
		fee = new Double( feeBTC.doubleValue() );
	    }
	    String txid = tx.getHashAsString();
	    ArrayList<JSONRPCTransactionAmount> amounts = getAssetTransactionAmounts(w, tx);
	    JSONRPCTransactionAmount[] amountsArray = amounts.toArray(new JSONRPCTransactionAmount[0]);

//	    int size = amounts.size();
//	    AssetTransactionAmountEntry[] entries = new AssetTransactionAmountEntry[size];
//	    int index = 0;
//	    for (JSONRPCTransactionAmount ata : amounts) {
//		entries[index++] = new AssetTransactionAmountEntry(ata.getAssetRef(), ata);
//	    }
	    JSONRPCTransaction atx = new JSONRPCTransaction(unixtime, confirmations, incoming, amountsArray, fee, txid);
	    resultList.add(atx);
	}
	
	JSONRPCTransaction[] resultArray = resultList.toArray(new JSONRPCTransaction[0]);
	return resultArray;
    }
    
    
    
    
    /*
    * Return array of asset transaction objects.
    * Original method is in CSMiscUtils, so any changes there, must be reflected here.
    */
    private ArrayList<JSONRPCTransactionAmount> getAssetTransactionAmounts(Wallet wallet, Transaction tx) {
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
		assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	    }
	    BigDecimal displayQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, netAmount);
	    JSONRPCTransactionAmount amount = new JSONRPCTransactionAmount();
	    amount.setAssetRef(assetRef);
	    amount.setDisplay(s1);
	    amount.setName(fullName);
	    amount.setNameShort(name);
	    amount.setQty(displayQty.doubleValue());
	    amount.setRaw(netAmount.longValue());
	    resultList.add(amount);
	}
	

	BigInteger satoshiAmount = receiveMap.get(0);
	satoshiAmount = satoshiAmount.subtract(sendMap.get(0));
	String btcAmount = Utils.bitcoinValueToFriendlyString(satoshiAmount) + " BTC";
	BigDecimal satoshiAmountBTC = new BigDecimal(satoshiAmount).divide(new BigDecimal(Utils.COIN));
	JSONRPCTransactionAmount amount = new JSONRPCTransactionAmount();
	    amount.setAssetRef("bitcoin");
	    amount.setDisplay(btcAmount);
	    amount.setName("Bitcoin");
	    amount.setNameShort("Bitcoin");
	    amount.setQty(satoshiAmountBTC.doubleValue());
	    amount.setRaw(satoshiAmount.longValue());
	    resultList.add(amount);
	
	return resultList;
    }
    
    @Override
    public JSONRPCBalance[] listbalances(String walletID, Boolean onlyVisible) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	int[] assetIDs = w.CS.getAssetIDs();
	if (assetIDs==null) {
	    return new JSONRPCBalance[0];
	}
	
	ArrayList<JSONRPCBalance> resultList = new ArrayList<>();

	// Add entry for BTC balances
	BigInteger rawBalanceSatoshi = w.getBalance(Wallet.BalanceType.ESTIMATED);
	BigInteger rawSpendableSatoshi = w.getBalance(Wallet.BalanceType.AVAILABLE);
	BigDecimal rawBalanceBTC = new BigDecimal(rawBalanceSatoshi).divide(new BigDecimal(Utils.COIN));
	BigDecimal rawSpendableBTC = new BigDecimal(rawSpendableSatoshi).divide(new BigDecimal(Utils.COIN));
	String rawBalanceDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";
	String rawSpendableDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";

	JSONRPCBalanceAmount bitcoinBalanceAmount = new JSONRPCBalanceAmount(rawBalanceSatoshi.longValue(), rawBalanceBTC.doubleValue(), rawBalanceDisplay);
	JSONRPCBalanceAmount bitcoinSpendableAmount = new JSONRPCBalanceAmount(rawSpendableSatoshi.longValue(), rawSpendableBTC.doubleValue(), rawSpendableDisplay);	
	JSONRPCBalance btcAssetBalance = new JSONRPCBalance();
	btcAssetBalance.setAssetRef("bitcoin");
	btcAssetBalance.setBalance(bitcoinBalanceAmount);
	btcAssetBalance.setSpendable(bitcoinSpendableAmount);
//	btcAssetBalance.setName("Bitcoin")
	resultList.add(btcAssetBalance);

	
	int n = assetIDs.length;
	Wallet.CoinSpark.AssetBalance assetBalance = null;
	for (int i=0; i<n; i++) {
	    int id = assetIDs[i];
	    if (id==0) continue;
	    CSAsset asset = w.CS.getAsset(id);
	    if (asset==null) continue;	    
	    if (onlyVisible && !asset.isVisible()) continue;
	    
	    String name=asset.getName();;
	    String nameShort=asset.getNameShort();;

	    if (name == null && asset != null) {
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
	    assetBalance = w.CS.getAssetBalance(id);
	    Long spendableRaw = assetBalance.spendable.longValue();
	    Double spendableQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, assetBalance.spendable).doubleValue();
	    String spendableDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.spendable);
	    JSONRPCBalanceAmount spendableAmount = new JSONRPCBalanceAmount(spendableRaw, spendableQty, spendableDisplay);
	    Long balanceRaw = assetBalance.total.longValue();
	    Double balanceQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, assetBalance.total).doubleValue();
	    String balanceDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.total);
	    JSONRPCBalanceAmount balanceAmount = new JSONRPCBalanceAmount(balanceRaw, balanceQty, balanceDisplay);
	    JSONRPCBalance ab = new JSONRPCBalance();
	    ab.setAssetRef(assetRef);
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
	    boolean isValid = (asset.getAssetState()!=CSAsset.CSAssetState.VALID);
	// FIXME: Check num confirms too?
	    ab.setValid(isValid);
	    ab.setChecked_unixtime(asset.getValidChecked().getTime()/1000L);
	    ab.setContract_url(asset.getContractUrl());
	    ab.setContract_file(asset.getContractPath());
	    ab.setGenesis_txid(asset.getGenTxID());
	    ab.setAdded_unixtime(asset.getDateCreation().getTime()/1000L);

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
	    } else {
		//ab.setExpiry_date("Never expires");
	    }
	    ab.setTracker_urls(asset.getCoinsparkTrackerUrls());
	    ab.setIcon_url(asset.getIconUrl());
	    ab.setImage_url(asset.getImageUrl());
	    ab.setFeed_url(asset.getFeedUrl());
	    ab.setRedemption_url(asset.getRedemptionUrl());
	    ab.setVisible(asset.isVisible());
	    resultList.add(ab);
	}
	
	JSONRPCBalance[] resultArray = resultList.toArray(new JSONRPCBalance[0]);
	return resultArray;
    }
    
    
    @Override
    public synchronized String sendbitcoin(String walletID, String address, Double amount) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	if (amount<=0.0) {
	    JSONRPCError.SEND_BITCOIN_AMOUNT_TOO_LOW.raiseRpcException();
	}
	
	String bitcoinAddress = address;
	if (address.startsWith("s")) {
	    bitcoinAddress = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
	    if (bitcoinAddress==null) {
		JSONRPCError.COINSPARK_ADDRESS_INVALID.raiseRpcException();
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
	    wd.setBusyTaskKey("sendbitcoin_jsonrpc");
	    this.controller.fireWalletBusyChange(true);
	}

	
	
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

	    // Note - Request is populated with the AES key in the SendBitcoinNowAction after the user has entered it on the SendBitcoinConfirm form.
	    // Complete it (which works out the fee) but do not sign it yet.
	    System.out.println(">>>> Just about to complete the tx (and calculate the fee)...");

	    w.completeTx(sendRequest, false);
	    sendValidated = true;
	    System.out.println(">>>> The fee after completing the transaction was " + sendRequest.fee);
	    // Let's do it for real now.

	    Transaction sendTransaction = this.controller.getMultiBitService().sendCoins(wd, sendRequest, null);
	    if (sendTransaction == null) {
		    // a null transaction returned indicates there was not
		// enough money (in spite of our validation)
		JSONRPCError.SEND_BITCOIN_INSUFFICIENT_MONEY.raiseRpcException();
	    } else {
		sendSuccessful = true;
		sendTxHash = sendTransaction.getHashAsString();
		System.out.println(">>>> Sent transaction was:\n" + sendTransaction.toString());
	    }

	    if (sendSuccessful) {
		// There is enough money.
	    } else {
		// There is not enough money
	    }
//      } catch (WrongNetworkException e1) {
//      } catch (AddressFormatException e1) {
//      } catch (KeyCrypterException e1) {
	} catch (InsufficientMoneyException e) {
	    JSONRPCError.SEND_BITCOIN_INSUFFICIENT_MONEY.raiseRpcException();
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Could not send bitcoin due to error", e);
	} finally {
	    // Save the wallet.
	    try {
		this.controller.getFileHandler().savePerWalletModelData(wd, false);
	    } catch (WalletSaveException e) {
//        log.error(e.getMessage(), e);
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
	String sendTxHash = null;
	boolean sendValidated = false;
	boolean sendSuccessful = false;
	
	Wallet w = getWalletForWalletName(walletID);
	if (w==null) {
	    JSONRPCError.WALLET_NOT_FOUND.raiseRpcException();
	}
	
	if (quantity<=0.0) {
	    JSONRPCError.SEND_ASSET_AMOUNT_TOO_LOW.raiseRpcException();
	}
	
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
	    wd.setBusyTaskKey("sendasset_jsonrpc");
	    this.controller.fireWalletBusyChange(true);
	}
	
	try {
	    // -- boilerplate ends here....

	    CSAsset asset = getAssetForAssetRefString(w, assetRef);
	    if (asset==null) {
		JSONRPCError.ASSETREF_NOT_FOUND.raiseRpcException();
	    }
	    
	    if (asset.getAssetState()!=CSAsset.CSAssetState.VALID) {
		JSONRPCError.ASSET_STATE_INVALID.raiseRpcException();
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
	    
            String sendAmount = Utils.bitcoinValueToPlainString(BitcoinModel.COINSPARK_SEND_MINIMUM_AMOUNT);	    
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

                // Complete it (which works out the fee) but do not sign it yet.
                System.out.println("Just about to complete the tx (and calculate the fee)...");

		// there is enough money, so let's do it for real now
		    w.completeTx(sendRequest, false);
	    sendValidated = true;
	    System.out.println(">>>> The fee after completing the transaction was " + sendRequest.fee);
	    // Let's do it for real now.

	    Transaction sendTransaction = this.controller.getMultiBitService().sendCoins(wd, sendRequest, null);
	    if (sendTransaction == null) {
		    // a null transaction returned indicates there was not
		// enough money (in spite of our validation)
		JSONRPCError.ASSET_INSUFFICIENT_BALANCE.raiseRpcException();
	    } else {
		sendSuccessful = true;
		sendTxHash = sendTransaction.getHashAsString();
		System.out.println(">>>> Sent transaction was:\n" + sendTransaction.toString());
	    }

	    if (sendSuccessful) {
		// There is enough money.
	    } else {
		// There is not enough money
	    }
	    
	    //--- bolilerplate begins...
	} catch (InsufficientMoneyException ime) {
	    JSONRPCError.ASSET_INSUFFICIENT_BALANCE.raiseRpcException();
	} catch (Exception e) {
	    JSONRPCError.throwAsRpcException("Could not send asset due to error: " , e);
	} finally {
	    // Save the wallet.
	    try {
		this.controller.getFileHandler().savePerWalletModelData(wd, false);
	    } catch (WalletSaveException e) {
//        log.error(e.getMessage(), e);
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
}
