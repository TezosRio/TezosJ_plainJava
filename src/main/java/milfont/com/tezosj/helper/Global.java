package milfont.com.tezosj.helper;

import java.security.KeyStore;
import java.security.KeyStoreException;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class Global
{
	public static KeyStore myKeyStore = null;
	public static Boolean ignoreInvalidCertificates = false;
	public static String proxyHost = "";
	public static String proxyPort = "";
	public static String defaultProvider = "https://mainnet.tezrpc.me";
	public static OkHttpClient myOkhttpClient = null;
	public static Builder myOkhttpBuilder = null; 
	public static String ledgerDerivationPath = "";
	public static String ledgerTezosFolderPath = "";
	public static String ledgerTezosFilePath = "";
   public static String KT_TO_TZ_GAS_LIMIT = "26283";
   public static String KT_TO_TZ_STORAGE_LIMIT = "0";
   public static String KT_TO_TZ_FEE = "0.005";
   public static String KT_TO_KT_GAS_LIMIT = "44725";
   public static String KT_TO_KT_STORAGE_LIMIT = "0";
   public static String KT_TO_KT_FEE = "0.005";

   
	public static void initKeyStore() throws KeyStoreException
	{
		if (myKeyStore == null) 
		{
           myKeyStore = KeyStore.getInstance("JCEKS");
		}
	}

	public static void initOkhttp() throws Exception
	{
		if (myOkhttpClient == null)
		{
           myOkhttpClient = new OkHttpClient();

           if (myOkhttpBuilder == null)
   		   {
   		      myOkhttpBuilder = myOkhttpClient.newBuilder();
   		   }
		}
	}

	
}
