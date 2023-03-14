package com.gitb.engine.testcase;

import com.gitb.types.DataType;
import com.gitb.types.MapType;

public class StepStatusMapType extends MapType {

    public DataType getScopedItem(String key, TestCaseScope currentScope) {
        var scopeId = currentScope.getQualifiedScopeId();
        if (scopeId != null && !scopeId.isEmpty()) {
            // Prefix with the current scope ID to resolve relative IDs within scriptlets.
            return getItem(currentScope.getQualifiedScopeId()+"_"+key);
        } else {
            return getItem(key);
        }
    }

}
