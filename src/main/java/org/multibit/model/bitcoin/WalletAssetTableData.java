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
import java.util.Date;

import org.coinspark.wallet.CSAsset;

/**
 * Class used to store the data in the Asset table in a quick to access data form.
 */
public class WalletAssetTableData {

    private CSAsset asset;
    
    /**
     * The height of the block this transaction appears in.
     */
    //private int height;

    // TODO Consider using Joda Time (java.util.Date is obsolete)
    private Date date;
    private String description;
//    private BigInteger debit;
//    private BigInteger credit;

    public WalletAssetTableData(CSAsset asset) {
        this.asset = asset;
    }

    public CSAsset getAsset() {
        return asset;
    }

    public void setAsset(CSAsset asset) {
        this.asset = asset;
    }

//    public int getHeight() {
//        return height;
//    }
//
//    public void setHeight(int height) {
//        this.height = height;
//    }

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

//    public BigInteger getDebit() {
//        return debit;
//    }
//
//    public void setDebit(BigInteger debit) {
//        this.debit = debit;
//    }
//
//    public BigInteger getCredit() {
//        return credit;
//    }
//
//    public void setCredit(BigInteger credit) {
//        this.credit = credit;
//    }


}
