# TezosJ_plainjava
A Java SDK for Tezos node interactions with [Conseil](https://cryptonomic.github.io/Conseil/#/) support.

The TezosJ SDK library enables plain Java developers to create applications that communicates with Tezos blockchain.

The library is written in Java and is based on Gradle framework. This repository contains the library source code and a Main class to test some features.

## Requirements

- Java 8
- Windows / Linux (not tested yet) / Mac
- Eclipse or another Java IDE.

## Getting started

- Clone the repository, import into your Java IDE and run the Main class.
- Or download the JAR (https://github.com/tezosRio/TezosJ_plainJava/blob/master/tezosj-sdk-plain-java-0.9.8.jar) and add to your project's classpath.
- Or (soon)... Download the JAR file from JCENTER (bintray.com/milfont/tezos/tezosj_plainjava/0.9.8/tezosj-sdk-plain-java-0.9.6.jar) and put in your project's classpath.
- Or (soon)... Add to your build.gradle dependencies: compile 'com.milfont.tezos:tezosj_plainjava:0.9.8'

## Usage

```java
    // Set proxy, if needed.
    // Global.proxyHost = "myProxyHost";
    // Global.proxyPort = "myProxyPort";

    // Tells system to ignore invalid certificates, if needed.
    // Global.ignoreInvalidCertificates = false;

    // Creates a new wallet with a passphrase.
    TezosWallet wallet = new TezosWallet("myPassphrase");

    // Shows some wallet data output.
    System.out.println(wallet.getMnemonicWords());
    System.out.println(wallet.getPublicKeyHash());
    System.out.println(wallet.getBalance());

    // Imports a previously owned wallet with mnemonic words and passphrase.
    // TezosWallet wallet2 = new TezosWallet("word1 word2 ... word15", "myPassphrase");

    // Shows some wallet data output.
    // System.out.println(wallet2.getMnemonicWords());
    // System.out.println(wallet2.getPublicKeyHash());
    // System.out.println(wallet2.getBalance());

    // Saves the current wallet from memory to file.
    wallet.save("c:\\temp\\mySavedWallet.txt");

    System.out.println("Saved the wallet to disk.");

    // Creates a new wallet by reading from file.
    TezosWallet myLoadedWallet = new TezosWallet(true, "c:\\temp\\mySavedWallet.txt", "myPassphrase");

    System.out.println("Loaded the wallet from disk:");

    // Shows loaded wallet data.
    System.out.println(myLoadedWallet.getMnemonicWords());
    System.out.println(myLoadedWallet.getPublicKeyHash());
    System.out.println(myLoadedWallet.getBalance());

    // Example of Sending funds.
    // BigDecimal amount = new BigDecimal("1");
    // BigDecimal fee = new BigDecimal("0.00142");
    // JSONObject jsonObject = wallet2.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
    // System.out.println(jsonObject.get("result"));

    // Using Conseil Gateway from Cryptonomic.
    // ConseilGateway cg = new ConseilGateway(new URL("<URL>"), "<APIKEY>", "alphanet");
```

## Disclaimer

This software is at Beta stage. It is currently experimental and still under development. Many features are not fully tested/implemented yet. This version uses Tezos Mainnet (!)

## Features

- Create valid Tezos wallet address
- Import Tezos wallet
- Check if an address is valid
- Get account balance
- Send funds
- Retrieve account information and transactions via Conseil.

The main purpose of TezosJ SDK library is to foster development of applications in plain Java that interacts with Tezos ecosystem. This might open Tezos to a whole world of software producers, ready to collaborate with the platform. TezosJ is to play the role of a layer that will translate default Java method calls to Tezos network real operations (create_account, transfer_token, etc.)

## Credits

- TezosJ is based on Stephen Andrews' EZTZ Javascript library https://github.com/stephenandrews/eztz.
- TezosJ is also based on ConseilJS from Cryptonomic https://github.com/Cryptonomic/ConseilJS
- TezosJ uses LazySodium https://github.com/terl/lazysodium-java
- TezosJ uses BitcoinJ Java Library https://github.com/bitcoinj/bitcoinj.
- Special thanks to Tezzigator (https://twitter.com/@tezzigator) for providing the code for Tezos Key Generation in Java.

## License

The TezosJ SDK library is available under the MIT License. Check out the license file for more information.

## See also

TezosJ SDK for Android.
