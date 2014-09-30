### Introduction

SparkBit is a Simplified Payment Verification (SPV) desktop client for the CoinSpark and Bitcoin protocols.

SparkBit relies on the following technologies:

* Maven as the build system, so the usual Maven processes apply. If you're not familiar
with Maven then [download it first](http://maven.apache.org) and follow their installation instructions.
* A CoinSpark aware version of [Bitcoinj](https://github.com/coinspark/sparkbit-bitcoinj) for access to the Bitcoin network.  The [original Bitcoinj](https://code.google.com/p/bitcoinj/)) does not yet support CoinSpark.
* [Bitcoinj Enforcer Rules](https://github.com/gary-rowe/BitcoinjEnforcerRules) to prevent dependency chain attacks

SparkBit uses [JWrapper](http://www.jwrapper.com/) to create binaries for Windows, Mac, Linux.

SparkBit is based on a fork of MultiBit 0.5.18, which relies on other technologies which have been retained in the repository even though they are not actively being used.
* [ZXing ("Zebra Crossing")](https://code.google.com/p/zxing/) for QR codes
* IzPack for creating installers for Windows, Mac, Linux
* [XChange](https://github.com/timmolter/XChange) for access to several Bitcoin exchanges

### SparkBit's Bitcoinj and 'The Bitcoinj "Alice" dependency'

SparkBit is based on MultiBit which itself depended on a special fork of Bitcoinj, due to legacy wallet serialization issues.  The basis for SparkBit's bitcoinj is MultiBit 0.5.18's bitcoinj fork, specifically branch bcj-0.11.2-mb-alice, available from:

```
https://code.google.com/r/jimburton618-bitcoinj-coinbase-tx/source/checkout
```

SparkBit has added support for CoinSpark assets to its own version of this branch.  You may want to review the modified library for yourself.  You can clone from this fork:

```
https://github.com/coinspark/sparkbit-bitcoinj
```

The branch you should use for the SparkBit master code is: `master`. The branch you should use for the SparkBit develop code is: `develop`

Once cloned, you can install the custom SparkBit Bitcoinj library using

```
mvn clean install -DskipTests
```

### Branching strategy

This follows the ["master-develop" or "Git flow"](http://nvie.com/posts/a-successful-git-branching-model/) pattern.

There are 2 main branches: `master` and `develop`. The `master` branch is exclusively for releases, while the `develop`
is exclusively for release candidates. The `develop` branch always has a Maven version of `develop-SNAPSHOT`.

Every GitHub Issue gets a branch off develop. When it is complete and code reviewed it is merged into `develop`.

When sufficient Issues are merged into `develop` to justify a release, a new branch off `develop` is created with the release number (e.g. `release-1.2.3`).
The Maven `pom.xml` is updated to reflect the snapshot version (e.g. `1.2.3-SNAPSHOT`).

Once the release has been tested and is ready to go live, the final act is to update the `pom.xml` to remove the SNAPSHOT suffix and merge it into `master`.

The `master` branch is then tagged with the release number. Tags are in the format `v1.2.3` to distinguish them from branch names.

An announcement is made on the SparkBit website to alert everyone that a new version is available.

### Maven build targets

The important targets are:

```
mvn clean package -DskipTests
```

which will package the MultiBit project into `sparkbit-x.y.z.jar` where `x.y.z` is the current version
number. This is suitable for local development work.

If you want to generate a final jar file, suitable for command-line execution, you use the following command

```
mvn clean install -DskipTests
```

After some processing, you will have the following artifacts in the target directory:

* an executable jar = sparkbit-exe-full.jar

To build the platform installers, switch to your jwrapper directory, and follow the instructions there.

To run MultiBit from the command-line, you can use the following command

```
java -jar sparkbit-exe-full.jar
```

### Custom configuration

SparkBit is quite flexible and has several features only accessible to power users through the configuration file. This
is discussed in more detail in [configuration.md](configuration.md)

### Contributing

If you would like to contribute please feel free to get in touch using [simon@coinsciences.com](mailto:simon@coinsciences.com).
We are particularly looking for developers with the following skills to contribute:

* Experienced Java programmers
* Beta testers for checking the latest pre-release

All contributors must be OK with releasing their work under the MIT license.
