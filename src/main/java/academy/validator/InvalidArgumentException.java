package academy.validator;

// trigger exit code 2 for command-line exception
public class InvalidArgumentException extends RuntimeException {
    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
