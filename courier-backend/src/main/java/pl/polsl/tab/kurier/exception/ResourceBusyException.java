package pl.polsl.tab.kurier.exception;

public class ResourceBusyException extends RuntimeException {
    public ResourceBusyException(String message) {
        super(message);
    }
}
