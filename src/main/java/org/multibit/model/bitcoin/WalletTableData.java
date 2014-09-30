/* 
 * SparkBit
 *
 * Copyright 2011-2014 multibit.org
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
package org.multibit.model.bitcoin;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Date;

import com.google.bitcoin.core.Transaction;
import java.util.Map;
import java.util.*;

import org.coinspark.wallet.CSAsset;
import com.google.bitcoin.core.Wallet;
import java.util.TreeSet;

import org.coinspark.wallet.CSTransactionAssets;
import org.multibit.utils.CSMiscUtils;

/**
 * Class used to store the data in the Transactions table in a quick to access data form.
 */
public class WalletTableData {

    private Transaction transaction;
    
    /**
     * The height of the block this transaction appears in.
     */
    private int height;

    // TODO Consider using Joda Time (java.util.Date is obsolete)
    private Date date;
    private String description;
    private BigInteger debit;
    private BigInteger credit;

    private CSTransactionAssets txAssets;
    
    public WalletTableData(Transaction transaction) {
        this.transaction = transaction;
	
	if (transaction!=null) {
	    this.txAssets = new CSTransactionAssets(transaction);
	}
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Date getDate() {
        // Avoids exposing internal state
        if (date == null) {
            return null;
        } else {
            return new Date(date.getTime());
        }
    }

    public void setDate(Date date) {
        if (date == null) {
            this.date = null;
        } else {
            this.date = new Date(date.getTime());
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigInteger getDebit() {
        return debit;
    }

    public void setDebit(BigInteger debit) {
        this.debit = debit;
    }

    public BigInteger getCredit() {
        return credit;
    }

    public void setCredit(BigInteger credit) {
        this.credit = credit;
    }

    // FIXME: Replace with get first (or biggest) asset, and then return its name
    // Ok with just one asset.
    public String getAssetDomain(Wallet wallet) {
	Map<Integer, BigInteger> map = wallet.CS.getAssetsSentToMe(this.transaction);
	if (map.keySet().size() == 0) {
	    map = wallet.CS.getAssetsSentFromMe(this.transaction);
	}
	if (map.keySet().size() == 0) {
	    return "";
	}

	String name = "";

	Set<Integer> keys = map.keySet();
	TreeSet sortedKeys = new TreeSet(keys);
	Integer assetID = (Integer) sortedKeys.last();
	if (assetID>0) {
	    CSAsset asset = wallet.CS.getAsset(assetID);
	    //name = asset.getName();
	    name = asset.getContractUrl();
	}
        return name;
    }
    
    public boolean isGenesis() {
	return this.txAssets.getGenesis() != null;
    }
    
    public boolean isTransfer() {
	return this.txAssets.getTransfers()!= null;
    }
    
    public String getFormattedAssetDebitCredit(Wallet wallet) { //, int assetID) {
        Map<Integer, BigInteger> receiveMap = wallet.CS.getAssetsSentToMe(this.transaction);
	Map<Integer, BigInteger> sendMap = wallet.CS.getAssetsSentFromMe(this.transaction);
	// iterate through, skip if integer is 0 as we can call regular
	// methods to get BTC data.
	Integer assetID = null;
	BigDecimal netAmount = null;
	for (Map.Entry<Integer, BigInteger> entry : receiveMap.entrySet()) {
	    assetID = entry.getKey();
	    if (assetID == null || assetID == 0) continue;
	    BigInteger receivedAmount = entry.getValue();
	    BigInteger sentAmount = sendMap.get(assetID);
	    netAmount = BigDecimal.ZERO;
	    if (receivedAmount != null) netAmount = netAmount.add(new BigDecimal(receivedAmount));
	    if (sentAmount != null) netAmount = netAmount.subtract(new BigDecimal(sentAmount));
	    break; // TODO: return the first asset we find, in future return map<Integer,BigInteger>
	}
	
	if (netAmount == null) return "";
	CSAsset asset = wallet.CS.getAsset(assetID);
	netAmount = CSMiscUtils.getDisplayUnitsForRawUnits(asset, netAmount.toBigInteger());	
	return CSMiscUtils.getFormattedDisplayString(asset, netAmount);
    }
    
}
