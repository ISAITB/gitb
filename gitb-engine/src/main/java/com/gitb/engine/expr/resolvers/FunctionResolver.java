package com.gitb.engine.expr.resolvers;

import com.gitb.ModuleManager;
import com.gitb.core.ErrorCode;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.IFunctionRegistry;
import com.gitb.utils.ErrorUtils;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * Created by senan on 9/8/14.
 */
public class FunctionResolver implements XPathFunctionResolver, XPathFunction {

    private TestCaseScope scope;
    private String functionName;

    public FunctionResolver(TestCaseScope scope) {
        this.scope = scope;
    }

    @Override
    public XPathFunction resolveFunction(QName functionName, int arity) {
        this.functionName = functionName.getLocalPart();
        return this;
    }

    @Override
    public Object evaluate(List args) throws XPathFunctionException {

	    Collection<IFunctionRegistry> implementations = ModuleManager.getInstance().getFunctionRegistries();

        for(IFunctionRegistry registry : implementations) {
            if(registry.isFunctionAvailable(this.functionName)){
                try {
                    return registry.callFunction(this.functionName, args.toArray());
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE), e);
                }
            }
        }

        return null;
    }
}
