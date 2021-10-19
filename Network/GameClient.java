package Network;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.rmi.NotBoundException;
import javax.net.ssl.*;

public class GameClient
{
	static String server = "localhost";
	static int port = 1099;// 0000;
	static int rmiport = 1199;
	static SSLSocket clientSocket = null;
	static SSLSocketFactory socketFactory = null;

	String roomName = "";

	public static void main(String[] args) throws NotBoundException
	{
		if (args.length != 3)
		{
			System.out.println("Usage: Classname ServerName Port Password");
			System.exit(1);
		}
		server = args[0];
		port = Integer.parseInt(args[1]);

		try
		{
			System.setProperty("javax.net.ssl.trustStore", "trustedcerts");
			System.setProperty("javax.net.ssl.trustStorePassword", args[2]);

			socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			clientSocket = (SSLSocket) socketFactory.createSocket(server, port);
		}
		catch (BindException b)
		{
			System.out.println("Can't bind on: " + port);
			System.exit(1);
		}
		catch (IOException ie)
		{
			System.out.println(ie);
			System.exit(1);
		}
		new Thread(new ClientReceiver(clientSocket)).start();
		new Thread(new ClientSender(clientSocket)).start();
	}
}

class ClientSender implements Runnable
{
	private SSLSocket clientSocket = null;

	ClientSender(SSLSocket socket)
	{
		this.clientSocket = socket;
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		Scanner keyIn = null;
		PrintWriter out = null;
		try
		{
			keyIn = new Scanner(System.in);
			out = new PrintWriter(clientSocket.getOutputStream(), true);

			String userInput = "";
			System.out.println("Your ID is " + clientSocket.getLocalPort());
			System.out.println("Please Type '-help' to show command");
			while ((userInput = keyIn.nextLine()) != null)
			{
				out.println(userInput);
				out.flush();
				if (userInput.equalsIgnoreCase("-exit"))
					break;
			}
			keyIn.close();
			out.close();
			clientSocket.close();
		}
		catch (IOException ie)
		{
			try
			{
				if (out != null)
					out.close();
				if (keyIn != null)
					keyIn.close();
				if (clientSocket != null)
					clientSocket.close();
			}
			catch (IOException e)
			{
				System.out.println(e);
			}
		}
	}

}

class ClientReceiver implements Runnable
{
	private SSLSocket clientSocket = null;

	ClientReceiver(SSLSocket socket)
	{
		this.clientSocket = socket;
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		while (clientSocket.isConnected())
		{
			BufferedReader in = null;
			try
			{
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String readSome = null;
				while ((readSome = in.readLine()) != null)
				{
					System.out.println(readSome);
				}
				in.close();
				clientSocket.close();
			}
			catch (IOException ie)
			{
				System.out.println(ie);
			}
			System.out.println("Leave.");
			System.exit(1);
		}

	}
}
