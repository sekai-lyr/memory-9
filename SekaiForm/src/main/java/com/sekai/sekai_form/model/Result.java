package com.sekai.sekai_form.model;

public class Result<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) { Result<T> r = new Result<>(); r.success = true; r.data = data; return r; }
    public static <T> Result<T> ok(String msg, T data) { Result<T> r = new Result<>(); r.success = true; r.message = msg; r.data = data; return r; }
    public static <T> Result<T> fail(String msg) { Result<T> r = new Result<>(); r.success = false; r.message = msg; return r; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}