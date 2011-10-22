package org.gonnot.imtp.command;
/**
 *
 */
public class Result {
    private Throwable failure;
    private Object result;
    private int commandId;


    private Result(Throwable failure, Object result, int commandId) {
        this.failure = failure;
        this.result = result;
        this.commandId = commandId;
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


    public int getCommandId() {
        return commandId;
    }


    public static Result value(Object result) {
        // TODO a virer ?
        return new Result(null, result, 0);
    }


    public static Result value(Object result, Command command) {
        // todo
        return value(result, command.getCommandId());
    }


    public static Result value(Object result, int commandId) {
        // todo
        return new Result(null, result, commandId);
    }


    public static Result failure(Throwable error) {
        // TODO a virer ?
        return new Result(error, null, 0);
    }


    public static Result failure(Throwable error, Command command) {
        // TODO what if "command is null"
        return new Result(error, null, command.getCommandId());
    }
}
