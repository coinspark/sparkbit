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

/**
 *
 * @author ruffnex
 */
public class SparkBitJSONRPCServiceImpl implements SparkBitJSONRPCService {
    
    private BitcoinController controller;
    
    public SparkBitJSONRPCServiceImpl() {
	this.controller = JSONRPCController.INSTANCE.getBitcoinController();
    }
    
    @Override
    public StatusResponse getstatus() throws com.bitmechanic.barrister.RpcException {
	StatusResponse resp = new StatusResponse();
	resp.setBlocks(42L);
	resp.setConnected(Boolean.TRUE);
	resp.setSynced(Boolean.TRUE);
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
