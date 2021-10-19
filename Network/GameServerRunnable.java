package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

class GameServerRunnable implements Runnable
{
	public int clientID = -1;

	protected GameServer gameServer = null;
	protected Socket clientSocket = null;
	protected PrintWriter out = null;
	protected BufferedReader in = null;

	protected String currentRoom = "";
	protected GameCommand command = null;

	public GameServerRunnable(GameServer server, Socket socket)
	{
		this.gameServer = server;
		this.clientSocket = socket;
		clientID = clientSocket.getPort();		

		try
		{
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		}
		catch (IOException ie)
		{
			System.out.println(ie);
		}
	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		try
		{
			String inputLine;
			while ((inputLine = in.readLine()) != null)
			{
				if (inputLine.equalsIgnoreCase("-exit"))
					break;
				if (inputLine.equalsIgnoreCase("-help"))
				{
					String str = "";
					str += "-------------BASIC-------------\n";
					str += "'-help' Show command\n";
					str += "'-exit' Exit server\n";
					str += "-----------IN SERVER-----------\n";
					str += "'-list' Show room list\n";
					str += "'-join [room]' Join or/and make room\n";
					str += "------------IN ROOM------------\n";
					str += "'-leave [room]' to leave current room\n";
					str += "'-start' Start Game in current room\n";
					str += "------------IN GAME------------\n";
					str += "'-turn' Check your turn\n";
					str += "'-comb' Check current combination\n";
					str += "'-show' Show your hands\n";
					str += "'-put si ei (joker)' Put card si to ei. joker is option\n";
					str += "'-pass index pos' Pass your turn and get spare card[index] in hands pos. If you have no spare, you lose.\n";
					str += "'-redraw i' Add panalty of Redraw in your hands index i\n";
					str += "-------------------------------";
					gameServer.NoticeToClient(GetClientID(), str, false);
				}
				else
				{
					String[] parseChat = inputLine.split("\\s");
					boolean isChat = false;
					if (command == null)
					{
						if (inputLine.equalsIgnoreCase("-list"))
							gameServer.PrintRoomList(GetClientID()); // �� ����Ʈ ������
						else if (parseChat[0].equalsIgnoreCase("-join"))
						{
							if (parseChat.length == 2)
							{
								gameServer.JoinRoom(GetClientID(), parseChat[1]);
							}
							else
							{
								gameServer.NoticeToClient(GetClientID(), "Command Error. -join (RoomName)", false);
							}
						}
						else isChat = true;
					}
					else
					{
						// ��Ģī�带 ���� �� �� ���� ������ ���߰� ��Ģ�� �޾ƾ��ϴ� ������� ��Ģī�带 �򵵷��ϰ� �Ѵ�.
						int redrawAmount = command.GetRedrawAmount(GetClientID());
						if (redrawAmount > 0)
						{
							gameServer.NoticeToClient(GetClientID(), "��Ģī�带 ���� �մϴ�." + command.GetRedrawInfo(), false);
						}
						if (inputLine.equalsIgnoreCase("-leave"))
							Out();
						if (inputLine.equalsIgnoreCase("-start"))
						{
							if (command.start())
							{
								gameServer.NoticeToClient(GetClientID(), "������ ���۵Ǿ����ϴ�. -turn���� �ڽ��� ���� Ȯ�����ּ���.", true);
							}else
								gameServer.NoticeToClient(GetClientID(), "������ �̹� �������̰ų� �ο��� ���� �ʽ��ϴ�. 3~5������ �����ּ���.", false);
						}
						else if (inputLine.equalsIgnoreCase("-turn"))
						{
							String str = "����� �� ��ȣ��: " + command.GetTurn(GetClientID()) + "�Դϴ�. ";
							str += command.GetCurrentTurn();
							gameServer.NoticeToClient(GetClientID(), str, false);
							str = command.CheckWin(GetClientID());
							if (str.length() > 1)
								gameServer.NoticeToClient(GetClientID(), str, false);
						}
						else if (inputLine.equalsIgnoreCase("-comb"))
							gameServer.NoticeToClient(GetClientID(), command.CurrentCombination(), false);
						else if (inputLine.equalsIgnoreCase("-show"))
							gameServer.NoticeToClient(GetClientID(), command.ShowHands(GetClientID()), false);
						else if (parseChat[0].equalsIgnoreCase("-pass") && redrawAmount <= 0)
						{
							try
							{
								int i = Integer.parseInt(parseChat[1]);
								int p = Integer.parseInt(parseChat[2]);
								int b = command.PassWithSpare(GetClientID(), i, p);
								// ��� ������ ����Ǹ� 3. �н��ϰ� �й��ϸ� 2. ���� ī�尡 ��� �й��ϸ� 1. �ϳѱ�� 0. index�� �߸��Ǹ� -1
								// �ڵ��� ũ��� ��ġ�� �ȸ����� -2. �����ī�尡 ��ġ�� ������ -3. ������ �������� �ʾ����� -4
								String str = "";
								switch (b)
								{
									case 3:
										str = "��� ������ ����Ǿ����ϴ�.\n";
										str += "-----------RESULT-----------\n";
										str += command.ResultInfo();
										str += "----------------------------\n";
										break;
									case 2:
										str = "���尡 ����Ǿ����ϴ�. �� ���带 �����մϴ�.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									case 1:
										str = "�÷��̾� " + GetClientID() + "�� ���̻� ���� ī�尡 �����Ƿ� �й��մϴ�. ���带 ���� �����մϴ�.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									// ���� �ѱ� ���
									case 0:
										str = "�÷��̾� " + GetClientID() + "�� ���� �Ѱ���ϴ�.\n";
										str += command.GetCurrentTurn();
										break;

									case -1:
										str = "index�� 1 Ȥ�� 2�� �ۼ����ּ���.";
										break;
									case -2:
										str = "Hands�� ũ��� ������ ��ġ�� ���� �ʽ��ϴ�.";
										break;
									case -3:
										str = "�ش� index�� spareī�尡 �������� �ʽ��ϴ�.";
										break;
									case -4:
										str = "���� ����� ���ʰ� �ƴմϴ�.";
										break;
									case -5:
										str = "���� ������ �������� �ʾҽ��ϴ�.";
										break;
								}

								if (b >= 0)
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), str, false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-pass ���� ���ڷ� �Է��ϼ���.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-pass ���� ���ڷ� �Է��ϼ���.", false);
							}
						}
						else if (parseChat[0].equalsIgnoreCase("-put") && redrawAmount <= 0)
						{
							try
							{
								int si = Integer.parseInt(parseChat[1]);
								int ei = Integer.parseInt(parseChat[2]);
								int joker = -1;
								if (parseChat.length > 3)
									joker = Integer.parseInt(parseChat[3]);
								int b = command.PutCard(GetClientID(), si, ei, joker);

								String str = "�÷��̾� " + GetClientID() + "�� ";

								// 3�� ��Ģī��, 2�� ����ī��, 1�� �Ϲ�ī��, -1�� ��Ŀ����, -2�� ī�������, -3�� �ε�������, -3�� ���պ���, -4�� ���ʿ���
								switch (b)
								{
									case 5:
										str = "��� ������ ����Ǿ����ϴ�.\n";
										str += "-----------RESULT-----------\n";
										str += command.ResultInfo();
										str += "----------------------------\n";
										break;
									case 4:
										str = "���尡 ����Ǿ����ϴ�. �� ���带 �����մϴ�.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									case 3:
										str += "��Ģ ī�带 �½��ϴ�." + command.CurrentCombination() + "\n";
										str += command.GetCurrentTurn();
										break;
									case 2:
										str += "���� ī�带 �½��ϴ�." + command.GetCurrentTurn();
										break;
									case 1:
										str += "ī�带 �½��ϴ�." + command.CurrentCombination() + "\n";
										str += command.GetCurrentTurn();
										break;

									case -1:
										str = "��Ŀ�� 1~12�� ���ڸ� �Է����ּ���.";
										break;
									case -2:
										str = "ī��� �� 3������� �� �� �ֽ��ϴ�.";
										break;
									case -3:
										str = "-put start end�� ������ ���ּ���.";
										break;
									case -4:
										str = "�� ���� ī�� ������ ���ּ���.";
										break;
									case -5:
										str = "���� ����� ���ʰ� �ƴմϴ�.";
										break;
									case -6:
										str = "���� ������ �������� �ʾҽ��ϴ�.";
										break;
								}

								if (b >= 0)
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), str, false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-put ���� ���� (����)�� �Է����ּ���.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-put ���� ���� (����)�� �Է����ּ���.", false);
							}
						} // ��Ģī�����̸� ī�带 ���� ���� �� ���� �����ؾ� ��.
						else if (parseChat[0].equalsIgnoreCase("-redraw") && redrawAmount > 0)
						{
							try
							{
								int i = Integer.parseInt(parseChat[1]);
								String str = "�÷��̾� " + GetClientID() + "�� ī�带 �п� �߰��߽��ϴ�. ";
								if (redrawAmount > 1)
								{
									str += (redrawAmount - 1) + "��ŭ ���ҽ��ϴ�.";
								}
								else if (redrawAmount == 1)
								{
									str += "������ �簳���ּ���.";
								}
								if (command.GetCard(GetClientID(), i))
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), "Hands�� ũ��� ������ ��ġ�� ���� �ʽ��ϴ�.", false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-put ���� ���� (����)�� �Է����ּ���.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-put ���� ���� (����)�� �Է����ּ���.", false);
							}
							continue;
						}
						else isChat = true;
					}
					if (isChat)
						gameServer.ClientChat(GetClientID(), inputLine);
				}
			}
			gameServer.RemoveClient(GetClientID());
		}
		catch (IOException ie)
		{
			gameServer.RemoveClient(GetClientID());
		}
	}

	// ����ڰ� �濡 ���� �� ������ �˷���
	public void Join(String roomName)
	{
		try
		{
			command = (GameCommand) Naming.lookup("rmi://localhost:1099/" + roomName);
			command.AddPlayer(GetClientID());
			gameServer.NoticeToClient(GetClientID(), GetClientID() + "�� �� [" + roomName + "]�� �����ϴ�.", true);
			currentRoom = roomName;
			gameServer.NoticeToClient(GetClientID(), GetClientID() + "�� �� [" + roomName + "]�� ���Խ��ϴ�.", true);
		}
		catch (MalformedURLException mue)
		{
			gameServer.NoticeToClient(GetClientID(), "MalformedURLException: " + mue, false);
			System.out.println("MalformedURLException: " + mue);
		}
		catch (RemoteException re)
		{
			gameServer.NoticeToClient(GetClientID(), "RemoteException: " + re, false);
			System.out.println("RemoteException: " + re);
		}
		catch (NotBoundException nbe)
		{
			gameServer.NoticeToClient(GetClientID(), "NotBoundException: " + nbe, false);
			System.out.println("NotBoundException: " + nbe);
		}
		catch (java.lang.ArithmeticException ae)
		{
			gameServer.NoticeToClient(GetClientID(), "java.lang.ArithmeticException : " + ae, false);
			System.out.println("java.lang.ArithmeticException : " + ae);
		}
	}

	// �÷��̾ ���� ������ �� �˷��ָ� ���� �ο� ���� üũ��, 0���� ������ ���� ���� ��
	public void Out()
	{
		try
		{
			if (command != null)
			{
				gameServer.NoticeToClient(GetClientID(), command.RemovePlayer(GetClientID()), true);
				if (command.GetPlayerAmount() <= 0)
					gameServer.RemoveRoom(currentRoom);
				command = null;
			}
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int GetClientID()
	{
		return clientID;
	}

	public String GetRoomName()
	{
		return currentRoom;
	}

	public void close()
	{
		try
		{
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (clientSocket != null)
				clientSocket.close();
		}
		catch (IOException ie)
		{
			System.out.println(ie);
		}
	}

}