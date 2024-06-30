package ru.nikidzawa.datingapp.api.internal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {
    @ExceptionHandler(Unauthorized.class)
    public ResponseEntity<ExceptionEntity> handleUnauthorizedException(Unauthorized ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ExceptionEntity.builder()
                        .code(HttpStatus.UNAUTHORIZED.value())
                        .message(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionEntity> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ExceptionEntity.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .message(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ExceptionEntity> handlePaymentException(PaymentException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ExceptionEntity.builder()
                        .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .message(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(MailException.class)
    public ResponseEntity<ExceptionEntity> handlePaymentException(MailException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ExceptionEntity.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(ex.getMessage())
                        .build());
    }
    @ExceptionHandler(OtherException.class)
    public ResponseEntity<ExceptionEntity> handleOtherException(OtherException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ExceptionEntity.builder()
                        .code(HttpStatus.CONFLICT.value())
                        .message(ex.getMessage())
                        .build());
    }
}