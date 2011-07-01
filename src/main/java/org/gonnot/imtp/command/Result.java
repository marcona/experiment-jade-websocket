package org.gonnot.imtp.command;
/**
 *
 */
public class Result {
    private Throwable failure;
    private String result;


    public Result(Throwable failure, String result) {
        this.failure = failure;
        this.result = result;
    }


    public boolean hasFailed() {
        return failure != null;
    }


    public Throwable getFailure() {
        return failure;
    }


    public String getResult() {
        return result;
    }
}
