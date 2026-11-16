package thread.exception;

public class PaymentByIdException extends RuntimeException {

    public PaymentByIdException(String message) {
        super(message);
    }
}
