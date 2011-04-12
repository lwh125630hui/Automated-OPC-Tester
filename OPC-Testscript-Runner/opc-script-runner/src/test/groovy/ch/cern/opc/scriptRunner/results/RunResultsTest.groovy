package ch.cern.opc.scriptRunner.results

import java.beans.PropertyChangeListener

import ch.cern.opc.scriptRunner.results.RunResults

import static org.junit.Assert.*
import org.apache.commons.lang.NotImplementedException
import org.junit.Test
import org.junit.Before

import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import groovy.mock.interceptor.StubFor
import ch.cern.opc.client.ClientApi
import ch.cern.opc.client.ClientInstance
import ch.cern.opc.scriptRunner.results.async.AsyncConditionManager
import ch.cern.opc.scriptRunner.AsyncUpdateHandler
import ch.cern.opc.scriptRunner.results.async.AssertAsyncEqualsRunResult
import ch.cern.opc.scriptRunner.results.async.AssertAsyncNotEqualsRunResult

import static ch.cern.opc.scriptRunner.results.RunResults.NULL_MSG
import static ch.cern.opc.scriptRunner.results.RunResults.EMPTY_MSG

class RunResultsTest 
{
	private def asyncManagerRegisteredConditionsCount = 0
	private def isAsyncManagerTicking = false
	
	private def isAsyncUpdateHandlerRegistered = false
	private def maxConditionTimeout = 10
	
	def testee
	
	@Before
	void setup()
	{
		
		def theClientInstance = [
				getLastError:{it->
					return 'last error from DLL'
				}
				] as ClientApi
		
		ClientInstance.metaClass.'static'.getInstance = {-> return theClientInstance}
		
		def stubAsyncManager = new StubFor(AsyncConditionManager)
		stubAsyncManager.demand.getRegisteredAsyncConditionsCount(100) 
		{
			println('note - decrementing registered conditions count...')
			return asyncManagerRegisteredConditionsCount > 0? asyncManagerRegisteredConditionsCount-- :0 
		}
		stubAsyncManager.demand.startTicking(100) {isAsyncManagerTicking = true}
		stubAsyncManager.demand.stopTicking(100) {isAsyncManagerTicking = false}
		stubAsyncManager.demand.registerAsyncCondition(100){}
		stubAsyncManager.demand.getMaxConditionTimeout(100){return maxConditionTimeout}
		
		def stubAsyncUpdater = new StubFor(AsyncUpdateHandler)
		stubAsyncUpdater.demand.register(100) {isAsyncUpdateHandlerRegistered = true}
		
		/*
		 * Non-obvious code: The testee.asyncManager and testee.asyncUpdater
		 * fields we want to mock are final, thus cannot be set with object.field
		 * (final fields must be initialised by the ctor).
		 *
		 * Create the testee within the use{} block of the stubbed classes. The
		 * testee's ctor calls 'new AsyncConditionManager()' and 'new AsyncUpdateHandler'.
		 * Since this is within the scope of the stubs use{} blocks stubbed object
		 * instance are created.
		 *
		 * Sorry.
		 */
		stubAsyncManager.use 
		{
			stubAsyncUpdater.use 
			{ 
				testee = new RunResults()
			}
		}
		
		testee.pingPeriod = 1//s
	}	
	
	@Test
	void testAddAssertTrue()
	{
		testee.assertTrue('should pass', true)
		testee.assertTrue('should fail', false)
		
		testee.addException(new Exception('deliberate exception'))
		
		testee.assertFalse('should pass', false)
		testee.assertFalse('should fail', true)
		
		testee.assertEquals('should pass', 1.0, 1.0)
		testee.assertEquals('should fail', 1.0, 2.0)
		
		assertEquals(7, testee.XML.getChildNodes().length)
	}
	
	@Test
	void testOnScriptEnd_NoRegisteredAsyncConditions()
	{
		asyncManagerRegisteredConditionsCount = 0
		isAsyncManagerTicking = true
		
		testee.onScriptEnd()
		
		assertFalse(isAsyncManagerTicking)
	}
	
	@Test
	void testOnScriptEnd_StopsAsyncTicker()
	{
		asyncManagerRegisteredConditionsCount = 0
		isAsyncManagerTicking = true

		testee.onScriptEnd()
		assertFalse(isAsyncManagerTicking)
	}
	
	@Test
	void testOnScriptEnd_WaitsForRegisteredAsyncConditionsToBeRemoved()
	{
		asyncManagerRegisteredConditionsCount = 1
		isAsyncManagerTicking = true
		
		testee.onScriptEnd()
		
		assertEquals("ticker should wait for active condition count to reach 0", 0, asyncManagerRegisteredConditionsCount)
	}

	
	@Test
	void testOnScriptEnd_WaitsForMinimumAmountOfTimeWithLongConditionTimeout()
	{
		testee.pingPeriod = 1
		maxConditionTimeout = 100
		
		asyncManagerRegisteredConditionsCount = 1
		
		def before = new Date().getTime()
		testee.onScriptEnd()
		def after = new Date().getTime()
		
		assertTrue(after-before >= testee.pingPeriod*1000)
		assertTrue(after-before < maxConditionTimeout*1000)
	}
	
	@Test
	void testOnScriptEnd_WaitsForMinimumAmountOfTimeWithShortConditionTimeout()
	{
		testee.pingPeriod = 100
		maxConditionTimeout = 1
		
		asyncManagerRegisteredConditionsCount = 1
		
		def before = new Date().getTime()
		testee.onScriptEnd()
		def after = new Date().getTime()
		
		assertTrue(after-before >= maxConditionTimeout*1000)
		assertTrue(after-before < testee.pingPeriod*1000)
	}

	
	@Test
	void testOnScriptStart_RegistersAsyncUpdaterAndStartsTicker()
	{
		assertFalse(isAsyncUpdateHandlerRegistered)
		assertFalse(isAsyncManagerTicking)
		
		testee.onScriptStart()
		
		assertTrue(isAsyncUpdateHandlerRegistered)
		assertTrue(isAsyncManagerTicking)
	}
	
	@Test
	void testAssertAsyncEquals_RegistersAsyncAssertObjectWithAsyncManager()
	{
		def isAsyncAssertRunResultRegisteredWithAsyncManager = false
		
		def stubAsyncAssertEquals = new StubFor(AssertAsyncEqualsRunResult)
		stubAsyncAssertEquals.demand.registerWithManager {isAsyncAssertRunResultRegisteredWithAsyncManager = true}
		
		stubAsyncAssertEquals.use 
		{
			testee.assertAsyncEquals(null, null, null, null)	
		}
		
		assertTrue(isAsyncAssertRunResultRegisteredWithAsyncManager)
	}
	
	@Test
	void testAssertAsyncEquals_AddsAsyncAssertObjectToResults()
	{
		assertEquals(0, testee.results.size)
		
		testee.assertAsyncEquals(null, null, null, null)
		
		assertEquals(1, testee.results.size)
	}
	
	@Test
	void testAssertAsyncNotEquals_RegistersAsyncAssertObjectWithManager()
	{
		def isAsyncAssertNotEqualsRegisteredWithAsyncManager = false
		
		def stubAsyncAssertNotEquals = new StubFor(AssertAsyncNotEqualsRunResult)
		stubAsyncAssertNotEquals.demand.registerWithManager {isAsyncAssertNotEqualsRegisteredWithAsyncManager = true}
		
		stubAsyncAssertNotEquals.use 
		{
			testee.assertAsyncNotEquals(null, null, null, null)	
		}
		
		assertTrue(isAsyncAssertNotEqualsRegisteredWithAsyncManager)
	}
	
	@Test
	void testAssertAsyncNotEquals_AddsAsyncAssertObjectToResults()
	{
		assertEquals(0, testee.results.size)
		
		testee.assertAsyncNotEquals(null, null, null, null)
		
		assertEquals(1, testee.results.size)
	}
	
	@Test
	void testAddingAssertTrueUpdatesObserver()
	{
		def updateInfo = null
		
		def observer = {Object[] args -> updateInfo = args[1]} as Observer
		testee.addObserver(observer)
		
		testee.assertTrue(null, null)
		
		assertTrue(updateInfo instanceof RunResult)
	}
	
	@Test
	void testAddingRunResultUpdatesObserver()
	{
		def updateInfo = null
		
		def observer = {Object[] args ->
			updateInfo = args[1]
		} as Observer
		
		testee.addObserver(observer)

		def newRunResult = {throw new NotImplementedException('do not call impl')} as RunResult
		testee.add(newRunResult)
		
		assertSame(newRunResult, updateInfo)
	}
}
