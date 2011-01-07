package ch.cern.opc.scriptRunner

import ch.cern.opc.client.ClientApi
import ch.cern.opc.client.ClientInstance

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

class ScriptContextTest 
{
	static final def TEST_GROUP_NAME = 'test.group'
	static final def TEST_LAST_ERR = 'this is the last error'
	def testee
	
	def initialisationParameters
	
	@Before
	void setup()
	{
		initialisationParameters = null
		
		def theClientInstance = [
			createGroup: {groupName, refreshRate -> 
				println "created group name [${groupName}] refresh rate [${refreshRate}]"
				return true
			},
			destroyGroup: {groupName ->
				println "destroyed group name [${groupName}]"
				return true
			},
			init: {host, server ->
				initialisationParameters = ['host': host, 'server':server]
				return true
			},
			getLastError:{it->
				return TEST_LAST_ERR
			}
		] as ClientApi
	
		ClientInstance.metaClass.'static'.getInstance = {-> return theClientInstance}
		
		testee = new ScriptContext()
	}
	
	@Test
	void testDefaultConstructorCreatesEmptyGroupsMap()
	{
		assertEquals(0, new ScriptContext().groups.size())
	}
	
	@Test
	void testFindGroupReturnsNullForInvalidGroupName()
	{
		assertNull(testee.findGroup('I do not exist'))
	}
	
	@Test
	void testCreateGroupAndFindGroup()
	{
		testee.createGroup(TEST_GROUP_NAME)
		assertEquals(TEST_GROUP_NAME, testee.findGroup(TEST_GROUP_NAME).name)
	}
	
	@Test
	void testCreateGroupReturnsNewGroup()
	{
		def newGroup = testee.createGroup(TEST_GROUP_NAME)
		assertEquals(TEST_GROUP_NAME, newGroup.name)	
	}
	
	@Test
	void testGetGroupAddsGroupIfGroupNotAlreadyAdded()
	{
		assertNull(testee.findGroup(TEST_GROUP_NAME))
		
		testee.group(TEST_GROUP_NAME)
		
		assertNotNull(testee.findGroup(TEST_GROUP_NAME))
	}
	
	@Test
	void testGetGroupDoesNotAddGroupIfGroupAlreadyAdded()
	{
		def theOriginalGroup = testee.group(TEST_GROUP_NAME)
		
		def theNewGroup = testee.group(TEST_GROUP_NAME)
		
		assertSame(theOriginalGroup, theNewGroup)
	}
	
	@Test
	void testInitCallsInitialiseWithCorrectHostAndServer()
	{
		final def host = 'test.host.machine'
		final def server = 'test.opc.server'
		
		testee.init(host, server)
		
		assertEquals(host, initialisationParameters['host'])
		assertEquals(server, initialisationParameters['server'])
	}
	
	@Test
	void testScriptContextSupportsAssertions()
	{
		// note: this is not an exhaustive test of assertions, just to check that assertions
		// are available to the ScriptContext class.
		try
		{
			testee.assertTrue("testing a test", true)
		}
		catch(MissingMethodException e)
		{
			fail('assertTrue should be available from ScriptContext')
		} 
	}
	
	@Test
	void testGetLastErrorReturnsLastError()
	{
		assertEquals(TEST_LAST_ERR, testee.lastError)
	}
	
	@Test
	void testInstanceFieldHoldsLastInstance()
	{
		assertEquals(testee, ScriptContext.instance)
		
		def newInstance = new ScriptContext()
		assertEquals(newInstance, ScriptContext.instance)
	}
	
	@Test
	void testDestroyGroupRemovesItFromGroupsCollection()
	{
		testee.group(TEST_GROUP_NAME)
		assertEquals(1, testee.groups.size())
		
		testee.group(TEST_GROUP_NAME).destroy()
		assertEquals(0, testee.groups.size())
	}
	
	@Test
	void testDestroyGroupHandlesUndefinedGroups()
	{
		testee.group(TEST_GROUP_NAME)
		assertEquals(1, testee.groups.size())
		
		testee.destroyGroup('undefined group')
		assertEquals(1, testee.groups.size())
	}
}
