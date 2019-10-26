package com.github.liuhuagui.smalldoc.util;

/**
 * @author KaiKang 799600902@qq.com
 */
public class Assert {
    /**
     * 如果expected不为true，抛出异常
     *
     * @param expected
     * @param message
     * @param args
     * @see {@link String#format(String, Object...)}
     */
    public static void check(boolean expected, String message, Object... args) {
        if (!expected)
            throw new AssertException(String.format(message, args));
    }

    /**
     * 如果target为null，抛出异常
     *
     * @param target
     * @param message
     * @param args
     * @see {@link String#format(String, Object...)}
     */
    public static void notNull(Object target, String message, Object... args) {
        if (target == null)
            throw new AssertException(String.format(message, args));
    }

    public static class AssertException extends RuntimeException {
        public AssertException(String message) {
            super(message);
        }
    }
}
