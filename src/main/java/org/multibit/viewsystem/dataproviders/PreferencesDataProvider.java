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
package org.multibit.viewsystem.dataproviders;

import java.awt.Font;


/**
 * DataProvider for the preferences screen and actions
 * @author jim
 *
 */
public interface PreferencesDataProvider extends DataProvider { 
    /**
     * Get the previous 'undo changes' text
     */
    public String getPreviousUndoChangesText();
    
    
//    /**
//     * Get the previous send fee
//     */
//    public String getPreviousSendFee();
//    
//    /**
//     * Get the new send fee
//     */
//    public String getNewSendFee();
    
    
    /**
     * Get the previous user language code
     */
    public String getPreviousUserLanguageCode();
    
    /**
     * Get the new user language code
     */
    public String getNewUserLanguageCode();
    
      
    /**
     * Get the new open URI dialog text
     */
    public String getOpenUriDialog();
    
       
    /**
     * Get the new open URI use URI text
     */
    public String getOpenUriUseUri();
    
    
    /**
     * get the previous font name
     */
    public String getPreviousFontName();
    
    /**
     * get the new font name
     */
    public String getNewFontName();
    
    
    /**
     * get the previous font style
     */
    public String getPreviousFontStyle();
    
    /**
     * get the new font style
     */
    public String getNewFontStyle();
   
    /**
     * get the previous font size
     */
    public String getPreviousFontSize();
    
    /**
     * get the new font size
     */
    public String getNewFontSize();
    
    /**
     * get the new font
     */
    public Font getSelectedFont();

    /**
     * ticker information
     * @return
     */
    public boolean getPreviousShowCurrency();
    public boolean getNewShowCurrency();
    
    public boolean getPreviousShowRate();
    public boolean getNewShowRate();
    
    public boolean getPreviousShowBid();
    public boolean getNewShowBid();
    
    public boolean getPreviousShowAsk();
    public boolean getNewShowAsk();
    
    public boolean getPreviousShowExchange();
    public boolean getNewShowExchange();
    
    public String getPreviousExchange1();
    public String getNewExchange1();
    
    public String getPreviousCurrency1();
    public String getNewCurrency1();
 
    public boolean getPreviousShowSecondRow();
    public boolean getNewShowSecondRow();

    public String getPreviousExchange2();
    public String getNewExchange2();

    public String getPreviousCurrency2();
    public String getNewCurrency2();


    boolean isTickerVisible();

    boolean getPreviousShowTicker();
    boolean getNewShowTicker();
    
    String getPreviousLookAndFeel();
    String getNewLookAndFeel();


    boolean getNewShowBitcoinConvertedToFiat();
    boolean getPreviousShowBitcoinConvertedToFiat();


    String getNewOpenExchangeRatesApiCode();


    String getPreviousOpenExchangeRatesApiCode();
    
    String[] getPreviousMessagingServerURLs();
    String[] getNewMessagingServerURLs();
    
}
