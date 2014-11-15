package org.sparkbit.jsonrpc.autogen;

/**
 * DO NOT EDIT THIS FILE!
 * 
 * Generated by Barrister Idl2Java: https://github.com/coopernurse/barrister-java
 * 
 * Generated at: Fri Nov 14 18:54:32 PST 2014
 */

public interface sparkbit {

    public Boolean createwallet(String walletID) throws com.bitmechanic.barrister.RpcException;
    public String sendasset(String walletID, String address, String assetRef, Double quantity, Boolean senderPays) throws com.bitmechanic.barrister.RpcException;
    public JSONRPCStatusResponse getstatus() throws com.bitmechanic.barrister.RpcException;
    public String[] listwallets() throws com.bitmechanic.barrister.RpcException;
    public Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException;
    public Boolean setassetvisible(String walletID, String assetRef, Boolean visibility) throws com.bitmechanic.barrister.RpcException;
    public Boolean addasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException;
    public Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException;
    public JSONRPCAddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException;
    public JSONRPCAddressBookEntry[] createaddress(String walletID, Long quantity) throws com.bitmechanic.barrister.RpcException;
    public Boolean setaddresslabel(String walletID, String address, String label) throws com.bitmechanic.barrister.RpcException;
    public JSONRPCTransaction[] listtransactions(String walletID, Long limit) throws com.bitmechanic.barrister.RpcException;
    public JSONRPCBalance[] listbalances(String walletID, Boolean onlyVisible) throws com.bitmechanic.barrister.RpcException;
    public String sendbitcoin(String walletID, String address, Double amount) throws com.bitmechanic.barrister.RpcException;

}

