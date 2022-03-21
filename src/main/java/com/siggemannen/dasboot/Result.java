package com.siggemannen.dasboot;

/**
 * Result of bulk storage
 */
public class Result
{
    private final boolean ok;
    private final Exception exception;

    Result()
    {
        ok = true;
        exception = null;
    }

    Result(Exception e)
    {
        ok = false;
        this.exception = e;
    }

    public boolean isOk()
    {
        return ok;
    }

    public Exception getException()
    {
        return exception;
    }
}
