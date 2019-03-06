package milfont.com.tezosj.model;

/**
 * Created by Milfont on 31/07/2018.
 */

// Encypted keys.

public class EncKeys
{
    private byte[] encPublicKey;
    private byte[] encPrivateKey;
    private byte[] encPublicKeyHash;
    private String encP;
    private String encIv;
    private int myRandomID;


    public EncKeys(byte[] encPublicKey, byte[] encPrivateKey, byte[] encPublicKeyHash, int myRandomID)
    {
       this.encPublicKey = encPublicKey;
       this.encPrivateKey = encPrivateKey;
       this.encPublicKeyHash = encPublicKeyHash;
       this.myRandomID = myRandomID;
    }

        
    public int getMyRandomID()
    {
		return myRandomID;
	}


	public void setMyRandomID(int myRandomID)
	{
		this.myRandomID = myRandomID;
	}


	public byte[] getEncPublicKey()
    {
        return encPublicKey;
    }

    public void setEncPublicKey(byte[] encPublicKey)
    {
        this.encPublicKey = encPublicKey;
    }

    public byte[] getEncPrivateKey()
    {
        return encPrivateKey;
    }

    public void setEncPrivateKey(byte[] encPrivateKey)
    {
        this.encPrivateKey = encPrivateKey;
    }

    public byte[] getEncPublicKeyHash()
    {
        return encPublicKeyHash;
    }

    public void setEncPublicKeyHash(byte[] encPublicKeyHash)
    {
        this.encPublicKeyHash = encPublicKeyHash;
    }

    public String getEncP()
    {
        return encP;
    }

    public void setEncP(String encP)
    {
        this.encP = encP;
    }

    public String getEncIv()
    {
        return encIv;
    }

    public void setEncIv(String encIv)
    {
        this.encIv = encIv;
    }
}
