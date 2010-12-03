package ch.cern.opc.client;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

interface DllInterface extends Library 
{
	DllInterface INSTANCE = (DllInterface)
    Native.loadLibrary("AutomatedOpcTester.dll", DllInterface.class);

    public void init(String host, String server);
    public boolean getItemNames(String[] itemsBuffer, int elementLength, int numElements, int offset);
    public NativeLong createGroup(String groupName, NativeLong requestedRefreshRate);
    public boolean addItem(String groupName, String itemPath);
    public boolean readItemSync(String groupName, String itemPath, byte buffer[], int bufferSz);
    public boolean readItemAsync(String groupName, String itemPath);
    public boolean writeItemSync(String groupName, String itemPath, String value);
    public boolean writeItemAsync(String groupName, String itemPath, String value);
    public void getLastError(byte buffer[], int bufferSz);
    public void registerAsyncUpdate(AsyncUpdateCallback cb);
}
