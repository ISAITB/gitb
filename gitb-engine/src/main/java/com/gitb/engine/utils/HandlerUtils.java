/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.engine.utils;

import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.DataType;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import com.gitb.utils.NamespaceContext;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.Map;

public class HandlerUtils {

    public static final String NAMESPACE_MAP_INPUT = "_com.gitb.Namespaces";
    public static final String SESSION_INPUT = "_com.gitb.Session";

    public static XPathExpression compileXPathExpression(MapType namespaces, StringType expression, VariableResolver variableResolver) {
        // Compile xpath expression
        XPath xPath = new net.sf.saxon.xpath.XPathFactoryImpl().newXPath();
        if (namespaces != null) {
            var nsMap = new HashMap<String, String>();
            for (var entry: ((Map<String, DataType>)namespaces.getValue()).entrySet()) {
                nsMap.put(entry.getKey(), (String) entry.getValue().getValue());
            }
            xPath.setNamespaceContext(new NamespaceContext(nsMap));
            xPath.setXPathVariableResolver(variableResolver);
        }
        try {
            return xPath.compile((String)expression.getValue());
        } catch (XPathExpressionException e) {
            throw new GITBEngineInternalError(e);
        }
    }

}
