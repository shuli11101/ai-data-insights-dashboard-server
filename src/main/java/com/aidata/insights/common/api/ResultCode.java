package com.aidata.insights.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一业务状态码
 *
 * <p>约定：body 中的 code 与 HTTP 状态码保持一致，如 401 同时出现在状态行与响应体中，
 * 便于前端只判断 body 或只判断 HTTP 状态都能正确处理。</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功", HttpStatus.OK),

    BAD_REQUEST(400, "请求参数有误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "请先登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "没有访问权限", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "请求的资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    NOT_ACCEPTABLE(406, "无法生成客户端可接受的响应", HttpStatus.NOT_ACCEPTABLE),
    CONFLICT(409, "资源已存在", HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(415, "Content-Type 不支持", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),

    LOGIN_FAILED(401, "手机号或密码错误", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(401, "登录凭证无效", HttpStatus.UNAUTHORIZED),
    PHONE_EXISTS(409, "该手机号已注册", HttpStatus.CONFLICT),
    VERIFY_CODE_ERROR(400, "验证码错误", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404, "用户不存在", HttpStatus.NOT_FOUND);

    private final int code;

    private final String message;

    private final HttpStatus httpStatus;

    ResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
