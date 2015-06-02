package com.gitb.engine.expr;

import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by senan on 9/8/14.
 */
public class NamespaceContext implements javax.xml.namespace.NamespaceContext{

    private static Logger logger = LoggerFactory.getLogger(NamespaceContext.class);

    //<prefix, namespaceURI>
    private Map<String, String> namespaceURIs;

    //<namespaceURI, prefix>
    private Map<String, String> prefixes;

    public NamespaceContext(TestCaseScope scope){
        namespaceURIs = new HashMap<>();
        prefixes = new HashMap<>();

        if(scope.getContext().getTestCase().getNamespaces() != null) {
            for(Namespace namespace : scope.getContext().getTestCase().getNamespaces().getNs()){
                namespaceURIs.put(namespace.getPrefix(), namespace.getValue());
                prefixes.put(namespace.getValue(), namespace.getPrefix());
            }
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        String namespaceURI = namespaceURIs.get(prefix);
        logger.debug("NamespaceURI returned: " + namespaceURI);
        return namespaceURI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        String prefix = prefixes.get(namespaceURI);
        logger.debug("Prefix returned: " + prefix);
        return prefix;
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        List<String> result = new ArrayList<String>();
        if(namespaceURIs.containsValue(namespaceURI)) {
            Iterator prefixes = namespaceURIs.keySet().iterator();
            while(prefixes.hasNext()) {
                String key = (String) prefixes.next();
                if(namespaceURIs.get(key).equals(namespaceURI)){
                    result.add(key);
                }
            }
        }
        logger.debug("Prefixes returned: " + result.toString());
        return result.iterator();
    }
}
