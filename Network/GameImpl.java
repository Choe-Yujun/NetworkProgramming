package Network;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import GamePlay.Card;
import GamePlay.Combination;
import GamePlay.GameController;
import GamePlay.Player;
import java.util.ArrayList;

public class GameImpl extends UnicastRemoteObject implements GameCommand
{
	private String roomName;
	private GameController game = null;
	private ArrayList<Player> playerList = new ArrayList<Player>();
	private int curRound = 0;

	private boolean isEndGame = false;
	private String loser = "";
	private String winner = "";

	public int GetPlayerAmount() throws RemoteException
	{
		return playerList.size();
	}

	protected GameImpl(String room) throws RemoteException
	{
		super();
		roomName = room;
		// TODO Auto-generated constructor stub
	}

	// ī�� ���, ī���� ���պ�

	public boolean start() throws RemoteException
	{
		if (this.game == null && this.playerList.size() >= 3 && this.playerList.size() <= 5)
		{
			this.game = new GameController(this.playerList);
			System.out.println("���� ����");
			curRound = game.round;
			return true;
		}
		return false;
	}

	// �÷��̾ ��ȿ� ������� �˷���
	public void AddPlayer(int playerNum) throws RemoteException
	{
		Player p = new Player(playerNum);
		this.playerList.add(p);
		System.out.println("�� [" + roomName + "]�� �÷��̾ �߰��߽��ϴ�. ���� �÷��̾� ��: " + this.playerList.size());
		if (this.playerList.size() == 5)
		{
			start();
		}
	}

	// �÷��̾ �濡�� ������ �� �˷��ش�. ������ �÷��� ���϶� ���� �ο����� ���������� ������ ������
	public String RemovePlayer(int playerNum) throws RemoteException
	{
		String str = "";
		for (int i = 0; i < playerList.size(); i++)
		{
			if (this.playerList.get(i).playerNumber == playerNum)
			{
				this.playerList.remove(i);
				break;
			}
		}
		str += ("�� [" + roomName + "]���� �÷��̾ �������ϴ�. ���� �÷��̾� ��: " + this.playerList.size());

		if (game != null && playerList.size() < 3)
		{
			str += ("\n�÷��̾��� ���� �����մϴ�. ������ �����մϴ�.");
			game = null;
		}
		System.out.println(str);
		return str;
	}

	// ������ ���۵Ǹ� �ڽ��� ���п� ������� �����и� ���� ���� ȣ�� �ϴ� �Լ�
	public String ShowHands(int playerNum) throws RemoteException
	{
		if (this.game != null)
		{
			String str = "";

			str += "-----------------OPPONENT CARDS-----------------\n"; // ������ ����
			str += "Player:";
			for (int i = 0; i < playerList.size(); i++)
			{
				if (this.playerList.get(i).playerNumber != playerNum)
					str += String.format(" %2d (%2d)", // ���� (ī�� ����)
							this.playerList.get(i).playerTurn + 1, this.playerList.get(i).hands.size());
			}
			str += "\n";

			str += "Index: ";
			for (int i = 0; i < (playerList.size() - 1) * 2; i++)
			{
				str += String.format(" %2d ", i % 2 + 1);
			}
			str += "\n";

			str += "Spare: ";
			for (int i = 0; i < playerList.size(); i++)
			{
				if (this.playerList.get(i).playerNumber != playerNum)
					str += this.playerList.get(i).PrintSpare();
			}
			str += "\n";

			str += "-------------------YOUR CARDS-------------------\n";
			for (int i = 0; i < playerList.size(); i++)
			{
				if (this.playerList.get(i).playerNumber == playerNum)
					str += this.playerList.get(i).PrintHands();
			}
			str += "\n------------------------------------------------";
			return str;
		}
		return "";
	}

	// 5�� ��� ���� ����. 4�� �� ���� ����, 3�� ��Ģī��, 2�� ����ī��, 1�� �Ϲ�ī��
	// -1�� ��Ŀ����, -2�� ī�������, -3�� �ε�������, -4�� ���պ���, -5�� ���ʿ���
	public int PutCard(int playerNum, int si, int ei, int joker) throws RemoteException
	{
		if (this.game != null)
		{
			if ((joker != -1 && joker < 1) || joker > 12)
				return -1;
			if (si > ei || (ei - si) >= 3)
				return -2;

			boolean isPut = false;
			for (int i = 0; i < playerList.size(); i++)
			{
				Player player = this.playerList.get(i);

				if (player.playerNumber == playerNum)
				{
					System.out.println("�÷��̾� " + playerNum + "�� " + si + "���� " + ei + "���� ī�带 �½��ϴ�.");
					if (player.playerTurn == this.game.GetTurn())
					{
						if (si < 1 || ei > player.hands.size())
							return -3;

						Combination c = player.SelectCardList(si, ei, joker);
						isPut = this.game.PutCombination(c, player.playerTurn);
						if (isPut == true)
						{
							player.RemoveCard(si, ei);
							PassTurn();
							if (isEndGame)
							{
								isEndGame = false;
								game = null;
								return 5;
							}
							if (this.curRound != game.round)
							{
								this.curRound = game.round;
								return 4;
							}

							switch (c.GetCombinationType())
							{
								case redraw:
									return 3;
								case stop:
									return 2;
								default:
									return 1;
							}
						}
						else return -4;
					}
					else return -5;
				}
			}
		}
		return -6;

	}

	// ���� �ѱ� �� �����ī�带 ��´�. �ٸ� �����ī�尡 �ϳ��� ������ �й��Ѵ�.
	// ��� ������ ����Ǹ� 3. �н��ϰ� �й��ϸ� 2. ���� ī�尡 ��� �й��ϸ� 1. �ϳѱ�� 0. index�� �߸��Ǹ� -1
	// �ڵ��� ũ��� ��ġ�� �ȸ����� -2. �����ī�尡 ��ġ�� ������ -3. ������ �������� �ʾ����� -4
	public int PassWithSpare(int playerNum, int index, int pos) throws RemoteException
	{
		if (game != null)
		{
			if (index != 1 && index != 2)
			{
				return -1;
			}
			for (int i = 0; i < playerList.size(); i++)
			{
				if (playerList.get(i).playerNumber == playerNum)
				{

					if (pos < 1 || pos > playerList.get(i).hands.size() + 1)
					{
						return -2;
					}
					if (game.GetTurn() == i)
					{
						// �����ī�带 ���� �� �־ �н��� �ϴ� ���
						if (playerList.get(i).spare[index - 1] != null)
						{
							playerList.get(i).GetSpare(index - 1, pos - 1);
							PassTurn();
							if (isEndGame)
							{
								isEndGame = false;
								game = null;
								return 3;
							}
							else if (this.curRound != game.round)
							{
								this.curRound = game.round;
								return 2;
							}
							return 0;
						}
						// �����ī�尡 ��� �й��ϴ� ���
						else if (playerList.get(i).spare[0] == null && playerList.get(i).spare[1] == null)
						{
							playerList.get(i).life--;
							if (game.InitializeGame(i))
							{
								this.curRound = game.round;
								return 1;
							}
							else
							{
								isEndGame = false;
								game = null;
								return 3;
							}
						}
						// �����ī�尡 ���������� ������ ������ ���� ���
						else return -3;
					}
					// �ڽ��� ���ʰ� �ƴ� ��
					else return -4;
				}
			}
		}
		// ������ ���� �������� �ʾ��� ��
		return -5;

	}

	// ��Ģ���� ���Ͽ� ī�带 �������� �Ҷ� ȣ�� �Ѵ�.
	public boolean GetCard(int playerNum, int index) throws RemoteException
	{
		if (game != null)
		{
			for (int i = 0; i < playerList.size(); i++)
			{
				if (playerList.get(i).playerNumber == playerNum)
				{
					if (index >= 1 && index <= (playerList.get(i).hands.size() + 1))
					{
						playerList.get(i).AddCard(redrawCards.get(0), index - 1);
						redrawCards.remove(0);
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	// ���� �ʵ忡 �����ִ� ī�������� ���� Ȯ��.
	public String CurrentCombination() throws RemoteException
	{
		if (game != null)
		{
			String str = "���� ������ " + (game.GetCurWin() + 1) + "��° ���� " + game.GetCombInfo() + "�Դϴ�.";
			return str;
		}
		return "���� ������ �������� �ʾҽ��ϴ�.";
	}

	// �Լ��� ȣ���� �÷��̾��� �� �ѹ��� ������ Ȯ��.
	public int GetTurn(int playerNum) throws RemoteException
	{
		if (game != null)
		{
			for (int i = 0; i < playerList.size(); i++)
			{
				if (playerList.get(i).playerNumber == playerNum)
				{
					return (playerList.get(i).playerTurn + 1);
				}
			}
		}
		return -1;
	}

	private ArrayList<Card> redrawCards = new ArrayList<Card>();

	// �ڽ��� ������ ���� ī�带 ������ �������� ������ �Ѱ��ش�.
	public void PassTurn() throws RemoteException
	{
		game.PassTurn();
		if (game.winTurn.size() != 0)
			if (playerList.size() <= game.winTurn.size() + 1)
			{
				int p = game.GetLosePlayer();
				playerList.get(p).life--;
				if (game.InitializeGame(p))
				{
					System.out.println("�� ���� ����");
				}
				else
				{
					isEndGame = true;
					winner = "�¸��� �÷��̾�: ";
					loser = "�й��� �÷��̾�: ";
					for (int i = 0; i < playerList.size(); i++)
					{
						if (playerList.get(i).life < 0)
							loser += playerList.get(i).playerNumber + " | ";
						else winner += playerList.get(i).playerNumber + " | ";
					}
					System.out.println("��� ���� ����");
				}
			}
			else while (game.GetPassCount() == 0 && game.GetRedrawAmount() > 0)
			{
				redrawCards.add(game.DrawCard());
				game.RedrawCard();
			}
	}

	// ��Ģ ī�带 ���������� �� ��Ģ ī���� ������ �˷� �ִ� �Լ�
	public String GetRedrawInfo() throws RemoteException
	{
		if (game != null)
		{
			String str = "���� ��Ģī��� " + redrawCards.size() + "�� �Դϴ�." + "\n" + "�̹��� ���� ī��� "
					+ redrawCards.get(0).CardInfo() + "�Դϴ�. �ڵ��� ��� �������?\n" + "��ɾ�: -redraw ��ġ(����)";
			return str;
		}
		return "";
	}

	// ��Ģ ī�带 �߰��ϴ� �Լ�
	public int GetRedrawAmount(int playerNum) throws RemoteException
	{
		if (game != null)
		{
			for (int i = 0; i < playerList.size(); i++)
			{
				if (playerList.get(i).playerNumber == playerNum)
				{
					if (playerList.get(i).playerTurn == game.GetTurn())
						return redrawCards.size();
				}
			}
		}
		return -1;
	}

	// ���� ���� �������� Ȯ��.
	public String GetCurrentTurn() throws RemoteException
	{
		if (game != null)
		{
			String str = "���� " + (game.GetTurn() + 1) + "���� �����Դϴ�. �� " + (playerList.size() - game.winTurn.size())
					+ "�� �� " + game.GetPassCount() + "�� �����߽��ϴ�.";
			if (this.redrawCards.size() > 0)
				str += (game.GetTurn() + 1) + "�� �÷��̾�� ��Ģī�带 ����ּ���.";
			return str;
		}
		return "";
	}

	public String CheckWin(int playerNum) throws RemoteException
	{
		if (game != null)
		{

			if (this.game.winTurn.contains(GetTurn(playerNum)))
				return "����� ���忡�� �¸��Ͽ����ϴ�. ������ ���������� ��ٷ��ּ���.";
		}
		return "";
	}

	// ���� �÷��̾��� ������ ������ �����
	public String LifeInfo() throws RemoteException
	{
		if (game != null)
		{
			String str = "";
			str += "Life: [ ";

			for (int i = 0; i < playerList.size(); i++)
			{
				str += playerList.get(i).playerNumber + ": " + playerList.get(i).life;
				if (i != playerList.size() - 1)
					str += " | ";
			}
			str += " ]\n";

			return str;
		}
		return "";
	}

	public String ResultInfo() throws RemoteException
	{
		String str = winner + "\n" + loser + "\n";
		winner = "";
		loser = "";
		return str;
	}
}
