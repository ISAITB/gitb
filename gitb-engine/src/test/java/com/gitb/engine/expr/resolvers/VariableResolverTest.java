package com.gitb.engine.expr.resolvers;

import com.gitb.engine.testcase.TestCaseContext;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.TestCase;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.types.StringType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VariableResolverTest {

	private TestCaseScope scope;
	private TestCaseContext context;

	@Before
	public void setUp() throws Exception {
		context = mock(TestCaseContext.class);
		scope = new TestCaseScope(context);

		MapType actorMap = (MapType) DataTypeFactory.getInstance().create(DataType.MAP_DATA_TYPE);
		MapType smpMessagingMap = (MapType) DataTypeFactory.getInstance().create(DataType.MAP_DATA_TYPE);
		StringType networkHost = (StringType) DataTypeFactory.getInstance().create(DataType.STRING_DATA_TYPE);
		networkHost.setValue("192.168.1.42");

		smpMessagingMap.addItem("network.host", networkHost);
		actorMap.addItem("SMPMessaging", smpMessagingMap);

		TestCaseScope.ScopedVariable var = scope.createVariable("actor1");
		var.setValue(actorMap);
	}

	@Test
	public void testResolveVariable() throws Exception {
		VariableResolver resolver = new VariableResolver(scope);

		DataType host = resolver.resolveVariable("$actor1{SMPMessaging}{network.host}");

		assertTrue(host instanceof StringType);
		assertEquals(host.getValue(), "192.168.1.42");
	}
}