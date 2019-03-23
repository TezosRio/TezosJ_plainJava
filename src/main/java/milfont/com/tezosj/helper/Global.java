package milfont.com.tezosj.helper;

import java.security.KeyStore;
import java.security.KeyStoreException;

public class Global
{
	public static KeyStore myKeyStore = null;
	public static boolean ignoreInvalidCertificates = false;
	public static String proxyHost = "";
	public static String proxyPort = "";
	public static String defaultProvider = "https://mainnet.tezrpc.me";
	
	public static void initKeyStore() throws KeyStoreException
	{
		if (myKeyStore == null)
		{
           myKeyStore = KeyStore.getInstance("JCEKS");
		}
	}
	
	public static Integer tryParseInt(String text) {
		try {
		  return Integer.parseInt(text.replaceAll("[^\\d.]", ""));
		} catch (NumberFormatException e) {
		  return null;
		}
	  }
}
