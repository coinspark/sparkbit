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

/**
 * JSON-RPC Error Codes
 * Inspired by http://stackoverflow.com/questions/446663/best-way-to-define-error-codes-strings-in-java
 */
public enum JSONRPCError {
  WALLET_NOT_FOUND(1, "Wallet ID not found."),
  WALLET_ID_ALREADY_EXISTS(2, "Wallet ID already exists."), // should not happen
  
  THROW_EXCEPTION(99999, "Internal error detected: "); // Code reserved for throwing a general exception
  

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
      throw new RpcException(this.code, this.description);
  }
  
  public static void throwAsRpcException(Exception e) throws Exception {
      String s = e.getMessage();
      if (s==null) s = e.toString();
      throw new RpcException(THROW_EXCEPTION.getCode(), THROW_EXCEPTION.getDescription() + s);
  }
  
}