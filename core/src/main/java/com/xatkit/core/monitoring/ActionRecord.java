package com.xatkit.core.monitoring;

import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.platform.action.RuntimeActionResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

class ActionRecord implements Record {

    private static final long serialVersionUID = 55L;

    private String actionName;

    private List<String> arguments;

    private long executionTime;

    private boolean isError;

    private Object result;

    private Exception thrownException;

    public ActionRecord(Class<? extends RuntimeAction> runtimeActionClass,
                        List<Object> arguments, RuntimeActionResult actionResult) {
        this.actionName = runtimeActionClass.getSimpleName();
        this.arguments = arguments.stream().map(arg -> {
            if(arg instanceof String) {
                return (String) arg;
            } else {
                return arg.toString() + "(" + arg.getClass().getName() + ")";
            }
        }).collect(Collectors.toList());
        this.executionTime = actionResult.getExecutionTime();
        this.isError = actionResult.isError();
        this.result = actionResult.getResult();
        this.thrownException = actionResult.getThrownException();
    }

    public String getActionName() {
        return this.actionName;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public long getExecutionTime() {
        return this.executionTime;
    }

    public boolean isError() {
        return this.isError;
    }

    public Object result() {
        return this.result;
    }

    public Exception getThrownException() {
        return this.thrownException;
    }

    @Override
    public int hashCode() {
        int hashCode = this.actionName.hashCode() + Long.hashCode(this.executionTime);
        if(this.isError) {
            if(nonNull(this.thrownException)) {
                hashCode += this.thrownException.hashCode();
            }
        } else {
            if(nonNull(result)) {
                hashCode += this.result.hashCode();
            }
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ActionRecord) {
            ActionRecord other = (ActionRecord) obj;
            return other.actionName.equals(this.actionName) && other.executionTime == this.executionTime
                    && other.isError == this.isError && Objects.equals(other.result, this.result)
                    && Objects.equals(other.thrownException, this.thrownException);
        }
        return super.equals(obj);
    }
}
