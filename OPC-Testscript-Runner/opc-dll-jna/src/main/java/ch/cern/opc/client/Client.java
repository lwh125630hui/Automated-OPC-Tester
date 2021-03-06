package ch.cern.opc.client;

import static ch.cern.opc.common.Log.logDebug;
import static ch.cern.opc.common.Log.logError;
import static ch.cern.opc.common.Log.logInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ch.cern.opc.common.Datatype;
import ch.cern.opc.common.ItemAccessRight;
import ch.cern.opc.common.ItemValue;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.ptr.IntByReference;

class Client implements OPCDAClientApi 
{
	private final static String DLL_NM = "AutomatedOpcTester.dll"; 
	private final static int MAX_BUFF_SZ = 1000;
	
	private DllInterface INSTANCE;
	
	public Client()
	{
		INSTANCE = (DllInterface)Native.loadLibrary(DLL_NM, DllInterface.class);
		logDebug("Created OPC DLL client, library ["+DLL_NM+"]");
	}
	
	@Override
	public boolean init(String host, String server) 
	{
		INSTANCE.init(host, server);
		logDebug("Initialised OPC DLL client library with host ["+host+"] server ["+server+"]");
		
		return true;
	}

	@Override
	public void end() 
	{
		logInfo("Client.end called");
		
		// disconnect client session
		INSTANCE.end();
		
		// remove mapping from dll to class
		Native.unregister(DllInterface.class);
		
		// drop DLL
		NativeLibrary.getProcess().dispose();		
		INSTANCE = null;
		
		// garbage collect the instance
		Runtime.getRuntime().gc();
		
		logInfo("Client.end completed");
	}

	@Override
	public State getState() 
	{
		return null;
	}

	@Override
	public boolean createGroup(String groupName, long refreshRateMs) 
	{
		NativeLong nativeRequestedRefreshRate = new NativeLong(refreshRateMs);
		NativeLong nativeActualRefreshRate = INSTANCE.createGroup(groupName, nativeRequestedRefreshRate);

		if(!nativeActualRefreshRate.equals(nativeRequestedRefreshRate))
		{
			logError("Requested refresh rate ["+refreshRateMs+"] was not met - returned refresh rate ["+nativeActualRefreshRate.longValue()+"]");
			return false;
		}
		return true;
	}
	
	@Override
	public boolean destroyGroup(String groupName) 
	{
		return INSTANCE.destroyGroup(groupName);
	}

	@Override
	public List<String> getItemNames() 
	{
    	final int nElementSz = 300;
    	final int nNumElements = 1000;
    	
    	List<String> result = new ArrayList<String>();
    	
    	boolean gotEmAll = false;
    	for(int offset = 0; !gotEmAll; offset += nNumElements)
    	{
    		String[] s = createBuffer(nElementSz, nNumElements);
    		gotEmAll = INSTANCE.getItemNames(s, nElementSz, nNumElements, offset);
    		System.out.println("gotEmAll ["+gotEmAll+"] offset ["+offset+"]");
    		
        	for(int i=0; i<nNumElements; i++)
        	{
        		if(!s[i].isEmpty())
        		{
        			System.out.println("item ["+i+"] value ["+s[i]+"]");
        			result.add(s[i]);
        		}
        	}
    	}
		
		return result;
	}

	@Override
	public boolean addItem(String groupName, String itemPath) 
	{
		return INSTANCE.addItem(groupName, itemPath);
	}

	@Override
	public ItemValue readItemSync(String groupName, String itemPath) 
	{
		byte[] valBuff = new byte[1024];
		byte[] tsBuff = new byte[1024];
		IntByReference quality = new IntByReference();
		IntByReference type = new IntByReference();
		
		if(INSTANCE.readItemSync(groupName, itemPath, 1024, ByteBuffer.wrap(valBuff), quality, type, ByteBuffer.wrap(tsBuff)))
		{
			return new ItemValue(translateCppString(valBuff), quality.getValue(), translateCppString(tsBuff), type.getValue());
		}
		else
		{
			logError("item value was not read, group ["+groupName+"] item path ["+itemPath+"]");
			return ItemValue.ERROR_VALUE;
		}
	}

	@Override
	public boolean writeItemSync(String groupName, String itemPath, String value) 
	{
		return INSTANCE.writeItemSync(groupName, itemPath, value);
	}
	
	@Override
	public boolean writeItemAsync(String groupName, String itemPath, String value) 
	{
		return INSTANCE.writeItemAsync(groupName, itemPath, value);
	}

	@Override
	public String getLastError() 
	{
		byte buff[] = new byte[MAX_BUFF_SZ];
		INSTANCE.getLastError(buff, MAX_BUFF_SZ);
		return translateCppString(buff);
	}
	
	private String translateCppString(byte cppBuff[])
	{
		final int stringTerminatorPosition = findCppStringTerminator(cppBuff);
		logDebug("using cpp string from 0 - " + stringTerminatorPosition);
		
		String cppString = new String(cppBuff);
		if(stringTerminatorPosition > 0)
		{
			return cppString.substring(0, stringTerminatorPosition);
		}
		else
		{
			return cppString;
		}
	}
	
	private int findCppStringTerminator(byte cppBuff[])
	{
		for(int i=0; i<cppBuff.length; i++)
		{
			if(cppBuff[i] == 0)
			{
				return i;
			}
		}
		return -1;
	}
	
	private String[] createBuffer(final int nElementSz, final int nNumElements) {
		String result[] = new String[nNumElements];    	
    	
		for(int i=0; i<nNumElements; i++)
    	{    		
    		result[i] = new String(new char[nElementSz]);
    	}
		return result;
	}

	@Override
	public void registerAsyncUpdate(OPCDAAsyncUpdateCallback callback) 
	{
		System.out.println("Registering OPCDA callback for updates from the dll...");
		INSTANCE.registerAsyncUpdate(callback);
		System.out.println("Registered OPCDA callback for updates from the dll");
	}

	@Override
	public boolean readItemAsync(String groupName, String itemPath) 
	{
		return INSTANCE.readItemAsync(groupName, itemPath);
	}

	@Override
	public ItemAccessRight getItemAccessRights(String groupName, String itemPath) 
	{
		return ItemAccessRight.fromId(INSTANCE.getItemAccessRights(groupName, itemPath));
	}

	@Override
	public Datatype getItemDatatype(String groupName, String itemPath) 
	{
		return Datatype.getDatatypeById(INSTANCE.getItemDatatype(groupName, itemPath));
	}
}
