package pl.polsl.tab.kurier.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ResourceBusyException extends RuntimeException {
    public ResourceBusyException(String message) {
        super(message);
    }
}
