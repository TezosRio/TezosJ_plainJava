package milfont.com.tezosj.model;

import java.math.BigInteger;

import org.json.JSONObject;

public interface FA12_Interface
{
   public JSONObject FA12_transfer(String targetContract, String from, String to, BigInteger value) throws Exception;
   public JSONObject FA12_approve(String targetContract, String spender, BigInteger value) throws Exception;
   public JSONObject FA12_getAllowance(String targetContract, String owner, String spender) throws Exception;
   public JSONObject FA12_getBalance(String targetContract, String owner) throws Exception;
   public JSONObject FA12_getTotalSupply(String targetContract) throws Exception;
}
