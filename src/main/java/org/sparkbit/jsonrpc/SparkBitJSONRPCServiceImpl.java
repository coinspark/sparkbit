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
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.codec.digest.DigestUtils;
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

/**
 *
 * @author ruffnex
 */
public class SparkBitJSONRPCServiceImpl implements SparkBitJSONRPCService {
    
    private BitcoinController controller;
    private MultiBitFrame mainFrame;
    private HashMap<String,String> walletFilenameMap;
    
    public SparkBitJSONRPCServiceImpl() {
	this.controller = JSONRPCController.INSTANCE.getBitcoinController();
	this.mainFrame = JSONRPCController.INSTANCE.getMultiBitFrame();
	walletFilenameMap = new HashMap<>();
	updateWalletFilenameMap();
    }
    
    /*
    getstatus
	{
		'connected': true/false,
		'synchronized': true/false, [if we are up to do with netweork]
		'blocks': <number of last block seen>,
	}
    */
    @Override
    public StatusResponse getstatus() throws com.bitmechanic.barrister.RpcException {
	StatusEnum status = this.mainFrame.getOnlineStatus();
	boolean connected = status == StatusEnum.ONLINE;
//	int lastSeenBlock = controller.getModel().getActiveWallet().getLastBlockSeenHeight();
	int lastSeenBlock = controller.getMultiBitService().getChain().getBestChainHeight();

	boolean replayTaskRunning = ReplayManager.INSTANCE.getCurrentReplayTask()!=null ;
	boolean regularDownloadRunning = ReplayManager.isRegularDownloadRunning();
	boolean synced = !regularDownloadRunning && !replayTaskRunning;
	
	// A regular download can be running in the background, because there is a new block,
	// and thus we are behind by one block, but the UI will still show that we synced.
	// We will consider ourselves to be out of sync if we are two or more blocks behind our peers.
	PeerGroup pg = controller.getMultiBitService().getPeerGroup();
	if (pg!=null) {
	    Peer peer = pg.getDownloadPeer();
	    if (peer != null) {
		int n = peer.getPeerBlockHeightDifference();
		if (synced && n>=2) {
		    synced = false;
		}
	    }
	}
	
	StatusResponse resp = new StatusResponse();
	resp.setBlocks((long)lastSeenBlock);
	resp.setConnected(connected);
	resp.setSynced(synced);
	return resp;
    }
    
    @Override
    public ListWalletsResponse listwallets() throws com.bitmechanic.barrister.RpcException {

	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
	List<ListWallet> wallets = new ArrayList<ListWallet>();
	if (perWalletModelDataList != null) {
	    for (WalletData loopPerWalletModelData : perWalletModelDataList) {
		String filename = loopPerWalletModelData.getWalletFilename();
		String digest = DigestUtils.md5Hex(filename);
		String description = loopPerWalletModelData.getWalletDescription();
		ListWallet lw = new ListWallet(digest, description);
		wallets.add(lw);
		
		// store/update local cache
		walletFilenameMap.put(digest, filename);
	    }
	}

	ListWallet[] resultArray = wallets.toArray(new ListWallet[0]);
	ListWalletsResponse resp = new ListWalletsResponse();
	resp.setWallets(resultArray);
	return resp;
    }
    
    @Override
    public Boolean createwallet(String description) throws com.bitmechanic.barrister.RpcException {
	LocalDateTime dt = new DateTime().toLocalDateTime();
	String name = "jsonrpc_" + dt.toString("MMDDYYYY") + "_" + dt.toString("HHmmss.SSS");
	
	String newWalletFilename = controller.getApplicationDataDirectoryLocator().getApplicationDataDirectory() + File.separator + name + MultiBitService.WALLET_SUFFIX;
	File newWalletFile = new File(newWalletFilename);
	if (newWalletFile.exists()) {
	    throw new RpcException(555, "Could not create new wallet, filename already used, try again");
	}
	
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
                this.controller.getModel().setActiveWalletByFilename(newWalletFilename);
                //controller.getModel().setUserPreference(BitcoinModel.GRAB_FOCUS_FOR_ACTIVE_WALLET, "true");

                // Save the user properties to disk.
                //FileHandler.writeUserPreferences(this.controller);
                //log.debug("User preferences with new wallet written successfully");

                // Backup the wallet and wallet info.
                BackupManager.INSTANCE.backupPerWalletModelData(controller.getFileHandler(), perWalletModelData);
                
                controller.fireRecreateAllViews(true);
                controller.fireDataChangedUpdateNow();
	} catch (Exception e) {
	    throw new RpcException(555, "Could not create new wallet, error: " + e.toString());	    
	}
      
	return true;
    }
	
    @Override
    public Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException {
	String filename = getFilenameForWalletID(walletID);
	if (filename==null) {
	    // TODO: setup and declare error codes
	    throw new RpcException(100, "Could not find a wallet with that ID");
	}

	Wallet w = getWalletForWalletID(walletID);
	if (w == null) {
	    throw new RpcException(100, "Could not find a wallet with that ID");
	}
	

//	String filename = getFilenameForWalletID(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
WalletInfoData winfo = wd.getWalletInfo();
	if (wd.isBusy()) {
	    throw new RpcException(300, "Wallet is busy");
	}
	
	// Unhook it from the PeerGroup.
	this.controller.getMultiBitService().getPeerGroup().removeWallet(w);

	// Remove it from the model.
      this.controller.getModel().remove(wd);

      // Set the new Wallet to be the active wallet.
      if (!this.controller.getModel().getPerWalletModelDataList().isEmpty()) {
        WalletData firstPerWalletModelData = this.controller.getModel().getPerWalletModelDataList().get(0);
        this.controller.getModel().setActiveWalletByFilename(firstPerWalletModelData.getWalletFilename());
      }
	controller.fireRecreateAllViews(true);

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
	    throw new RpcException(444, "Error deleting wallet files: " + e.toString());
	}
	
	if (!winfo.isDeleted()) {
	    throw new RpcException(445, "Wallet was not deleted. Reason unknown.");
	}
	return true;
    }

    private void updateWalletFilenameMap() {
	List<WalletData> perWalletModelDataList = controller.getModel().getPerWalletModelDataList();
	List<ListWallet> wallets = new ArrayList<ListWallet>();
	if (perWalletModelDataList != null) {
	    for (WalletData loopPerWalletModelData : perWalletModelDataList) {
		String filename = loopPerWalletModelData.getWalletFilename();
		String digest = DigestUtils.md5Hex(filename);
		walletFilenameMap.put(digest, filename);
		// TODO: Synchronized, make thread safe.
	    }
	}
	
    }
    
    private String getFilenameForWalletID(String walletID) {
	return walletFilenameMap.get(walletID);
    }
    
    private Wallet getWalletForWalletID(String walletID) {
	Wallet w = null;
	String filename = getFilenameForWalletID(walletID);
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
    
    public Boolean setassetvisible(String walletID, String assetRef, Boolean visibility) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset != null) {
	    asset.setVisibility(visibility);
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_VISIBILITY_CHANGED, asset.getAssetID());
	    return true;
	}
	throw new RpcException(200, "Asset ref not found");	    
    }
    
    public Boolean addasset(String walletID, String assetRefString) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");
	
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
		throw new RpcException(201, "Internal error, assetDB.insertAsset == null");
		    }
		}
	    } else {
		throw new RpcException(200, "Asset ref not valid");
	    }
	}
	
	return true;
    }
    
    public Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");
	CSAsset asset = getAssetForAssetRefString(w, assetRef);
	if (asset != null) {
	    asset.setRefreshState();
			// Note: the event can be fired, but the listener can do nothing if in headless mode.
	    // We want main asset panel to refresh, since there isn't an event fired on manual reset.
	    CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_UPDATED, asset.getAssetID());
	} else {
	    throw new RpcException(200, "Asset ref not found");
	}

	return true;
    }

    public AddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");

	List<AddressBookEntry> addresses = new ArrayList<AddressBookEntry>();
	
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
				AddressBookEntry entry = new AddressBookEntry(label, address, sparkAddress);
				addresses.add(entry);
			    }
                        }
                    }
                }
            }
	
	AddressBookEntry[] resultArray = addresses.toArray(new AddressBookEntry[0]);
	return resultArray;
    }
    
    // TODO: Should we remove limit of 100 addresses?
    public AddressBookEntry[] createaddress(String walletID, Long quantity) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);
	if (w == null) {
	    throw new RpcException(100, "Could not find a wallet with that ID");
	}

	int qty = quantity.intValue();
	if (qty <= 0) {
	    throw new RpcException(101, "Quantity must be at least 1");
	}
	if (qty > 100) {
	    throw new RpcException(102, "Quantity can not be greater than 100");
	}

	String filename = getFilenameForWalletID(walletID);
	final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	if (wd.isBusy()) {
	    throw new RpcException(300, "Wallet is busy");
	} else {
	    wd.setBusy(true);
	    wd.setBusyTaskKey("createaddress_jsonrpc");
	    this.controller.fireWalletBusyChange(true);
	}

	List<AddressBookEntry> addresses = new ArrayList<AddressBookEntry>();

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
		AddressBookEntry entry = new AddressBookEntry(label, lastAddressString, sparkAddress);
		addresses.add(entry);
	    }

	    // Backup the wallet and wallet info.
	    BackupManager.INSTANCE.backupPerWalletModelData(fileHandler, wd);
	} catch (KeyCrypterException kce) {
	    throw new RpcException(900, "Create addresses failed with KeyCrypterException: " + kce.toString());
	} catch (Exception e) {
	    throw new RpcException(902, "Create addresses failed with an Exception: " + e.toString());
	} finally {
	    // Declare that wallet is no longer busy with the task.
	    wd.setBusyTaskKey(null);
	    wd.setBusy(false);
	    this.controller.fireWalletBusyChange(false);
	}

	
	CSEventBus.INSTANCE.postAsync(new SBEvent(SBEventType.ADDRESS_CREATED));
	wd.setDirty(true);
	
	AddressBookEntry[] resultArray = addresses.toArray(new AddressBookEntry[0]);
	return resultArray;

	// TODO: Fire an event to trigger receive panel to update addresses being displayed
    }

    
    public Boolean setaddresslabel(String walletID, String address, String label) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");

	if (address.startsWith("s")) {
	    address = CSMiscUtils.getBitcoinAddressFromCoinSparkAddress(address);
	    if (address==null) {
		throw new RpcException(800, "CoinSpark address invalid");
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
	    String filename = getFilenameForWalletID(walletID);
	    final WalletData wd = this.controller.getModel().getPerWalletModelDataByWalletFilename(filename);
	    wd.setDirty(true);
	} else {
	    throw new RpcException(700, "Could not find the address");
	}
	
	return success;
    }
    
    public AssetTransaction[] listtransactions(String walletID, Long limit) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");
	if (limit>100) throw new RpcException(100, "Number of transactions cannot be greater than 100");
	if (limit<=0) throw new RpcException(100, "Number of transactions must be at least 1");
	List<AssetTransaction> resultList = new ArrayList<AssetTransaction>();
	
	
	int lastSeenBlock = controller.getMultiBitService().getChain().getBestChainHeight();

	List<Transaction> transactions = w.getRecentTransactions(limit.intValue(), false);
	for (Transaction tx : transactions) {
	    
	    Date txDate = controller.getModel().getDateOfTransaction(controller, tx);
	    long unixtime = txDate.getTime(); // unix epoch
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
	    ArrayList<AssetTransactionAmount> amounts = getAssetTransactionAmounts(w, tx);
	    AssetTransactionAmount[] amountsArray = amounts.toArray(new AssetTransactionAmount[0]);

//	    int size = amounts.size();
//	    AssetTransactionAmountEntry[] entries = new AssetTransactionAmountEntry[size];
//	    int index = 0;
//	    for (AssetTransactionAmount ata : amounts) {
//		entries[index++] = new AssetTransactionAmountEntry(ata.getAssetRef(), ata);
//	    }
	    AssetTransaction atx = new AssetTransaction(unixtime, confirmations, incoming, amountsArray, fee, txid);
	    resultList.add(atx);
	}
	
	AssetTransaction[] resultArray = resultList.toArray(new AssetTransaction[0]);
	return resultArray;
    }
    
    
    
    
    /*
    * Return array of asset transaction objects.
    * Original method is in CSMiscUtils, so any changes there, must be reflected here.
    */
    private ArrayList<AssetTransactionAmount> getAssetTransactionAmounts(Wallet wallet, Transaction tx) {
	if (wallet==null || tx==null) return null;
	
	Map<Integer, BigInteger> receiveMap = wallet.CS.getAssetsSentToMe(tx);
	Map<Integer, BigInteger> sendMap = wallet.CS.getAssetsSentFromMe(tx);

//	System.out.println(">>>> tx = " + tx.getHashAsString());
//	System.out.println(">>>>     receive map = " +  receiveMap);
//	System.out.println(">>>>     send map = " +  sendMap);
	
	//Map<String, String> nameAmountMap = new TreeMap<>();
	ArrayList<AssetTransactionAmount> resultList = new ArrayList<>();
	
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
	    // Create AssetTransactionAmount and add it to list
	    // 
	    String fullName = "";
	    String assetRef = "null";
	    if (asset!=null) {
		fullName = asset.getName();
		assetRef = CSMiscUtils.getHumanReadableAssetRef(asset);
	    }
	    BigDecimal displayQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, netAmount);
	    AssetTransactionAmount amount = new AssetTransactionAmount();
	    amount.setAssetRef(assetRef);
	    amount.setDisplay(s1);
	    amount.setName(fullName);
	    amount.setName_short(name);
	    amount.setQty(displayQty.doubleValue());
	    amount.setRaw(netAmount.longValue());
	    resultList.add(amount);
	}
	

	BigInteger satoshiAmount = receiveMap.get(0);
	satoshiAmount = satoshiAmount.subtract(sendMap.get(0));
	String btcAmount = Utils.bitcoinValueToFriendlyString(satoshiAmount) + " BTC";
	BigDecimal satoshiAmountBTC = new BigDecimal(satoshiAmount).divide(new BigDecimal(Utils.COIN));
	AssetTransactionAmount amount = new AssetTransactionAmount();
	    amount.setAssetRef("bitcoin");
	    amount.setDisplay(btcAmount);
	    amount.setName("Bitcoin");
	    amount.setName_short("Bitcoin");
	    amount.setQty(satoshiAmountBTC.doubleValue());
	    amount.setRaw(satoshiAmount.longValue());
	    resultList.add(amount);
	
	return resultList;
    }
    
    public AssetBalance[] listbalances(String walletID, Boolean onlyvisible) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletID(walletID);
	if (w==null) throw new RpcException(100, "Could not find a wallet with that ID");
	
	int[] assetIDs = w.CS.getAssetIDs();
	if (assetIDs==null) throw new RpcException(999, "Internal error, getAssetIDs returned null");
	
	ArrayList<AssetBalance> resultList = new ArrayList<>();

	// Add entry for BTC balances
	BigInteger rawBalanceSatoshi = w.getBalance(Wallet.BalanceType.ESTIMATED);
	BigInteger rawSpendableSatoshi = w.getBalance(Wallet.BalanceType.AVAILABLE);
	BigDecimal rawBalanceBTC = new BigDecimal(rawBalanceSatoshi).divide(new BigDecimal(Utils.COIN));
	BigDecimal rawSpendableBTC = new BigDecimal(rawSpendableSatoshi).divide(new BigDecimal(Utils.COIN));
	String rawBalanceDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";
	String rawSpendableDisplay = Utils.bitcoinValueToFriendlyString(rawBalanceSatoshi) + " BTC";

	AssetBalanceAmount bitcoinBalanceAmount = new AssetBalanceAmount(rawBalanceSatoshi.longValue(), rawBalanceBTC.doubleValue(), rawBalanceDisplay);
	AssetBalanceAmount bitcoinSpendableAmount = new AssetBalanceAmount(rawSpendableSatoshi.longValue(), rawSpendableBTC.doubleValue(), rawSpendableDisplay);	
	AssetBalance btcAssetBalance = new AssetBalance("bitcoin", bitcoinBalanceAmount, bitcoinSpendableAmount, "Bitcoin", "Bitcoin");
	resultList.add(btcAssetBalance);

	
	int n = assetIDs.length;
	Wallet.CoinSpark.AssetBalance assetBalance = null;
	for (int i=0; i<n; i++) {
	    int id = assetIDs[i];
	    if (id==0) continue;
	    CSAsset asset = w.CS.getAsset(id);
	    if (asset==null) continue;	    
	    if (onlyvisible && !asset.isVisible()) continue;
	    
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
	    AssetBalanceAmount spendableAmount = new AssetBalanceAmount(spendableRaw, spendableQty, spendableDisplay);
	    Long balanceRaw = assetBalance.total.longValue();
	    Double balanceQty = CSMiscUtils.getDisplayUnitsForRawUnits(asset, assetBalance.total).doubleValue();
	    String balanceDisplay = CSMiscUtils.getFormattedDisplayStringForRawUnits(asset, assetBalance.total);
	    AssetBalanceAmount balanceAmount = new AssetBalanceAmount(balanceRaw, balanceQty, balanceDisplay);
	    AssetBalance ab = new AssetBalance(assetRef, balanceAmount, spendableAmount, name, nameShort);
	    resultList.add(ab);
	}
	
	AssetBalance[] resultArray = resultList.toArray(new AssetBalance[0]);
	return resultArray;
    }
}
