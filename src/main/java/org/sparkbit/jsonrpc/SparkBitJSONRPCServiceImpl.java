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
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.model.core.StatusEnum;
import org.multibit.network.ReplayManager;
import com.google.bitcoin.core.*;

/**
 *
 * @author ruffnex
 */
public class SparkBitJSONRPCServiceImpl implements SparkBitJSONRPCService {
    
    private BitcoinController controller;
    private MultiBitFrame mainFrame;
    
    public SparkBitJSONRPCServiceImpl() {
	this.controller = JSONRPCController.INSTANCE.getBitcoinController();
	this.mainFrame = JSONRPCController.INSTANCE.getMultiBitFrame();
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
	List<String> names = new ArrayList<String>();
	if (perWalletModelDataList != null) {
	    for (WalletData loopPerWalletModelData : perWalletModelDataList) {
		String name = loopPerWalletModelData.getWalletDescription();// Filename();
		names.add(name);
	    }
	}

	String[] nameArray = names.toArray(new String[0]);
	ListWalletsResponse resp = new ListWalletsResponse();
	resp.setWallets(nameArray);
	return resp;
    }
    
}
