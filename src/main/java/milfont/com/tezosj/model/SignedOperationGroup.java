package milfont.com.tezosj.model;

public class SignedOperationGroup
{

    private byte[] bytes;
    private String signature;
    private String sbytes;

    public SignedOperationGroup(byte[] bytes, String signature, String sbytes)
    {
        this.bytes = bytes;
        this.signature = signature;
        this.sbytes = sbytes;
    }

    public String getSbytes()
    {
        return sbytes;
    }

    public void setSbytes(String sbytes)
    {
        this.sbytes = sbytes;
    }

    public byte[] getTheBytes()
    {
        return bytes;
    }

    public void setBytes(byte[] bytes)
    {
        this.bytes = bytes;
    }

    public String getSignature()
    {
        return signature;
    }

    public void setSignature(String signature)
    {
        this.signature = signature;
    }


}
