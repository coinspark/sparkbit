### SparkBit configuration options

All these options are set in the main control file `sparkbit.properties`. SparkBit looks in a few places for this file in the following order:

1. Current working directory when launched. This is for backwards compatibility and for running from a USB drive.

2. (Mac OS X only) Four directory levels up. This is for running from a USB drive, but outside the OSX `.app` directory.
 
3. The operating system's standard application data directory:

#### Windows

* `System.getenv("APPDATA")/SparkBit-Data`
* Example: `C:/Documents and Settings/Administrator/Application Data/SparkBit-Data`
* Example: `C:/Users/benny/AppData/Roaming/SparkBit-Data`

Note that the folder may be hidden and you may need to configure Windows Explorer to show hidden files on [Windows 7](http://windows.microsoft.com/en-us/windows/show-hidden-files#show-hidden-files=windows-7) and [Windows 8](http://blogs.msdn.com/b/zxue/archive/2012/03/08/win8-howto-19-show-hidden-files-folders-and-drives.aspx).

#### Mac OS X

* `System.getProperty("user.home")/Library/Application Support/SparkBit-Data`
* Example: `/Users/benny/Library/Application Support/SparkBit-Data`

If you are running OS X 10.7 or later, the Library folder may be hidden from view in Finder, so you will have to [enable viewing the Library folder on Mac OS X](http://www.macworld.com/article/2057221/how-to-view-the-library-folder-in-mavericks.html).

#### Linux

* `System.getProperty("user.home")/SparkBit-Data`
* Example: `/Users/benny/SparkBit-Data`

Wherever this file is found, that directory is used as the application data directory for Sparkbit.


#### Connect to specific peers

If you want to connect to specific peers set `peers=<comma separated list of peers to connect to>`

The list of peers can be specified using domain names (`www.myNode.com`) or IP addresses. Example:

```
peers=173.242.119.177, 176.9.42.247, 217.79.19.226, 98.216.173.54
```

Note: ONLY these peers will be used for connections.

#### Testnet

To use Testnet3 set `testOrProductionNetwork=testnet3`.

### Developer Tools

To enable a menu option to display some tools, which are subject to change at any time:

```
showDeveloperTools=true
```

### Expert Options

These options are useful for developers but are subject to change at any time and removal from future updates:

```
canDeleteInvalidAssets=true
```

Allow deleting an asset in an invalid state which has a zero balance.
Useful if an asset is add manually by asset reference, but the reference is invalid.
SparkBit 0.9.3 and later.

```
canSendInvalidAssets=true
```

Allow sending an asset which is in a temporary invalid state.

Caution: It is not recommended that you enable this. SparkBit 0.9.3 and later.


```
httpsTrustAllCerts=true
```

Allow connecting to an issuer website which has a self-signed SSL certificate.
Useful for testing, but not recommended that you enable this. SparkBit 0.9.3 and later.


```
sendAssetWithJustOneConfirmation
```

Allow sending a new asset which has just been issued after just one blockchain.
Useful when you are creating new assets to test issuance and transfer.  SparkBit 0.9 and later.

```
showPrivateKeyMenu=true
```

Show a menu to allow the import and export of Private keys to a wallet.  SparkBit 0.9.3 and later.
