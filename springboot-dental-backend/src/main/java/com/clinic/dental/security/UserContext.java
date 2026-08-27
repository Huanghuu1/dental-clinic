package com.clinic.dental.security;

/**
 * 请求级用户上下文。AuthInterceptor 在请求开始写入，在 afterCompletion 清理。
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    public static void remove() {
        CURRENT_USER.remove();
    }
}
