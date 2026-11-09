package com.aidata.insights.common.exception;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.common.api.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一转换为 {@link Result}，并让 HTTP 状态码与业务码保持一致
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return build(e.getHttpStatus(), Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return build(HttpStatus.BAD_REQUEST, Result.fail(ResultCode.BAD_REQUEST,
                message.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : message));
    }

    /**
     * 表单 / 查询参数校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return build(HttpStatus.BAD_REQUEST, Result.fail(ResultCode.BAD_REQUEST,
                message.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : message));
    }

    /**
     * 方法参数校验失败（@RequestParam / @PathVariable 上的约束注解）
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.getMessage());
        log.warn("参数校验失败: {}", message);
        return build(HttpStatus.BAD_REQUEST, Result.fail(ResultCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return build(HttpStatus.BAD_REQUEST, Result.fail(ResultCode.BAD_REQUEST, message));
    }

    /**
     * 请求体缺失或 JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误或缺失"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST,
                Result.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST,
                Result.fail(ResultCode.BAD_REQUEST, "参数类型不正确: " + e.getName()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                Result.fail(ResultCode.METHOD_NOT_ALLOWED, e.getMessage()));
    }

    /**
     * Content-Type 不支持：例如用 form-data / text/plain 调用 @RequestBody 接口
     *
     * <p>不单独处理会落到 Exception 兜底返回 500，接口调用方很难定位问题。</p>
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        String supported = e.getSupportedMediaTypes().stream()
                .map(MediaType::toString)
                .collect(Collectors.joining(", "));
        String message = supported.isEmpty()
                ? ResultCode.UNSUPPORTED_MEDIA_TYPE.getMessage() + "，请使用 application/json"
                : ResultCode.UNSUPPORTED_MEDIA_TYPE.getMessage() + "，请使用: " + supported;
        log.warn("Content-Type 不支持: {}, supported={}", e.getContentType(), supported);
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                Result.fail(ResultCode.UNSUPPORTED_MEDIA_TYPE, message));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        log.warn("Accept 无法满足: {}", e.getMessage());
        return build(HttpStatus.NOT_ACCEPTABLE, Result.fail(ResultCode.NOT_ACCEPTABLE));
    }

    /**
     * 缺少必需的请求头 / Cookie 等绑定参数
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Result<Void>> handleRequestBinding(ServletRequestBindingException e) {
        log.warn("请求参数绑定失败: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                Result.fail(ResultCode.BAD_REQUEST, e.getMessage()));
    }

    /**
     * 接口 / 静态资源不存在
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return build(HttpStatus.NOT_FOUND, Result.fail(ResultCode.NOT_FOUND));
    }

    /**
     * 认证失败（兜底，正常情况下由 AuthenticationEntryPoint 处理）
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, Result.fail(ResultCode.UNAUTHORIZED));
    }

    /**
     * 已登录但权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return build(HttpStatus.FORBIDDEN, Result.fail(ResultCode.FORBIDDEN));
    }

    /**
     * 未预期异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, Result.fail(ResultCode.INTERNAL_ERROR));
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, Result<Void> body) {
        return ResponseEntity.status(status).body(body);
    }
}
