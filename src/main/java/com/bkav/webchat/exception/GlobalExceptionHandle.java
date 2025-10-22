//package com.bkav.webchat.exception;
//
//import com.google.api.pathtemplate.ValidationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.example.custom.Constants.Status.STATUS_ERROR;
//import static com.example.custom.Constants.Status.STATUS_PARAM_INVALID;
//@ControllerAdvice
//public class GlobalExceptionHandle {
//    @ExceptionHandler(value = RuntimeException.class)
//    public ResponseEntity<Object> handlingRuntimeException(RuntimeException exception){
//        HttpStatus errorStatus = HttpStatus.BAD_REQUEST;
//        ApiErrorResponse<?> apiError = ResponseHelper.getApiErrorResponse(STATUS_ERROR, exception);
//
//        return ResponseHelper.createResponseEntity(errorStatus, apiError);
//    }
//
//    @ExceptionHandler(ValidationException.class)
//    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
//        HttpStatus errorStatus = HttpStatus.BAD_REQUEST;
//
//        ApiErrorResponse<?> apiError = ResponseHelper.getInvalidParamResponse(STATUS_PARAM_INVALID, ex);
//
//        return ResponseHelper.createResponseEntity(errorStatus, apiError);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Object> handleValidationArgumentNotValidException(MethodArgumentNotValidException ex) {
//        HttpStatus errorStatus = HttpStatus.BAD_REQUEST;
//
//        ApiErrorResponse<?> apiError = getBindExceptionApiError(ex);
//        return ResponseHelper.createResponseEntity(errorStatus, apiError);
//    }
//
//    private ApiErrorResponse<?> getBindExceptionApiError(BindException ex) {
//
//        List<String> errors = new ArrayList<>();
//        ex.getBindingResult().getFieldErrors().forEach(error -> errors.add(error.getField() + ": " + error.getDefaultMessage()));
//        ex.getBindingResult().getGlobalErrors().forEach(error -> errors.add(error.getObjectName() + ": " + error.getDefaultMessage()));
//
//        return ResponseHelper.getInvalidParamResponse(STATUS_PARAM_INVALID, ex, errors);
//    }
//}
//
//
