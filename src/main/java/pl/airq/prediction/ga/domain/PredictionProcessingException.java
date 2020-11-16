package pl.airq.prediction.ga.domain;

import pl.airq.common.exception.ProcessingException;
import pl.airq.common.exception.ResourceNotFoundException;

public class PredictionProcessingException extends ProcessingException {

    public static final String MISSING_RESOURCE_TEMPLATE = "Unable to process prediction. %s resource is missing.";
    public static final String DEFAULT_MESSAGE = "Error occur during prediction processing.";

    public PredictionProcessingException() {
        super(DEFAULT_MESSAGE);
    }

    public PredictionProcessingException(String message) {
        super(message);
    }

    public PredictionProcessingException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    public PredictionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public PredictionProcessingException(ResourceNotFoundException cause) {
        super(String.format(MISSING_RESOURCE_TEMPLATE, cause.missingResourceClass.getSimpleName()), cause);
    }
}
