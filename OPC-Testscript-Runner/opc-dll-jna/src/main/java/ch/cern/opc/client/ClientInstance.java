package ch.cern.opc.client;

import static ch.cern.opc.common.Log.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A singleton that proxies the {@link ch.cern.opc.client.Client Client} class
 * 
 * @author bfarnham
 *
 */
public class ClientInstance implements ClientApi
{
	private static ClientInstance theInstance = null;
	
	private List<String> opcAddressSpace = new ArrayList<String>();

	/**
	 * The instance of the dll as seen by jna
	 */
	private Client client;
	
	public static ClientApi getInstance()
	{
		if(theInstance == null)
		{
			logInfo("Creating the one and only client instance");
			theInstance = new ClientInstance();
		}
		
		return theInstance;
	}
	
	private ClientInstance()
	{
		client = new Client();		
	}

	@Override
	public boolean init(String host, String server)
	{
		logInfo("Initialising instance for host ["+host+"] server ["+server+"]");
		boolean initialised = client.init(host, server);

		opcAddressSpace.clear();
		opcAddressSpace.addAll(client.getItemNames());
		logInfo("Retrieved opc server address space, ["+opcAddressSpace.size()+"] items");
		
		return initialised && !opcAddressSpace.isEmpty();
	}

	@Override
	public boolean destroy() 
	{
		boolean result = client.destroy();
		theInstance = null;
		
		return result;
	}

	@Override
	public State getState() 
	{
		return client.getState();
	}

	@Override
	public boolean createGroup(String groupName, long refreshRateMs) 
	{
		return client.createGroup(groupName, refreshRateMs);
	}

	@Override
	public boolean destroyGroup(String groupName) 
	{
		return client.destroyGroup(groupName);
	}

	@Override
	public List<String> getItemNames() 
	{
		return opcAddressSpace;
	}

	@Override
	public boolean addItem(String groupName, String itemPath) 
	{
		return client.addItem(groupName, itemPath);
	}

	@Override
	public String readItemSync(String groupName, String itemPath) 
	{
		return client.readItemSync(groupName, itemPath);
	}

	@Override
	public boolean writeItemSync(String groupName, String itemPath, String value) 
	{
		return client.writeItemSync(groupName, itemPath, value);
	}
	
	@Override
	public boolean writeItemAsync(String groupName, String itemPath, String value) 
	{
		return client.writeItemAsync(groupName, itemPath, value);
	}

	@Override
	public String getLastError() 
	{
		return client.getLastError();
	}

	@Override
	public void registerAsyncUpdate(AsyncUpdateCallback callback) 
	{
		client.registerAsyncUpdate(callback);
	}

	@Override
	public boolean readItemAsync(String groupName, String itemPath) 
	{
		return client.readItemAsync(groupName, itemPath);
	}

}
