package com.agentforge.platform.exception;

import com.agentforge.platform.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 业务异常。
   */
  @ExceptionHandler(BusinessException.class)
  public Result<Void> handleBusinessException(BusinessException e) {
    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * @Valid 请求体参数校验失败。
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
    return Result.error(40000, message);
  }

  /**
   * 方法参数校验失败。
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
    return Result.error(40000, e.getMessage());
  }

  /**
   * 兜底异常。
   */
  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("未处理异常", e);
    return Result.error(50000, "系统内部错误");
  }
}