package milfont.com.tezosj.model;

public interface FA12
{
   
   // Under maintenance...
   
   public void transfer(String contractAddress, String from, String to, Integer value) throws Exception;
   public void approve(String contractAddress, String spender, Integer value) throws Exception;
}

