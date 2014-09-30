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
package org.multibit.exchange;

public interface CurrencyConverterListener {

    /**
     * The exchange rate is no longer valid as it is out of date
     */
    public void lostExchangeRate(ExchangeRate exchangeRate);
    
    /**
     * The exchange rate is now valid (for the first time)
     * @param exchangeRate
     */
    public void foundExchangeRate(ExchangeRate exchangeRate);
    
    /**
     * An updated value of the exchange rate is available
     * @param exchangeRate
     */
    public void updatedExchangeRate(ExchangeRate exchangeRate);
}
