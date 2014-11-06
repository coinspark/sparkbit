package org.sparkbit.jsonrpc.autogen;

/**
 * DO NOT EDIT THIS FILE!
 * 
 * Generated by Barrister Idl2Java: https://github.com/coopernurse/barrister-java
 * 
 * Generated at: Wed Nov 05 17:34:08 PST 2014
 */
public class SparkBitJSONRPCServiceClient implements SparkBitJSONRPCService {

    private com.bitmechanic.barrister.Transport _trans;

    public SparkBitJSONRPCServiceClient(com.bitmechanic.barrister.Transport trans) {
        trans.getContract().setPackage("org.sparkbit.jsonrpc.autogen");
        trans.getContract().setNsPackage("org.sparkbit.jsonrpc.autogen");
        this._trans = trans;
    }

    public StatusResponse getstatus() throws com.bitmechanic.barrister.RpcException {
        Object _params = null;
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.getstatus", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (StatusResponse)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public ListWalletsResponse listwallets() throws com.bitmechanic.barrister.RpcException {
        Object _params = null;
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.listwallets", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (ListWalletsResponse)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public Boolean deletewallet(String walletID) throws com.bitmechanic.barrister.RpcException {
        Object _params = new Object[] { walletID };
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.deletewallet", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (Boolean)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public Boolean setassetvisible(String walletID, String assetRef, Boolean visibility) throws com.bitmechanic.barrister.RpcException {
        Object _params = new Object[] { walletID, assetRef, visibility };
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.setassetvisible", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (Boolean)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public Boolean addasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
        Object _params = new Object[] { walletID, assetRef };
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.addasset", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (Boolean)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public Boolean refreshasset(String walletID, String assetRef) throws com.bitmechanic.barrister.RpcException {
        Object _params = new Object[] { walletID, assetRef };
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.refreshasset", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (Boolean)_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

    public AddressBookEntry[] listaddresses(String walletID) throws com.bitmechanic.barrister.RpcException {
        Object _params = new Object[] { walletID };
        com.bitmechanic.barrister.RpcRequest _req = new com.bitmechanic.barrister.RpcRequest(java.util.UUID.randomUUID().toString(), "SparkBitJSONRPCService.listaddresses", _params);
        com.bitmechanic.barrister.RpcResponse _resp = this._trans.request(_req);
        if (_resp == null) {
            return null;
        }
        else if (_resp.getError() == null) {
            return (AddressBookEntry[])_resp.getResult();
        }
        else {
            throw _resp.getError();
        }
    }

}

