package milfont.com.tezosj.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

public class FA12Wallet extends TezosWallet implements FA12
{
   private String contractAddress = "";

   // Constructor with passPhrase.
   // This will create a new token and generate new keys and mnemonic words.
   public FA12Wallet(String passPhrase, String provider) throws Exception
   {
      super(passPhrase);
      setProvider(provider);
   }

   public FA12Wallet(String privateKey, String publicKey, String publicKeyHash, String passPhrase, String provider,
         String contractAddress) throws Exception
   {
      super(privateKey, publicKey, publicKeyHash, passPhrase);
      this.contractAddress = contractAddress;
      setProvider(provider);  
   }

   // Constructor for previously media persisted (saved) token.
   // This will load an existing tokenfrom media.
   public FA12Wallet(Boolean loadFromFile, String pathToFile, String p, String provider, String contractAddress)
   {
      super(loadFromFile, pathToFile, p);
      this.contractAddress = contractAddress;
      setProvider(provider);      
   }
   
   @Override
   public JSONObject approve(Integer amount, String f, String t) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "approve", new String[]
            { String.valueOf(amount), f, t }, false);

      return jsonObject;
   }

   @Override
   public JSONObject burn(String address, Integer amount) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "burn", new String[]
            { address, String.valueOf(amount) }, false);

      return jsonObject;

   }

   @Override
   public JSONObject getAdministrator(String address) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "getAdministrator", new String[]
            { address }, false);

      return jsonObject;

   }

   @Override
   public JSONObject getAllowance(Map arg, String target) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "getAllowance", new String[]
            { (String) arg.get("owner"), (String) arg.get("spender"), target }, false);

      return jsonObject;
   }

   @Override
   public JSONObject getBalance(Map arg, String target) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "getBalance", new String[]
            { (String) arg.get("owner"), target }, false);

      return jsonObject;
   }

   @Override
   public JSONObject getTotalSupply(String address) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "getTotalSupply", new String[]
            { address }, false);

      return jsonObject;
   }

   @Override
   public JSONObject mint(String address, Integer amount) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "mint", new String[]
            { address, String.valueOf(amount) }, false);

      return jsonObject;
   }

   @Override
   public JSONObject setAdministrator(String address) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "setAdministrator", new String[]
            { address }, false);

      return jsonObject;

   }

   @Override
   public JSONObject setPause(Boolean status) throws Exception
   {
      String strStatus = String.valueOf(status);
      String capitalized = strStatus.substring(0, 1).toUpperCase() + strStatus.substring(1);
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "setPause", new String[]
            { capitalized }, false);

      return jsonObject;
   }

   @Override
   public JSONObject transfer(Integer amount, String f, String t) throws Exception
   {
      JSONObject jsonObject = callContractEntryPoint(getPublicKeyHash(), this.contractAddress, new BigDecimal("0"),
            new BigDecimal("0.1"), "", "", "transfer", new String[]
            { String.valueOf(amount), f, t }, false);

      return jsonObject;

   }


   public ArrayList<Map> getContractStorage() throws Exception
   {
      ArrayList<Map> items = new ArrayList<Map>();
      
      items = (ArrayList<Map>) getContractStorage(this.contractAddress);
      
      return items;
   }

}
