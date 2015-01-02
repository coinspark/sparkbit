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
import java.lang.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON-RPC Error Codes
 * Inspired by http://stackoverflow.com/questions/446663/best-way-to-define-error-codes-strings-in-java
 */
public enum JSONRPCError {
  WALLET_NOT_FOUND(1, "Wallet ID not found."),
  WALLET_ID_ALREADY_EXISTS(2, "Wallet ID already exists."), // should not happen
  
  DELETE_WALLET_NOT_EMPTY(3, "Cannot delete wallet as it is not empty"),
  WALLEY_IS_BUSY(4, "Wallet is busy"),
  ASSETREF_NOT_FOUND(5, "Could not find an asset with that asset reference in the wallet"),
  CREATE_ADDRESS_TOO_MANY(6, "Number of addresses to create cannot be more than 100"),
  CREATE_ADDRESS_TOO_FEW(7, "Number of addresses to create must be at least 1"),
  COINSPARK_ADDRESS_INVALID(8, "CoinSpark address is invalid"),
  ADDRESS_NOT_FOUND(9, "Address not found in wallet"),
  LIST_TRANSACTIONS_TOO_MANY(10, "Number of transactions cannot be more than 100"),
  LIST_TRANSACTIONS_TOO_FEW(11, "Number of transactions must be at least 1"),
  SEND_BITCOIN_AMOUNT_TOO_LOW(12, "Amount of bitcoin must be greater than 0"),
  BITCOIN_ADDRESS_INVALID(13, "Bitcoin address is invalid"),
  SEND_BITCOIN_INSUFFICIENT_MONEY(14, "Insufficient funds to send bitcoin"),
  SEND_ASSET_AMOUNT_TOO_LOW(15, "Amount of asset must be greater than 0"),
  ADDRESS_NOT_COINSPARK_ADDRESS(16, "Address must be a CoinSpark address"),
  COINSPARK_ADDRESS_MISSING_ASSET_FLAG(17, "CoinSpark address does not have asset flag set"),
  ASSET_STATE_INVALID(18, "Asset not in valid state for sending"),
  ASSET_NOT_CONFIRMED(19, "Asset does not have enough confirmations to be sent"),
  ASSET_INSUFFICIENT_BALANCE(20, "Insufficient funds to send asset"),
  CREATE_WALLET_FAILED(21, "Could not create wallet"),
  WALLET_NAME_BAD_CHARS(22, "Wallet name should not contain illegal characters"),
  WALLET_NAME_PERIOD_START_END(23, "Wallet name cannot start or end with a period character"),
  DELETE_ASSET_FAILED(24, "Could not delete asset"),
  DELETE_ASSET_NONZERO_BALANCE(25, "Cannot delete asset because it still has a balance"),
  ASSETREF_INVALID(26, "Asset reference is not valid"),
  DELETE_INVALID_ASSET_FAILED(27,"Cannot delete an invalid asset with a non-zero balance"),
  CONFIRMATIONS_TOO_LOW(28,"Number of confirmations cannot be negative"),
  
  THROW_EXCEPTION(99999, ""); // RESERVED. For wrapping up a general exception and throwing it.
  

  private static final Logger log = LoggerFactory.getLogger(JSONRPCError.class);

  private final int code;
  private final String description;

  private JSONRPCError(int code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getDescription() {
     return description;
  }

  public int getCode() {
     return code;
  }

  @Override
  public String toString() {
    return code + ": " + description;
  }
  
    // Convenience function
    public void raiseRpcException() throws RpcException {
	log.error(this.toString());
	throw new RpcException(this.code, this.description);
    }
    
    // Convenience function, raise predefined exception with extra info.
    public void raiseRpcException(String message) throws RpcException {
	log.error(this.toString() + " : " + message);
	throw new RpcException(this.code, this.description + " : " + message);
    }

    public static void throwAsRpcException(String message) throws RpcException {
	log.error(THROW_EXCEPTION.getCode() + ": " + message);
	throw new RpcException(THROW_EXCEPTION.getCode(), message);
    }

    public static void throwAsRpcException(String message, Exception e) throws RpcException {
	String s = e.getMessage();
	if (s == null) {
	    s = e.toString();
	}
	log.error(THROW_EXCEPTION.getCode() + ": " + message + " : " + s);
	throw new RpcException(THROW_EXCEPTION.getCode(), message + " : " + s);
    }
  
}