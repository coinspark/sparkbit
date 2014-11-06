package org.sparkbit.jsonrpc.autogen;

/**
 * DO NOT EDIT THIS FILE!
 * 
 * Generated by Barrister Idl2Java: https://github.com/coopernurse/barrister-java
 * 
 * Generated at: Wed Nov 05 17:34:08 PST 2014
 */

public interface SparkBitJSONRPCService {

    public StatusResponse getstatus() throws com.bitmechanic.barrister.RpcException;
    public ListWalletsResponse listwallets() throws com.bitmechanic.barrister.RpcException;
    public Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException;
    public Boolean setassetvisible(String walletID, String assetRef, Boolean visibility) throws com.bitmechanic.barrister.RpcException;
    public Boolean addasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException;
    public Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException;
    public AddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException;

}

