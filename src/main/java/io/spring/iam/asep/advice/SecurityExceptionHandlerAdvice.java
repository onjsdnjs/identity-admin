package io.spring.iam.asep.advice;

import io.spring.iam.asep.ErrorCode;
import io.spring.iam.asep.ErrorResponse;
import io.spring.iam.asep.annotation.SecurityControllerAdvice;
import io.spring.iam.asep.annotation.SecurityExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@SecurityControllerAdvice
@Configuration
public class SecurityExceptionHandlerAdvice {

    @SecurityExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(RuntimeException e, HttpServletRequest request) {

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                HttpServletResponse.SC_UNAUTHORIZED,
                ErrorCode.AUTH_FAILED.code(),
                ErrorCode.AUTH_FAILED.message(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(body);
    }
}
