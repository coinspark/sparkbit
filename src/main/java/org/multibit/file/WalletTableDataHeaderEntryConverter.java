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
package org.multibit.file;

import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.exchange.CurrencyInfo;
import org.multibit.model.bitcoin.WalletTableData;

import com.googlecode.jcsv.writer.CSVEntryConverter;

/**
 * Create a CSVEntryConverter for the header values in the CSV
 */
public class WalletTableDataHeaderEntryConverter implements CSVEntryConverter<WalletTableData> {

    BitcoinController bitcoinController = null;

    @Override
    public String[] convertEntry(WalletTableData walletTableData) {
        String[] columns = new String[5];

        // Date.
        columns[0] = bitcoinController.getLocaliser().getString("walletData.dateText");
        
        // Description.
        columns[1] = bitcoinController.getLocaliser().getString("walletData.descriptionText");

        // Amount in BTC.
        columns[2] = bitcoinController.getLocaliser().getString("sendBitcoinPanel.amountLabel") + " (" + bitcoinController.getLocaliser().getString("sendBitcoinPanel.amountUnitLabel") + ")";;
        
        // Amount in fiat
        if (CurrencyConverter.INSTANCE.isShowingFiat()) {
            CurrencyInfo currencyInfo = CurrencyConverter.INSTANCE.getCurrencyCodeToInfoMap().get(CurrencyConverter.INSTANCE.getCurrencyUnit().getCode());
            String currencySymbol = CurrencyConverter.INSTANCE.getCurrencyUnit().getCode();
            if (currencyInfo != null) {
                currencySymbol = currencyInfo.getCurrencySymbol();
            }
            columns[3] = bitcoinController.getLocaliser().getString("sendBitcoinPanel.amountLabel") + " (" + currencySymbol + ")";
        } else {
            columns[3] = "";
        }
         
        // Transaction hash.
        columns[4] = bitcoinController.getLocaliser().getString("exportTransactionsSubmitAction.transactionId");

        return columns;
    }

    public void setBitcoinController(BitcoinController bitcoinController) {
        this.bitcoinController = bitcoinController;
    }
}
