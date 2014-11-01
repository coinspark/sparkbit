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
import org.apache.commons.codec.digest.DigestUtils;
import org.coinspark.protocol.CoinSparkAssetRef;
import org.coinspark.wallet.CSAsset;
import org.coinspark.wallet.CSAssetDatabase;
import org.coinspark.wallet.CSEventBus;
import org.coinspark.wallet.CSEventType;
import org.multibit.utils.CSMiscUtils;


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
    
    public Boolean setassetvisible(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException
    {
	Wallet w = getWalletForWalletID(walletID);
	return true;
    }
    
    public Boolean addasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);
	return true;
    }
    
    public Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
	Wallet w = getWalletForWalletID(walletID);

	CSAssetDatabase db = w.CS.getAssetDB();
	int[] assetIDs = w.CS.getAssetIDs();
	if (assetIDs != null) {
	    for (int id : assetIDs) {
		CSAsset asset = db.getAsset(id);
		if (asset != null) {
		    //CoinSparkAssetRef ref = asset.getAssetReference();
		    String s = CSMiscUtils.getHumanReadableAssetRef(asset);
		    if (s.equals(assetRef)) {
//		System.out.println("asset id = " + id);
			asset.setRefreshState();
			// Note: the event can be fired, but the listener can do nothing if in headless mode.
			// We want main asset panel to refresh, since there isn't an event fired on manual reset.
			CSEventBus.INSTANCE.postAsyncEvent(CSEventType.ASSET_UPDATED, asset.getAssetID());
			break;
		    }
		}
	    }
	}

	return true;
    }

}
