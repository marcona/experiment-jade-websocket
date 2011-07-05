package org.gonnot.imtp.command;
/**
 *
 */
public class Result {
    private Throwable failure;
    private Object result;


    private Result(Throwable failure, Object result) {
        this.failure = failure;
        this.result = result;
    }


    public boolean hasFailed() {
        return failure != null;
    }


    public Throwable getFailure() {
        return failure;
    }


    public Object getResult() {
        return result;
    }


    public static Result value(Object result) {
        return new Result(null, result);
    }


    public static Result failure(Throwable error) {
        return new Result(error, null);
    }
}
