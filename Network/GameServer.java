package Network;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.KeyStore;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.ArrayList;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;

public class GameServer implements Runnable
{
	private ArrayList<String> roomList = new ArrayList<String>();
	private static ArrayList<GameServerRunnable> clients = new ArrayList<GameServerRunnable>();
	
	private int port = 1199;// -1;
	private static int rmiport = 1099;

	ServerSocketFactory ssf = null;
	SSLServerSocket serverSocket = null;
	static KeyStore ks;
	static KeyManagerFactory kmf;
	static SSLContext sc;
	static SSLEngine se;
	static String runRoot = "./";
	static String ksName = runRoot + ".keystore/SSLSocketServerKey";

	public GameServer(int p)
	{
		try
		{
			port = p;
		}
		catch (Exception e)
		{
			System.out.println("Trouble: " + e);
		}
	}

	public static void StartRegistry(int port) throws RemoteException
	{
		try
		{
			Registry registry = LocateRegistry.getRegistry(port);
			registry.list();
		}
		catch (RemoteException re)
		{
			System.out.println("RMI registry is not located at port " + port);
			LocateRegistry.createRegistry(port);
			System.out.println("RMI registry created at port " + port);
		}
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		try
		{
			se = sc.createSSLEngine();
			se.setUseClientMode(false);
			ssf = sc.getServerSocketFactory();
			serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
			serverSocket.setWantClientAuth(true);
			System.out.println("Server started: socket created on " + port);

			StartRegistry(rmiport);
			while (true)
			{
				AddClient(serverSocket);
			}
		}
		catch (BindException be)
		{
			System.out.println("Can't bind on: " + port);
		}
		catch (IOException ie)
		{
			System.out.println(ie);
		}
		finally
		{
			try
			{
				if (serverSocket != null)
					serverSocket.close();

			}
			catch (IOException ie)
			{
				System.out.println(ie);
			}
		}
	}

	// client?????? ????????? ?????? client ????????? ??????
	public void AddClient(SSLServerSocket server)
	{
		SSLSocket clientSocket = null;

		try
		{
			clientSocket = (SSLSocket) server.accept();

			String[] supported = clientSocket.getSupportedCipherSuites();

			clientSocket.setEnabledCipherSuites(supported);
			clientSocket.startHandshake();

			GameServerRunnable newClient = new GameServerRunnable(this, clientSocket);

			clients.add(newClient);
			int index = clients.size() - 1;
			new Thread(clients.get(index)).start();
			System.out.println("Client connected: " + clientSocket.getPort() + ", CurrentClient: " + clients.size());
		}
		catch (IOException ie)
		{
			System.out.println("Socket " + clientSocket.getPort() + " Accept fail: " + ie);
		}
	}

	// client??? ????????? ???????????? client ???????????? ??????
	public synchronized void RemoveClient(int clientID)
	{
		GameServerRunnable endClient = null;
		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				endClient = clients.get(i);
				clients.get(i).Out();
				clients.remove(i);
				System.out.println(
						"Client removed: " + clientID + " at clients[" + i + "], CurrentClient: " + clients.size());
				endClient.close();
			}
		}
	}

	// ??? ?????? ??????
	public void PrintRoomList(int clientID)
	{
		int index = -1;
		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				index = i;
				break;
			}
		}
		if (index < 0)
			return;

		if (roomList.size() == 0)
		{
			clients.get(index).out.println("No room in server");
			return;
		}
		for (int i = 0; i < roomList.size(); i++)
		{
			clients.get(index).out.println((i + 1) + ". [" + roomList.get(i) + "]");
		}
	}

	// ????????? ??? ??????, ?????? ?????????????????? ????????? ??? ?????? ?????????
	public void JoinRoom(int clientID, String roomName)
	{
		int index = -1;
		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				index = i;
				break;
			}
		}
		if (index < 0)
			return;

		if (roomList.contains(roomName) == false)
		{
			try
			{
				GameCommand c = new GameImpl(roomName);
				Naming.rebind("rmi://localhost:1099/" + roomName, c);
				System.out.println("room" + roomName + " is Created");
				roomList.add(roomName);
				NoticeToClient(-1, "??? [" + roomName + "]??? ?????????????????????.", true);
			}
			catch (MalformedURLException me)
			{
				NoticeToClient(clientID, "?????? ????????? ?????? ??????????????????. ??? ????????? ?????????????????????.", false);
			}
			catch (Exception e)
			{
				System.out.println("TroubleL: " + e);
			}
		}
		clients.get(index).Join(roomName);
	}

	// ?????? ?????? ?????? ??????????????? ????????? ?????? ?????? ??????
	public void RemoveRoom(String roomName)
	{
		if (roomList.contains(roomName))
		{
			roomList.remove(roomName);
			try
			{
				Naming.unbind("rmi://" + "localhost" + ":1099/" + roomName);
			}
			catch (RemoteException | MalformedURLException | NotBoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// ???????????? ????????? ???????????? ????????? ????????? ????????? ?????? ?????????????????? ?????? ????????? ?????? ?????? ????????? ????????? ?????? ????????????????????? ??????
	public void ClientChat(int clientID, String inputLine)
	{
		String room = "";
		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				room = clients.get(i).GetRoomName();
				break;
			}
		}

		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				System.out.println(clientID + " Say in Room " + room + ": " + inputLine);
			}
			else if (clients.get(i).GetRoomName().compareTo(room) == 0)
			{
				System.out.println("Write " + clients.get(i).GetClientID() + " in Room " + room + " [" + clientID + ": "
						+ inputLine + "]");
				clients.get(i).out.println(clientID + ": " + inputLine);
			}
		}
	}

	// ??????????????? ??????????????? ?????? ????????? ????????? (room??? ??????, ????????? ?????? ???), ?????? ???????????? ???????????? ????????? ?????? ????????? ?????????
	public void NoticeToClient(int clientID, String inputLine, boolean sayEveryone)
	{
		String room = "";
		for (int i = 0; i < clients.size(); i++)
		{
			if (clients.get(i).GetClientID() == clientID)
			{
				room = clients.get(i).GetRoomName();
				break;
			}
		}

		for (int i = 0; i < clients.size(); i++)
		{
			if (sayEveryone == false && clients.get(i).GetClientID() != clientID)
				continue;
			if (clients.get(i).GetRoomName().compareTo(room) == 0)
				clients.get(i).out.println(inputLine);
		}
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 3)
		{
			System.out.println("Usage: Classname Serverport(not 1099) KeyStorePass KeyPass");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		char[] keyStorePass = args[1].toCharArray();
		char[] keyPass = args[2].toCharArray();

		if (port == 1099)
		{
			System.out.println("Port cannot 1099");
			System.exit(1);
		}

		try
		{
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(ksName), keyStorePass);

			kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keyPass);

			sc = SSLContext.getInstance("TLS");
			sc.init(kmf.getKeyManagers(), null, null);

			se = sc.createSSLEngine();
			se.setUseClientMode(false);

		}
		catch (Exception re)
		{
			re.printStackTrace();
		}

		new Thread(new GameServer(port)).start();
	}
}