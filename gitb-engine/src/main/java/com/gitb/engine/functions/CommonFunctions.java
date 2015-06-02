package com.gitb.engine.functions;

import com.gitb.repository.IFunctionRegistry;
import com.gitb.utils.TimeUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Provides common functions across all test modules
 */
@MetaInfServices(IFunctionRegistry.class)
public class CommonFunctions implements IFunctionRegistry{

    private static final String NAME = "Common Functions Registry";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isFunctionAvailable(String functionName) {
        //TODO method check by signature
        Method[] methods = getClass().getDeclaredMethods();
        for(Method method : methods) {
            if(method.getName().equals(functionName))
                return true;
        }

        return false;
    }

    @Override
    public Object callFunction(String functionName, Object[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return MethodUtils.invokeMethod(this, functionName, args);
    }

    //***********************
    //* Method Declarations *
    //***********************

    /**
     * Concatenates two objects by converting them into strings
     * @param arg1
     * @param arg2
     * @return Concatenation of two objects
     */
    public String concat(Object arg1, Object arg2) {
        String string1 = null;
        if(arg1 instanceof  String){
            string1 = (String) arg1;
        }
        else if(arg1 instanceof Node){
            Node node = (Node) arg1;
            string1 = node.getTextContent();
        } else{
            string1 = arg1.toString();
        }

        String string2 = null;
        if(arg1 instanceof String){
            string2 = (String) arg2;
        }
        else if(arg2 instanceof Node){
            Node node = (Node) arg2;
            string2 = node.getTextContent();
        } else{
            string2 = arg2.toString();
        }

        return string1 + string2;
    }

    /**
     * Replaces each substring of given string that matches the literal target
     * sequence with the specified literal replacement sequence.
     * @param arg1 Target string that will be replaced
     * @param arg2 Pattern inside the target string to be replaced
     * @param arg3 Replacement string
     * @return Resulting string after replacement
     */
    public String replace(Object arg1, Object arg2, Object arg3) {
        String string1 = (String) arg1;
        String string2 = (String) arg2;
        String string3 = (String) arg3;

        return string1.replace(string2, string3);
    }

    /**
     * Calculates the length of given object
     * @param arg
     * @return Element count of given object
     */
    public int sizeOf(Object arg){
        if(arg instanceof List){
            return ((List) arg).size();
        } else if(arg instanceof NodeList){
            return ((NodeList) arg).getLength();
        } else if(arg instanceof String){
            return ((String) arg).length();
        } else{
            //TODO throw exception
            return -1;
        }
    }

    public Number add(Number arg1, Number arg2){
        return arg1.doubleValue() + arg2.doubleValue();
    }

    public Number subtract(Number arg1, Number arg2){
        return arg1.doubleValue() - arg2.doubleValue();
    }

    public Number multiply(Number arg1, Number arg2){
        return arg1.doubleValue() * arg2.doubleValue();
    }

    public Number divide(Number arg1, Number arg2){
        return arg1.doubleValue() / arg2.doubleValue();
    }

    public boolean isEqual(Number arg1, Number arg2){
        return arg1.doubleValue() == arg2.doubleValue();
    }

    public boolean isGreater(Number arg1, Number arg2){
        return arg1.doubleValue() > arg2.doubleValue();
    }

    public boolean isGreaterOrEqual(Number arg1, Number arg2){
        return arg1.doubleValue() >= arg2.doubleValue();
    }

    public boolean isLess(Number arg1, Number arg2){
        return arg1.doubleValue() < arg2.doubleValue();
    }

    public boolean isLessOrEqual(Number arg1, Number arg2){
        return arg1.doubleValue() <= arg2.doubleValue();
    }

    public boolean and(Object arg1, Object arg2){
        boolean b1 = false;
        if(arg1 instanceof String) {
            b1 = Boolean.parseBoolean((String) arg1);
        } else{
            b1 = (boolean) arg1;
        }

        boolean b2 = false;
        if(arg2 instanceof String) {
            b2 = Boolean.parseBoolean((String) arg2);
        } else{
            b2 = (boolean) arg2;
        }

        return b1 && b2;
    }

    public boolean or(Object arg1, Object arg2){
        boolean b1 = false;
        if(arg1 instanceof String) {
            b1 = Boolean.parseBoolean((String) arg1);
        } else{
            b1 = (boolean) arg1;
        }

        boolean b2 = false;
        if(arg2 instanceof String) {
            b2 = Boolean.parseBoolean((String) arg2);
        } else{
            b2 = (boolean) arg2;
        }

        return b1 || b2;
    }


    /**
     * Generates a UUID
     */
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    public long generateLong() {
        return Math.abs(new Random().nextLong());
    }

    public int generateInteger(double arg1, double arg2){
        int minimum = (int) arg1;
        int maximum = (int) arg2;
        return (Math.abs(new Random().nextInt())) % (maximum + 1 - minimum) + minimum;
    }

    public float generateFloat(double arg1, double arg2, double arg3){
        float minimum = (float) arg1;
        float maximum = (float) arg2;
        int precision = (int) arg3;

        float f = (Math.abs(new Random().nextFloat()) % (maximum + 1 - minimum) + minimum);

        String format = "#." + new String(new char[precision]).replace('\0', '#');
        DecimalFormat formatter = new DecimalFormat(format);

        return Float.parseFloat(formatter.format(f));
    }

    public String currentDateTime() {
        return TimeUtils.serializeUTC(TimeUtils.getCurrentDateTime());
    }

}
