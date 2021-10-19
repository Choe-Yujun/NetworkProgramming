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

	// 카드 배분, 카드이 조합비교

	public boolean start() throws RemoteException
	{
		if (this.game == null && this.playerList.size() >= 3 && this.playerList.size() <= 5)
		{
			this.game = new GameController(this.playerList);
			System.out.println("게임 시작");
			curRound = game.round;
			return true;
		}
		return false;
	}

	// 플레이어가 방안에 몇명인지 알려줌
	public void AddPlayer(int playerNum) throws RemoteException
	{
		Player p = new Player(playerNum);
		this.playerList.add(p);
		System.out.println("방 [" + roomName + "]에 플레이어를 추가했습니다. 현재 플레이어 수: " + this.playerList.size());
		if (this.playerList.size() == 5)
		{
			start();
		}
	}

	// 플레이어가 방에서 떠났을 때 알려준다. 게임을 플레이 중일때 나가 인원수가 부족해지면 게임을 종료함
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
		str += ("방 [" + roomName + "]에서 플레이어가 나갔습니다. 현재 플레이어 수: " + this.playerList.size());

		if (game != null && playerList.size() < 3)
		{
			str += ("\n플레이어의 수가 부족합니다. 게임을 종료합니다.");
			game = null;
		}
		System.out.println(str);
		return str;
	}

	// 게임이 시작되면 자신의 손패와 상대방들의 예비패를 보기 위해 호출 하는 함수
	public String ShowHands(int playerNum) throws RemoteException
	{
		if (this.game != null)
		{
			String str = "";

			str += "-----------------OPPONENT CARDS-----------------\n"; // 상대방의 정보
			str += "Player:";
			for (int i = 0; i < playerList.size(); i++)
			{
				if (this.playerList.get(i).playerNumber != playerNum)
					str += String.format(" %2d (%2d)", // 상대방 (카드 개수)
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

	// 5는 모든 게임 종료. 4는 새 라운드 시작, 3은 벌칙카드, 2는 정지카드, 1은 일반카드
	// -1은 조커에러, -2는 카드수에러, -3는 인덱스에러, -4는 조합부족, -5은 차례에러
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
					System.out.println("플레이어 " + playerNum + "가 " + si + "부터 " + ei + "까지 카드를 냈습니다.");
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

	// 턴을 넘길 때 스페어카드를 얻는다. 다만 스페어카드가 하나도 없으면 패배한다.
	// 모든 게임이 종료되면 3. 패스하고 패배하면 2. 얻을 카드가 없어서 패배하면 1. 턴넘기면 0. index가 잘못되면 -1
	// 핸드의 크기와 위치가 안맞으면 -2. 스페어카드가 위치에 없으면 -3. 게임이 시작하지 않았으면 -4
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
						// 스페어카드를 얻을 수 있어서 패스를 하는 경우
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
						// 스페어카드가 없어서 패배하는 경우
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
						// 스페어카드가 존재하지만 선택한 곳에는 없는 경우
						else return -3;
					}
					// 자신의 차례가 아닐 때
					else return -4;
				}
			}
		}
		// 게임이 아직 시작하지 않았을 때
		return -5;

	}

	// 벌칙으로 인하여 카드를 가져가야 할때 호출 한다.
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

	// 현재 필드에 놓여있는 카드조합이 뭔지 확인.
	public String CurrentCombination() throws RemoteException
	{
		if (game != null)
		{
			String str = "현재 조합은 " + (game.GetCurWin() + 1) + "번째 턴의 " + game.GetCombInfo() + "입니다.";
			return str;
		}
		return "아직 게임이 시작하지 않았습니다.";
	}

	// 함수를 호출한 플레이어의 턴 넘버가 몇인지 확인.
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

	// 자신의 차래에 예비 카드를 손으로 가져가며 순서를 넘겨준다.
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
					System.out.println("새 게임 시작");
				}
				else
				{
					isEndGame = true;
					winner = "승리한 플레이어: ";
					loser = "패배한 플레이어: ";
					for (int i = 0; i < playerList.size(); i++)
					{
						if (playerList.get(i).life < 0)
							loser += playerList.get(i).playerNumber + " | ";
						else winner += playerList.get(i).playerNumber + " | ";
					}
					System.out.println("모든 게임 종료");
				}
			}
			else while (game.GetPassCount() == 0 && game.GetRedrawAmount() > 0)
			{
				redrawCards.add(game.DrawCard());
				game.RedrawCard();
			}
	}

	// 벌칙 카드를 가져가야할 때 벌칙 카드의 정보를 알려 주는 함수
	public String GetRedrawInfo() throws RemoteException
	{
		if (game != null)
		{
			String str = "남은 벌칙카드는 " + redrawCards.size() + "장 입니다." + "\n" + "이번에 얻은 카드는 "
					+ redrawCards.get(0).CardInfo() + "입니다. 핸드의 어디에 넣을까요?\n" + "명령어: -redraw 위치(숫자)";
			return str;
		}
		return "";
	}

	// 벌칙 카드를 추가하는 함수
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

	// 현재 누구 차례인지 확인.
	public String GetCurrentTurn() throws RemoteException
	{
		if (game != null)
		{
			String str = "현재 " + (game.GetTurn() + 1) + "턴의 차례입니다. 총 " + (playerList.size() - game.winTurn.size())
					+ "명 중 " + game.GetPassCount() + "명 진행했습니다.";
			if (this.redrawCards.size() > 0)
				str += (game.GetTurn() + 1) + "턴 플레이어는 벌칙카드를 얻어주세요.";
			return str;
		}
		return "";
	}

	public String CheckWin(int playerNum) throws RemoteException
	{
		if (game != null)
		{

			if (this.game.winTurn.contains(GetTurn(playerNum)))
				return "당신은 라운드에서 승리하였습니다. 게임이 끝날때까지 기다려주세요.";
		}
		return "";
	}

	// 각각 플레이어의 생명의 정보를 출력함
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
