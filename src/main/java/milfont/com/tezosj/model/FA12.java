package milfont.com.tezosj.model;

import java.util.ArrayList;
import java.util.Map;

import org.json.JSONObject;

public interface FA12
{

   public JSONObject approve(Integer amount, String f, String t) throws Exception;

   public JSONObject burn(String address, Integer amount) throws Exception;

   public JSONObject getAdministrator(String address) throws Exception;

   public JSONObject getAllowance(Map arg, String target) throws Exception;

   public JSONObject getBalance(Map arg, String target) throws Exception;

   public JSONObject getTotalSupply(String address) throws Exception;

   public JSONObject mint(String address, Integer amount) throws Exception;

   public JSONObject setAdministrator(String address) throws Exception;

   public JSONObject setPause(Boolean status) throws Exception;

   public JSONObject transfer(Integer amount, String f, String t) throws Exception;

}

