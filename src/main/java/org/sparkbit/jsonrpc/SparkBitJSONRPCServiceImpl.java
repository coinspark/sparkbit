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
    public Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException {
	String filename = getFilenameForWalletID(walletID);
	if (filename==null) {
	    // TODO: setup and declare error codes
	    throw new RpcException(100, "Could not find a wallet with that ID");
	}
	
	// Perform delete if possible etc.
	
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
	String filename = getFilenameForWalletID(walletID);

	int qty = quantity.intValue();
	if (qty <= 0) {
	    throw new RpcException(101, "Quantity must be at least 1");
	}
	if (qty > 100) {
	    throw new RpcException(102, "Quantity can not be greater than 100");
	}

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
	    for (ECKey newKey : newKeys) {
		String lastAddressString = newKey.toAddress(this.controller.getModel().getNetworkParameters()).toString();
		String label = "Created by JSONRPC";
		wd.getWalletInfo().addReceivingAddress(new WalletAddressBookData(label, lastAddressString),
			false);

		// Coinspark address
		String sparkAddress = CSMiscUtils.convertBitcoinAddressToCoinSparkAddress(lastAddressString);
		if (sparkAddress != null) {
		    AddressBookEntry entry = new AddressBookEntry(label, lastAddressString, sparkAddress);
		    addresses.add(entry);
		}
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

	AddressBookEntry[] resultArray = addresses.toArray(new AddressBookEntry[0]);
	return resultArray;

	// TODO: Fire an event to trigger receive panel to update addresses being displayed
    }

}
