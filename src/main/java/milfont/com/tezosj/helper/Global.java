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
