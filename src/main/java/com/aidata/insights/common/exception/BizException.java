package com.aidata.insights.common.exception;

import com.aidata.insights.common.api.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * 业务异常：由业务代码主动抛出，由全局异常处理器统一转换为标准响应
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final HttpStatus httpStatus;

    public BizException(String message) {
        this(ResultCode.BAD_REQUEST.getCode(), message, ResultCode.BAD_REQUEST.getHttpStatus());
    }

    public BizException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage(), resultCode.getHttpStatus());
    }

    public BizException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message, resultCode.getHttpStatus());
    }

    public BizException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
