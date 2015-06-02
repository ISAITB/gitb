package com.gitb.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An interface to define a function registry whose functions will be available
 * to be called within TestCase processors.
 */
public interface IFunctionRegistry {
    /**
     * Returns the unique name of the function registry.
     * @return
     */
    public String getName();

    /**
     * Checks if the function with given name is available
     * @param functionName function to be queried
     * @return
     */
    public boolean isFunctionAvailable(String functionName);

    /**
     * Calls the function with given name and arguments
     * @param functionName function to be executed
     * @param args
     * @return
     */
    public Object callFunction(String functionName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;
}
