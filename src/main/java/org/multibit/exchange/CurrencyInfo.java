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

public class CurrencyInfo {

    String currencyCode;
    
    String currencySymbol;
    
    boolean isPrefix;
    
    boolean hasSeparatingSpace = false;

    public CurrencyInfo(String currencyCode, String currencySymbol, boolean isPrefix) {
        super();
        this.currencyCode = currencyCode;
        this.currencySymbol = currencySymbol;
        this.isPrefix = isPrefix;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public boolean isPrefix() {
        return isPrefix;
    }

    public void setPrefix(boolean isPrefix) {
        this.isPrefix = isPrefix;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((currencyCode == null) ? 0 : currencyCode.hashCode());
        result = prime * result + ((currencySymbol == null) ? 0 : currencySymbol.hashCode());
        result = prime * result + (isPrefix ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof CurrencyInfo))
            return false;
        CurrencyInfo other = (CurrencyInfo) obj;
        if (currencyCode == null) {
            if (other.currencyCode != null)
                return false;
        } else if (!currencyCode.equals(other.currencyCode))
            return false;
        if (currencySymbol == null) {
            if (other.currencySymbol != null)
                return false;
        } else if (!currencySymbol.equals(other.currencySymbol))
            return false;
        if (isPrefix != other.isPrefix)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CurrencyInfo [currencyCode=" + currencyCode + ", currencySymbol=" + currencySymbol + ", isPrefix=" + isPrefix + "]";
    }

    public boolean isHasSeparatingSpace() {
        return hasSeparatingSpace;
    }

    public void setHasSeparatingSpace(boolean hasSeparatingSpace) {
        this.hasSeparatingSpace = hasSeparatingSpace;
    }
    
}
