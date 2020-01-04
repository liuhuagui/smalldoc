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
     * @see String#format(String, Object...)
     */
    public static void check(boolean expected, String message, Object... args) {
        if (!expected)
            throw new AssertException(message, args);
    }

    /**
     * 如果notExpected为true，抛出异常
     *
     * @param notExpected
     * @param message
     * @param args
     * @see String#format(String, Object...)
     */
    public static void checkNot(boolean notExpected, String message, Object... args) {
        if (notExpected)
            throw new AssertException(message, args);
    }

    /**
     * 如果target为null，抛出异常
     *
     * @param target
     * @param message
     * @param args
     * @see String#format(String, Object...)
     */
    public static void notNull(Object target, String message, Object... args) {
        if (target == null)
            throw new AssertException(message, args);
    }


    public static class AssertException extends RuntimeException {
        /**
         * @param message
         * @param args
         * @see String#format(String, Object...)
         */
        public AssertException(String message, Object... args) {
            super(args.length == 0 ? message : String.format(message, args));
        }
    }
}
