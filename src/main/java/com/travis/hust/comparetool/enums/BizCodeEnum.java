package com.travis.hust.comparetool.enums;

import lombok.Getter;

/**
 * @ClassName BizCodeEnum
 * @Description 业务状态枚举类
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/5/6
 */
@Getter
public enum BizCodeEnum {

    /**
     * 处理成功
     */
    SUCCESS(200, "请求处理成功"),
    /**
     * 客户端错误
     */
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "账号未登陆"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求未找到"),
    METHOD_NOT_ALLOWED(405, "请求方法不正确"),
    LOCKED(423, "请求失败, 请稍后重试"), // 不允许并发请求，阻塞中
    TOO_MANY_REQUESTS(429, "请求过于频繁, 请稍后重试"),
    /**
     * 服务端错误
     */
    INTERNAL_SERVER_ERROR(500, "系统异常"),
    /**
     * 自定义服务错误
     */
    TOKEN_CHECK_FAILED(901, "token 验证失败"),
    TOKEN_EXPIRED(902, "token 已过期"),
    TOKEN_REFRESH(903, "token 可以刷新"),
    TOKEN_MISSION(904, "token 缺失"),
    UNKNOW(999, "未知错误");


    private int code;
    private String message;

    BizCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
