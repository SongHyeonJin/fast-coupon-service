package com.example.fastcoupon.exception;

import com.example.fastcoupon.dto.common.BasicResponseDto;
import com.example.fastcoupon.dto.common.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<BasicResponseDto> signValidException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();

        List<String> errors = bindingResult.getFieldErrors().stream()
                .map(error -> String.format("[%s] %s", error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(BasicResponseDto.addBadRequest("유효성 검증에 실패했습니다.", errors));
    }

    @ExceptionHandler(ErrorException.class)
    public ResponseEntity<ErrorResponseDto> handleErrorException(ErrorException e) {
        log.warn("[{} 예외]: {}", e.getExceptionEnum().getMsg());

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                e.getExceptionEnum().getStatus(), e.getExceptionEnum().getType(), e.getExceptionEnum().getMsg()
        );
        return ResponseEntity.status(e.getExceptionEnum().getStatus()).body(errorResponseDto);
    }


}
