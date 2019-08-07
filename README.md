# TezosJ_plainjava
A Java SDK for Tezos node interactions with [Conseil](https://cryptonomic.github.io/Conseil/#/) support.

The TezosJ SDK library enables plain Java developers to create applications that communicates with Tezos blockchain.

The library is written in Java and is based on Gradle framework. This repository contains the library source code and a Main class to test some features.

## Requirements

- Java 8
- Windows / Linux (not tested yet) / Mac
- Eclipse or another Java IDE.

## Getting started

- Clone the repository, import as a Gradle Project into your Java IDE and run the Main class.
- Or download the JAR (https://github.com/tezosRio/TezosJ_plainJava/blob/master/tezosj-sdk-plain-java-1.0.0.jar) and add to your project's classpath.
- Or (soon)... Download the JAR file from JCENTER (bintray.com/milfont/tezos/tezosj_plainjava/1.0.0/tezosj-sdk-plain-java-1.0.0.jar) and put in your project's classpath.
- Or (soon)... Add to your build.gradle dependencies: compile 'com.milfont.tezos:tezosj_plainjava:1.0.0'  

## Usage

```java
    // Set proxy, if needed.
    // Global.proxyHost = "myProxyHost";
    // Global.proxyPort = "myProxyPort";

    // Creates a new wallet with just a passphrase.
    // TezosWallet wallet = new TezosWallet("myPassphrase");

    // Imports a previously owned wallet with mnemonic words and passphrase.
    // TezosWallet wallet = new TezosWallet("word1, word2, ..., word15", "passphrase");

    // Creates (imports) a new wallet with its keys.
    // TezosWallet wallet = new TezosWallet(privateKey, publicKey, publicKeyHash, passPhrase);

    // Creates a new wallet by reading from file.
    // TezosWallet wallet = new TezosWallet(true, "c:\\temp\\mySavedWallet.txt", "myPassphrase");

    // Shows loaded wallet data.
    // System.out.println(wallet.getMnemonicWords());
    // System.out.println(wallet.getPublicKeyHash());
    // System.out.println(wallet.getBalance());

    // Example of origination operation.   
    // BigDecimal fee = new BigDecimal("0.001300"); // Needed fee for origination.
    // BigDecimal amount = new BigDecimal("2"); // Starting new kt1_delegator address balance.
    // JSONObject jsonObject = wallet.originate(wallet2.getPublicKeyHash(), true, true, fee, "", "", amount, "", "");
    // System.out.println(jsonObject.get("result"));

    // Example of delegation operation.
    // BigDecimal fee = new BigDecimal("0.001300");
    // JSONObject jsonObject = wallet.delegate("kt1_delegatorAddress", "tz1_delegate_address", fee, "", "");
    // System.out.println(jsonObject.get("result"));
       
    // Example of undelegation operation.
    // BigDecimal fee = new BigDecimal("0.001300");
    // JSONObject jsonObject = wallet.undelegate("kt1_delegatorAddress", fee);
    // System.out.println(jsonObject.get("result"));

    // Example of Sending funds.
    // BigDecimal amount = new BigDecimal("1");
    // BigDecimal fee = new BigDecimal("0.00142");
    // JSONObject jsonObject = wallet.send("tz1FromAddress", "tz1ToAddress", amount, fee, "", "");
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
- Originate a KT address.
- Delegate to a known baker.

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
