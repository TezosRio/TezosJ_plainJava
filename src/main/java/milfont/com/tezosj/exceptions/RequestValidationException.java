package milfont.com.tezosj.exceptions;

public class RequestValidationException extends Exception {
    private static final long serialVersionUID = 2645168648809062561L;

    public RequestValidationException(String message) {
        super(message);
    }
}
