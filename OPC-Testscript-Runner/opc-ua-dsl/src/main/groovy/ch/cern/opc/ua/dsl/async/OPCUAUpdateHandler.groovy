package ch.cern.opc.ua.dsl.async

import ch.cern.opc.common.ItemValue
import ch.cern.opc.dsl.common.client.UpdateHandler
import ch.cern.opc.ua.clientlib.notification.OPCUAAsyncUpdateCallback;
import ch.cern.opc.ua.clientlib.notification.SubscriptionNotification;
import ch.cern.opc.ua.clientlib.notification.SubscriptionNotificationHandler;

class OPCUAUpdateHandler implements OPCUAAsyncUpdateCallback
{
	final UpdateHandler dslHandler
	
	def OPCUAUpdateHandler(UpdateHandler genericHandler)
	{
		dslHandler = genericHandler
	}

	@Override
	public int onUpdate(String nodeId, String attributeId, SubscriptionNotification notification) 
	{
		if(nodeId == null) return 0
		if(attributeId == null) return 0
		if(notification == null) return 0
		if(notification.value == null) return 0
		
		def valueAsString = notification.value.value.value.toString()
		ItemValue itemValue = new ItemValue(valueAsString, 0, "no timestamp", 0)
		dslHandler.onUpdate(nodeId, attributeId, itemValue)
		return 1;
	}
}
