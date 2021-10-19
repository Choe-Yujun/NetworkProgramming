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
							gameServer.PrintRoomList(GetClientID()); // 룸 리스트 보여줌
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
						// 벌칙카드를 얻어야 할 때 게임 진행을 멈추고 벌칙을 받아야하는 사람에게 벌칙카드를 얻도록하게 한다.
						int redrawAmount = command.GetRedrawAmount(GetClientID());
						if (redrawAmount > 0)
						{
							gameServer.NoticeToClient(GetClientID(), "벌칙카드를 얻어야 합니다." + command.GetRedrawInfo(), false);
						}
						if (inputLine.equalsIgnoreCase("-leave"))
							Out();
						if (inputLine.equalsIgnoreCase("-start"))
						{
							if (command.start())
							{
								gameServer.NoticeToClient(GetClientID(), "게임이 시작되었습니다. -turn으로 자신의 턴을 확인해주세요.", true);
							}else
								gameServer.NoticeToClient(GetClientID(), "게임이 이미 진행중이거나 인원이 맞지 않습니다. 3~5인으로 맞춰주세요.", false);
						}
						else if (inputLine.equalsIgnoreCase("-turn"))
						{
							String str = "당신의 턴 번호는: " + command.GetTurn(GetClientID()) + "입니다. ";
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
								// 모든 게임이 종료되면 3. 패스하고 패배하면 2. 얻을 카드가 없어서 패배하면 1. 턴넘기면 0. index가 잘못되면 -1
								// 핸드의 크기와 위치가 안맞으면 -2. 스페어카드가 위치에 없으면 -3. 게임이 시작하지 않았으면 -4
								String str = "";
								switch (b)
								{
									case 3:
										str = "모든 게임이 종료되었습니다.\n";
										str += "-----------RESULT-----------\n";
										str += command.ResultInfo();
										str += "----------------------------\n";
										break;
									case 2:
										str = "라운드가 종료되었습니다. 새 라운드를 시작합니다.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									case 1:
										str = "플레이어 " + GetClientID() + "가 더이상 뽑을 카드가 없으므로 패배합니다. 라운드를 새로 시작합니다.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									// 턴을 넘긴 경우
									case 0:
										str = "플레이어 " + GetClientID() + "가 턴을 넘겼습니다.\n";
										str += command.GetCurrentTurn();
										break;

									case -1:
										str = "index를 1 혹은 2로 작성해주세요.";
										break;
									case -2:
										str = "Hands의 크기와 설정한 위치가 맞지 않습니다.";
										break;
									case -3:
										str = "해당 index에 spare카드가 존재하지 않습니다.";
										break;
									case -4:
										str = "아직 당신의 차례가 아닙니다.";
										break;
									case -5:
										str = "아직 게임이 시작하지 않았습니다.";
										break;
								}

								if (b >= 0)
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), str, false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-pass 숫자 숫자로 입력하세요.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-pass 숫자 숫자로 입력하세요.", false);
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

								String str = "플레이어 " + GetClientID() + "가 ";

								// 3은 벌칙카드, 2는 정지카드, 1은 일반카드, -1은 조커에러, -2는 카드수에러, -3는 인덱스에러, -3는 조합부족, -4은 차례에러
								switch (b)
								{
									case 5:
										str = "모든 게임이 종료되었습니다.\n";
										str += "-----------RESULT-----------\n";
										str += command.ResultInfo();
										str += "----------------------------\n";
										break;
									case 4:
										str = "라운드가 종료되었습니다. 새 라운드를 시작합니다.\n";
										str += command.LifeInfo();
										str += command.GetCurrentTurn();
										break;
									case 3:
										str += "벌칙 카드를 냈습니다." + command.CurrentCombination() + "\n";
										str += command.GetCurrentTurn();
										break;
									case 2:
										str += "정지 카드를 냈습니다." + command.GetCurrentTurn();
										break;
									case 1:
										str += "카드를 냈습니다." + command.CurrentCombination() + "\n";
										str += command.GetCurrentTurn();
										break;

									case -1:
										str = "조커는 1~12의 숫자를 입력해주세요.";
										break;
									case -2:
										str = "카드는 총 3장까지만 낼 수 있습니다.";
										break;
									case -3:
										str = "-put start end의 순서로 내주세요.";
										break;
									case -4:
										str = "더 강한 카드 조합을 내주세요.";
										break;
									case -5:
										str = "아직 당신의 차례가 아닙니다.";
										break;
									case -6:
										str = "아직 게임이 시작하지 않았습니다.";
										break;
								}

								if (b >= 0)
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), str, false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-put 숫자 숫자 (숫자)로 입력해주세요.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-put 숫자 숫자 (숫자)로 입력해주세요.", false);
							}
						} // 벌칙카드턴이면 카드를 전부 먹을 때 까지 진행해야 함.
						else if (parseChat[0].equalsIgnoreCase("-redraw") && redrawAmount > 0)
						{
							try
							{
								int i = Integer.parseInt(parseChat[1]);
								String str = "플레이어 " + GetClientID() + "가 카드를 패에 추가했습니다. ";
								if (redrawAmount > 1)
								{
									str += (redrawAmount - 1) + "만큼 남았습니다.";
								}
								else if (redrawAmount == 1)
								{
									str += "게임을 재개해주세요.";
								}
								if (command.GetCard(GetClientID(), i))
									gameServer.NoticeToClient(GetClientID(), str, true);
								else gameServer.NoticeToClient(GetClientID(), "Hands의 크기와 설정한 위치가 맞지 않습니다.", false);
							}
							catch (NumberFormatException ne)
							{
								gameServer.NoticeToClient(GetClientID(), "-put 숫자 숫자 (숫자)로 입력해주세요.", false);
							}
							catch (ArrayIndexOutOfBoundsException ae)
							{
								gameServer.NoticeToClient(GetClientID(), "-put 숫자 숫자 (숫자)로 입력해주세요.", false);
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

	// 사용자가 방에 들어가면 그 정보를 알려줌
	public void Join(String roomName)
	{
		try
		{
			command = (GameCommand) Naming.lookup("rmi://localhost:1099/" + roomName);
			command.AddPlayer(GetClientID());
			gameServer.NoticeToClient(GetClientID(), GetClientID() + "가 방 [" + roomName + "]에 들어갔습니다.", true);
			currentRoom = roomName;
			gameServer.NoticeToClient(GetClientID(), GetClientID() + "가 방 [" + roomName + "]에 들어왔습니다.", true);
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

	// 플레이어가 방을 떠났을 때 알려주며 방의 인원 수를 체크해, 0보다 작으면 방을 없에 줌
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