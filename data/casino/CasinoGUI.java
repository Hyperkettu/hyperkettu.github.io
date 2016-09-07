package CasinoGUI;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;

import Casino.Table;
import Casino.Card;
import Casino.Human;
import Casino.NonUser1;
import Casino.Player;

/**
 * This class models a graphic user interface for casino game
 */

public class CasinoGUI extends JFrame implements ActionListener,
		WindowListener, MouseListener {

	private boolean windowOpened = false;

	private InfoWindow infoWindow;

	private CasinoPane contentPane;

	private NewGameWindow newGame;

	private SaveFileWindow saveWindow;

	private LoadFileWindow loadWindow;

	private LinkedList<Table> temp;

	private boolean alreadySelected; // if player has selected hand card

	private CardButton selectedPlayButton; // player's play card's button

	private Table currentTable;

	public CasinoGUI(String name) {
		super(name);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.temp = new LinkedList<Table>();
		this.alreadySelected = false;

		// set menu layout
		Color color = new Color(221, 222, 254);
		Font menuFont = new Font("Centaur", Font.PLAIN, 14);

		// Create Menubar
		JMenuBar menubar = new JMenuBar();
		createMenu(menubar, color, menuFont);
		menubar.setBorder(BorderFactory.createLineBorder(color));

		// Create content pane
		contentPane = new CasinoPane(this);
		contentPane.setPreferredSize(new Dimension(1000, 600));
		setContentPane(contentPane);

		// set components to JFrame
		setJMenuBar(menubar);

		// set visible
		pack();
		setResizable(false);
		setVisible(true);

	}

	/**
	 * This method implements ActionListener interface. This CasinoGUI is
	 * listener of all other classes, every action is handled here
	 */

	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		if (command.equals("Uusi peli")) {
			if (!this.windowOpened) {
				newGame = new NewGameWindow("Uusi peli", this);
				newGame.addWindowListener(this);
				newGame.setVisible(true);
				newGame.setAlwaysOnTop(true);
				this.windowOpened = true;
			} else
				Toolkit.getDefaultToolkit().beep();
		}

		if (command.equals("Tallenna")) {
			if (!this.windowOpened) {
				this.saveWindow = new SaveFileWindow("Tallenna peli", this);
				this.saveWindow.setVisible(true);
				this.saveWindow.setAlwaysOnTop(true);
				this.saveWindow.addWindowListener(this);
				this.windowOpened = true;
			} else
				Toolkit.getDefaultToolkit().beep();
		}

		if (command.equals("Siirry peliin")) {

			if (!temp.isEmpty() && temp.getFirst().getPlayerCount() > 1) {

				// clear contentPane if new game before new game is started
				if (this.currentTable != null) {
					this.contentPane.getRightPanel().removeAll();
					this.contentPane.getPlayerPanel().removeAll();
					this.contentPane.getTablePanel().removeAll();
					this.contentPane.getCardsOnTable().clear();
				}

				// set first temp game as current game
				this.currentTable = temp.getFirst();

				// clear temp
				temp.clear();

				this.alreadySelected = false;

				// do preparations for game
				this.currentTable.createCards();
				this.currentTable.finalizeRing();
				this.currentTable.playNewRound();

				// set up game window for start
				this.contentPane.startSetUp();

				// adds starting cards to table
				for (Card card : this.currentTable.getCardsOnTable()) {
					this.contentPane.addCardToTable(new CardButton(this, card));
				}

				// adds current player's cards to hand
				for (Card card : this.currentTable.getCurrentPlayer().getHand()) {
					this.contentPane.addCardToHand(card);
				}

				// update text area
				this.contentPane.updateTextArea(this.currentTable.printData());

				this.contentPane.updateUI();

				// shut down new game window
				this.newGame.dispose();
				this.newGame.setVisible(false);

				// show message
				JOptionPane.showMessageDialog(this.contentPane,
						"Seuraavaksi vuorossa "
								+ this.currentTable.getCurrentPlayer()
										.getName());

				this.newGame = null;
				this.windowOpened = false;

				// if cpu player(s) starts game
				while (!this.currentTable.getCurrentPlayer().isHuman()
						&& this.currentTable.checkWinner() == null) {

					String action = this.currentTable.getCurrentPlayer()
							.playTurn();

					// update UI

					if (this.contentPane.isNormal()) {
						updateTableCardsButtonsduringBot();
					}

					// remove previous cards
					this.contentPane.getPlayerPanel().removeAll();

					// add new currentPlayer's cards
					for (Card card : this.currentTable.getCurrentPlayer()
							.getHand()) {
						this.contentPane.addCardToHand(card);
					}

					// check which representation should be used
					this.contentPane.checkRepresentation();

					// update text area
					this.contentPane.updateTextArea(this.currentTable
							.printData()
							+ action);

					// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
					if (!this.contentPane.isNormal()) {
						updateTableButtonsduringBot();
					}

					// update UI
					this.contentPane.updateUI();

					// player ends turn
					this.currentTable.endTurn();

					// if round has ended after cpu player
					if (this.currentTable.roundEnded()) {

						// remove rest cards from table
						this.contentPane.prepareNewRound();

						// calculate points and check winner
						this.currentTable.calculatePoints();
						this.contentPane
								.updateTextArea(this.currentTable.printData()
										+ "\n\n Kierros päättyi ja viimeiset kortit \npöydästä sai "
										+ this.currentTable.getLastTaker()
												.getName());

						this.contentPane.updateUI();

						Player player = this.currentTable.checkWinner();

						// if we have winner
						if (player != null) {
							JOptionPane.showMessageDialog(this.contentPane,
									player.getName() + " voitti pelin!");
							this.contentPane.getTablePanel().removeAll();
							this.contentPane.getPlayerPanel().removeAll();
							this.contentPane.getRightPanel().removeAll();

						} else { // not winner yet

							// prepare next round
							this.currentTable.playNewRound();

							// play next round
							JOptionPane.showMessageDialog(this.contentPane,
									"Siirrytään seuraavalle kierrokselle.");
						}
					} else { // round continues

						// update for next player

						// remove previous if not yet removed
						this.contentPane.getTablePanel().removeAll();
						this.contentPane.getCardsOnTable().clear();

						// adds pack to table if possible
						if (!this.currentTable.getPack().isEmpty())
							this.contentPane.addCardToTable(this.contentPane
									.getPack());

						// adds cards to table
						for (Card card : this.currentTable.getCardsOnTable()) {
							this.contentPane.addCardToTable(new CardButton(
									this, card));
						}

						// remove previous cards
						this.contentPane.getPlayerPanel().removeAll();

						// adds current player's cards to hand
						for (Card card : this.currentTable.getCurrentPlayer()
								.getHand()) {
							this.contentPane.addCardToHand(card);
						}

						// check which representation should be used
						this.contentPane.checkRepresentation();

						// update text area
						this.contentPane.updateTextArea(this.currentTable
								.printData()
								+ action);

						// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
						if (!this.contentPane.isNormal()) {
							updateTableButtonsduringBot();
						}

						// play next round
						JOptionPane.showMessageDialog(this.contentPane,
								"Seuraavaksi vuorossa "
										+ this.currentTable.getCurrentPlayer()
												.getName());

						// update
						this.contentPane.updateUI();
					}

				}

				// if winner found, game ended
				if (this.currentTable.checkWinner() != null)
					this.currentTable = null;

			} else {

				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(this.newGame,
						"Oltava vähintään 2 pelaajaa pöydässä!");
			}

		}

		if (command.equals("Kasino-ohje")) {
			if (!this.windowOpened) {
				this.infoWindow = new InfoWindow("Casino-info", this);
				this.infoWindow.addWindowListener(this);
				this.infoWindow.setVisible(true);
				this.infoWindow.setAlwaysOnTop(true);
				this.windowOpened = true;

			} else
				Toolkit.getDefaultToolkit().beep();

		}

		if (command.equals("Saveta")) {

			String fileName = this.saveWindow.getSaveField().getText();

			if (this.currentTable != null && fileName.length() >= 5
					&& fileName.endsWith(".cas")) {

				// save current game to given file
				try {
					new CasinoIO().saveGame(fileName, this.currentTable);
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(this.saveWindow, e1
							.getMessage());
				}

				// close window after saving
				this.saveWindow.setVisible(false);
				this.saveWindow.dispose();
				this.saveWindow = null;
				this.windowOpened = false;

			} else {

				if (fileName.length() <= 4 || !fileName.endsWith(".cas")) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane
							.showMessageDialog(this.saveWindow,
									"Viallinen tiedostonimi, liian lyhyt nimi tai puuttuva .cas-pääte!");
				}
				if (this.currentTable == null) {
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(this.saveWindow,
							"Ei ole peliä, jota tallentaa!");
				}
			}
		}

		if (command.equals("Pelin lataus")) {

			// if right file selected
			if (!this.loadWindow.getComboBox().getSelectedItem().equals(
					"Valitse tiedosto")) {
				try {
					this.currentTable = new CasinoIO()
							.loadGame((String) this.loadWindow.getComboBox()
									.getSelectedItem());

				} catch (CorruptedCasinoFileException e1) {

					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(this, e1.getMessage());
					this.currentTable = null;
				}

				// after load update UI and close window

				this.loadWindow.setVisible(false);
				this.loadWindow.dispose();
				this.loadWindow = null;
				this.windowOpened = false;

				if (this.currentTable != null) {

					// update table for loaded game
					this.contentPane.getTablePanel().removeAll();
					this.contentPane.getPlayerPanel().removeAll();
					this.contentPane.getRightPanel().removeAll();
					this.contentPane.startSetUp();

					// update table cards
					for (Card card : this.currentTable.getCardsOnTable()) {
						this.contentPane.addCardToTable(new CardButton(this,
								card));
					}

					// adds current player's cards to hand
					for (Card card : this.currentTable.getCurrentPlayer()
							.getHand()) {
						this.contentPane.addCardToHand(card);
					}

					// set selected cards as selected

					if (!this.currentTable.getSelectedCards().isEmpty()) {

						// set every card button selected corresponding cards
						for (Card card : this.currentTable.getSelectedCards()) {

							for (CardButton button : this.contentPane
									.getCardsOnTable()) {

								if (button.getCard().equals(card)) {

									button.setSelected(true);
									button.setOpacity(0.3f);
									break;
								}
							}
						}
					}

					// set play card as it was
					if (this.currentTable.getCurrentPlayer().getCardToPlay() != null) {

						for (Component button : this.contentPane
								.getPlayerPanel().getComponents()) {

							CardButton b = (CardButton) button;

							if (b.getCard().equals(
									this.currentTable.getCurrentPlayer()
											.getCardToPlay())) {

								this.alreadySelected = true;
								this.selectedPlayButton = b;
								b.setSelected(true);
								b.setOpacity(0.3f);
							}
						}
					} else { // play card not selected
						this.alreadySelected = false;
					}

					// check which representation should be used
					this.contentPane.checkRepresentation();

					// update text area
					this.contentPane.updateTextArea(this.currentTable
							.printData());

					this.contentPane.updateUI();
				}

			}
		}

		if (command.equals("infoChanged")) {

			JComboBox cb = (JComboBox) e.getSource();
			String item = (String) cb.getSelectedItem();

			// update text area if not Valitse ohje
			if (!item.equals("Valitse ohje"))
				this.infoWindow.updateTextArea(item);
		}

		if (command.equals("Lisää")) {

			// max 10 signs on names
			if (!this.newGame.getTextField().getText().equals("")
					&& this.newGame.getTextField().getText().length() <= 10) {
				Table game;

				if (this.temp.isEmpty()) {
					game = new Table();
					this.temp.add(game);
				}
				game = this.temp.getLast();
				Player player = this.newGame.getRadioButton().isSelected() ? new NonUser1(
						this.newGame.getTextField().getText(), game)
						: new Human(this.newGame.getTextField().getText());

				// if add didn't succeed, create new table
				if (!game.addPlayer(player)) {

					game = new Table();
					this.temp.add(game);
					player = this.newGame.getRadioButton().isSelected() ? new NonUser1(
							this.newGame.getTextField().getText(), game)
							: new Human(this.newGame.getTextField().getText());
					game.addPlayer(player);
				}
				this.newGame.getTextField().setText("");
				this.newGame.getTextArea().setEditable(true);

				if (this.temp.size() == 1)
					this.newGame.getTextArea().setText(
							this.temp.getFirst().getPlayerCount() + ". "
									+ player.getName());

				this.newGame.getTextArea().setEditable(false);
			} else {
				Toolkit.getDefaultToolkit().beep();

				if (this.newGame.getTextField().getText().equals("")) {
					JOptionPane.showMessageDialog(this.newGame,
							"Pelaajalla täytyy olla nimi!");

				}
				if (this.newGame.getTextField().getText().length() > 10) {
					JOptionPane.showMessageDialog(this.newGame,
							"Liian pitkä nimi, enintään 10 merkkiä!");
				}

			}
		}

		if (command.equals("Lataa")) {
			if (!this.windowOpened) {
				this.loadWindow = new LoadFileWindow("Lataa peli", this);
				this.loadWindow.addWindowListener(this);
				this.loadWindow.setVisible(true);
				this.loadWindow.setAlwaysOnTop(true);
				this.windowOpened = true;
			} else
				Toolkit.getDefaultToolkit().beep();

		}

		if (command.equals("Lopeta")) {
			setVisible(false);
			dispose();
			System.exit(0);
		}

		if (command.equals("Left")) {

			// remove old button
			this.contentPane.getTablePanel().removeAll();

			if (!this.currentTable.getPack().isEmpty())
				this.contentPane.getTablePanel()
						.add(this.contentPane.getPack());

			this.contentPane.getTablePanel().add(
					this.contentPane.getLeftButton());
			this.contentPane.getTablePanel().add(
					this.contentPane.getRightButton());

			// set previous button as current
			this.contentPane.setPreviousAsCurrentButton();

			// add new current button
			this.contentPane.getTablePanel().add(
					this.contentPane.getCurrentButton());

			this.contentPane.updateUI();
		}

		if (command.equals("Right")) {

			// remove old button
			this.contentPane.getTablePanel().removeAll();

			if (!this.currentTable.getPack().isEmpty())
				this.contentPane.getTablePanel()
						.add(this.contentPane.getPack());

			this.contentPane.getTablePanel().add(
					this.contentPane.getLeftButton());
			this.contentPane.getTablePanel().add(
					this.contentPane.getRightButton());

			// set next button as current
			this.contentPane.setNextAsCurrentButton();

			// add new current button
			this.contentPane.getTablePanel().add(
					this.contentPane.getCurrentButton());

			this.contentPane.updateUI();
		}

		if (command.equals("Confirm")) {

			boolean playerGained = false;
			LinkedList<Card> selected = new LinkedList<Card>();

			if (!this.currentTable.getSelectedCards().isEmpty()) {

				// if selected cards is not empty, player tried to gain cards
				playerGained = true;

				// copy list
				for (Card card : this.currentTable.getSelectedCards()) {
					selected.add(card);
				}
			}

			// if confirm succeeded
			if (this.currentTable.confirm()) {

				// create new text to text area
				StringBuilder sb = new StringBuilder(this.currentTable
						.printData());

				// if pack is empty
				if (this.currentTable.getPack().isEmpty()) {
					this.contentPane.emptyPack();
				}

				// player set card on table
				if (!playerGained) {

					sb.append("\n");
					sb.append(this.currentTable.getCurrentPlayer().getName());
					sb.append(" asetti pöydälle kortin\n");
					sb.append(this.selectedPlayButton.getCard().toString());

					// add card to table and remove it from hand
					this.contentPane.addCardToTable(this.selectedPlayButton);

					// fix card not selected too
					this.selectedPlayButton.setSelected(false);
					this.selectedPlayButton.setOpacity(1f);
					this.contentPane.getPlayerPanel().remove(
							this.selectedPlayButton);

				} else { // player gained cards

					// remove all selected cards from table
					for (Card card : selected) {
						this.contentPane.removeCardFromTable(card);
					}

					// update text

					sb.append("\n");
					sb.append(this.currentTable.getCurrentPlayer().getName());
					sb.append(" kaappasi itselleen kortit:\n");

					sb.append(this.selectedPlayButton.getCard().toString());
					sb.append(", ");

					for (int i = 0; i < selected.size(); i++) {

						sb.append(selected.get(i).toString());
						if (!selected.get(i).equals(selected.getLast()))
							sb.append(", ");

						if ((i + 1) % 2 == 0) {
							sb.append("\n");
						}

					}

					// if table is empty, gained full house
					if (this.currentTable.getCardsOnTable().isEmpty()) {
						sb.append("\n");
						sb.append(this.currentTable.getCurrentPlayer()
								.getName());
						sb.append(" sai mökin.");
					}

					// remove play card from hand
					this.contentPane.getPlayerPanel().remove(
							this.selectedPlayButton);
				}

				// set up every round this if not normal representation
				if (!this.contentPane.isNormal()) {
					this.contentPane.getTablePanel().remove(
							this.contentPane.getCurrentButton());
					this.contentPane.setFirstAsCurrentButton();
					this.contentPane.getTablePanel().add(
							this.contentPane.getCurrentButton());
				}

				// fix selected button non selected
				this.alreadySelected = false;
				this.selectedPlayButton = null;

				// add new drawn card to hand if pack not empty
				if (!this.currentTable.getPack().isEmpty()) {
					this.contentPane.addCardToHand(this.currentTable
							.getCurrentPlayer().getHand().getLast());

					// add text to area
					sb.append("\n");
					sb.append(this.currentTable.getCurrentPlayer().getName());
					sb.append(" nosti itselleen pakasta kortin\n");
					sb.append(this.currentTable.getCurrentPlayer().getHand()
							.getLast().toString());
				}

				// update
				this.contentPane.checkRepresentation();
				this.contentPane.updateUI();
				this.contentPane.updateTextArea(sb.toString());

				// automatically end turn
				this.currentTable.endTurn();

				// if round has ended after human player turn
				if (this.currentTable.roundEnded()) {

					// remove rest cards from table
					this.contentPane.prepareNewRound();

					// calculate points and check winner
					this.currentTable.calculatePoints();
					this.contentPane
							.updateTextArea(this.currentTable.printData()
									+ "\n\n Kierros päättyi ja viimeiset kortit \npöydästä sai "
									+ this.currentTable.getLastTaker()
											.getName());
					this.contentPane.updateUI();

					Player player = this.currentTable.checkWinner();

					// if we have winner
					if (player != null) {
						JOptionPane.showMessageDialog(this.contentPane, player
								.getName()
								+ " voitti pelin!");
						this.contentPane.getTablePanel().removeAll();
						this.contentPane.getPlayerPanel().removeAll();
						this.contentPane.getRightPanel().removeAll();
						this.currentTable = null;

					} else { // not winner yet

						// prepare next round
						this.currentTable.playNewRound();

						// adds pack to table
						this.contentPane.addCardToTable(this.contentPane
								.getPack());

						// adds starting cards to table
						for (Card card : this.currentTable.getCardsOnTable()) {
							this.contentPane.addCardToTable(new CardButton(
									this, card));
						}

						// adds current player's cards to hand
						for (Card card : this.currentTable.getCurrentPlayer()
								.getHand()) {
							this.contentPane.addCardToHand(card);
						}

						// update text area
						this.contentPane.updateTextArea(this.currentTable
								.printData());

						// play next round
						JOptionPane.showMessageDialog(this.contentPane,
								"Siirrytään seuraavalle kierrokselle.");

						// play next round
						JOptionPane.showMessageDialog(this.contentPane,
								"Seuraavaksi vuorossa "
										+ this.currentTable.getCurrentPlayer()
												.getName());

						// update
						this.contentPane.updateUI();

						// if new round begins with cpu player(s)
						while (!this.currentTable.getCurrentPlayer().isHuman()) {

							String action = this.currentTable
									.getCurrentPlayer().playTurn();

							// current player's turn ends
							this.currentTable.endTurn();

							// update UI

							// remove old
							this.contentPane.getTablePanel().removeAll();
							this.contentPane.getCardsOnTable().clear();

							// add new

							// if pack not empty, add it
							if (!this.currentTable.getPack().isEmpty()) {
								this.contentPane
										.addCardToTable(this.contentPane
												.getPack());
							}

							for (Card card : this.currentTable
									.getCardsOnTable()) {
								this.contentPane.addCardToTable(new CardButton(
										this, card));
							}

							// remove previous cards
							this.contentPane.getPlayerPanel().removeAll();

							// add new currentPlayer's cards
							for (Card card : this.currentTable
									.getCurrentPlayer().getHand()) {
								this.contentPane.addCardToHand(card);
							}

							// update text area
							this.contentPane.updateTextArea(this.currentTable
									.printData()
									+ action);

							this.contentPane.updateUI();

							JOptionPane.showMessageDialog(this.contentPane,
									"Seuvaavaksi vuorossa "
											+ this.currentTable
													.getCurrentPlayer()
													.getName());
						}
					}

				} else { // else round continues

					// prepare table for next player

					// remove previous player's cards
					this.contentPane.getPlayerPanel().removeAll();

					// add currentPlayer's cards
					for (Card card : this.currentTable.getCurrentPlayer()
							.getHand()) {
						this.contentPane.addCardToHand(card);
					}

					// show message about next turn
					JOptionPane.showMessageDialog(this.contentPane,
							"Seuraavaksi vuorossa "
									+ this.currentTable.getCurrentPlayer()
											.getName());

					// update pane
					this.contentPane.updateTextArea(this.currentTable
							.printData());
					this.contentPane.updateUI();

					// as long as next current players are CPU, play turn
					while (!this.currentTable.getCurrentPlayer().isHuman()) {

						String action = this.currentTable.getCurrentPlayer()
								.playTurn();

						// update UI

						if (this.contentPane.isNormal()) {
							updateTableCardsButtonsduringBot();
						}

						// remove previous cards
						this.contentPane.getPlayerPanel().removeAll();

						// add new currentPlayer's cards
						for (Card card : this.currentTable.getCurrentPlayer()
								.getHand()) {
							this.contentPane.addCardToHand(card);
						}

						// check which representation should be used
						this.contentPane.checkRepresentation();

						// update text area
						this.contentPane.updateTextArea(this.currentTable
								.printData()
								+ action);

						// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
						if (!this.contentPane.isNormal()) {
							updateTableButtonsduringBot();
						}

						// update UI
						this.contentPane.updateUI();

						// player ends turn
						this.currentTable.endTurn();

						// if round has ended after cpu player
						if (this.currentTable.roundEnded()) {

							// remove rest cards from table
							this.contentPane.prepareNewRound();

							// calculate points and check winner
							this.currentTable.calculatePoints();
							this.contentPane
									.updateTextArea(this.currentTable
											.printData()
											+ "\n\n Kierros päättyi ja viimeiset kortit \npöydästä sai "
											+ this.currentTable.getLastTaker()
													.getName());

							this.contentPane.updateUI();

							Player player = this.currentTable.checkWinner();

							// if we have winner
							if (player != null) {
								JOptionPane.showMessageDialog(this.contentPane,
										player.getName() + " voitti pelin!");
								this.contentPane.getTablePanel().removeAll();
								this.contentPane.getPlayerPanel().removeAll();
								this.contentPane.getRightPanel().removeAll();

								// game is not needed anymore
								this.currentTable = null;
								return;

							} else { // not winner yet

								// prepare next round
								this.currentTable.playNewRound();

								// adds pack to table
								this.contentPane
										.addCardToTable(this.contentPane
												.getPack());

								// adds starting cards to table
								for (Card card : this.currentTable
										.getCardsOnTable()) {
									this.contentPane
											.addCardToTable(new CardButton(
													this, card));
								}

								// adds current player's cards to hand
								for (Card card : this.currentTable
										.getCurrentPlayer().getHand()) {
									this.contentPane.addCardToHand(card);
								}

								// update text area
								this.contentPane
										.updateTextArea(this.currentTable
												.printData());

								// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
								if (!this.contentPane.isNormal()) {
									updateTableButtonsduringBot();
								}

								// play next round
								JOptionPane.showMessageDialog(this.contentPane,
										"Siirrytään seuraavalle kierrokselle.");

								// play next round
								JOptionPane.showMessageDialog(this.contentPane,
										"Seuraavaksi vuorossa "
												+ this.currentTable
														.getCurrentPlayer()
														.getName());

								// update
								this.contentPane.updateUI();

								// if new round begins with cpu player(s)
								while (!this.currentTable.getCurrentPlayer()
										.isHuman()) {

									String action1 = this.currentTable
											.getCurrentPlayer().playTurn();

									// current player's turn ends
									this.currentTable.endTurn();

									// update UI

									if (this.contentPane.isNormal()) {
										updateTableCardsButtonsduringBot();
									}

									// remove previous cards
									this.contentPane.getPlayerPanel()
											.removeAll();

									// add new currentPlayer's cards
									for (Card card : this.currentTable
											.getCurrentPlayer().getHand()) {
										this.contentPane.addCardToHand(card);
									}

									// update text area
									this.contentPane
											.updateTextArea(this.currentTable
													.printData()
													+ action1);

									// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
									if (!this.contentPane.isNormal()) {
										updateTableButtonsduringBot();
									}

									this.contentPane.updateUI();

									JOptionPane.showMessageDialog(
											this.contentPane,
											"Seuvaavaksi vuorossa "
													+ this.currentTable
															.getCurrentPlayer()
															.getName());
								}
							}

						} else { // round continues after cpu player

							// update UI for new player

							if (this.contentPane.isNormal()) {
								updateTableCardsButtonsduringBot();
							}

							// remove previous player's cards
							this.contentPane.getPlayerPanel().removeAll();

							// add new currentPlayer's cards
							for (Card card : this.currentTable
									.getCurrentPlayer().getHand()) {
								this.contentPane.addCardToHand(card);
							}

							// check which representation should be used
							this.contentPane.checkRepresentation();

							// update text area
							this.contentPane.updateTextArea(this.currentTable
									.printData()
									+ action);

							// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
							if (!this.contentPane.isNormal()) {
								updateTableButtonsduringBot();
							}

							// update UI
							this.contentPane.updateUI();

							JOptionPane.showMessageDialog(this.contentPane,
									"Seuraavaksi vuorossa "
											+ this.currentTable
													.getCurrentPlayer()
													.getName());
						}
					}
				}

			} else { // confirm failed
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(this.contentPane,
						"Laiton siirto, yritä uudelleen!");
			}

		}
	}

	/**
	 * This CasinoGUI is main class of the game Game starts here
	 */

	public static void main(String[] args) {
		new CasinoGUI("Kasino");
	}

	/**
	 * Creates Menus for GUI
	 */

	void createMenu(JMenuBar menubar, Color color, Font menuFont) {

		menubar.setOpaque(true);
		menubar.setBackground(color);
		// menubar.setBorder();
		menubar.setPreferredSize(new Dimension(1000, 20));

		// Create Peli- menu
		JMenu menu = new JMenu("Peli");
		menu.setFont(menuFont);
		menubar.add(menu);

		// Pelit->Uusipeli
		JMenuItem newGame = new JMenuItem("Uusi peli");
		newGame.setBackground(color);
		newGame.setFont(menuFont);
		newGame.addActionListener(this);
		menu.add(newGame);

		// Pelit->Tallenna
		JMenuItem saveGame = new JMenuItem("Tallenna");
		saveGame.setBackground(color);
		saveGame.setFont(menuFont);
		saveGame.addActionListener(this);
		menu.add(saveGame);

		// Pelit-> Lataa
		JMenuItem loadGame = new JMenuItem("Lataa");
		loadGame.setBackground(color);
		loadGame.setFont(menuFont);
		loadGame.addActionListener(this);
		menu.add(loadGame);

		// Pelit->Lopeta
		JMenuItem quitGame = new JMenuItem("Lopeta");
		quitGame.setBackground(color);
		quitGame.setFont(menuFont);
		quitGame.addActionListener(this);
		menu.add(quitGame);

		// Create Ohje-menu
		JMenu menu2 = new JMenu("Ohjeet");
		menu2.setFont(menuFont);
		menu2.addActionListener(this);
		menubar.add(menu2);

		JMenuItem help = new JMenuItem("Kasino-ohje");
		help.setBackground(color);
		help.setFont(menuFont);
		help.addActionListener(this);
		menu2.add(help);
	}

	/**
	 * This method is used to clear temp if new game window is closed and game
	 * data was added before that, also enables that more than one window is not
	 * open simultaneously
	 */

	public void windowClosing(WindowEvent e) {
		this.windowOpened = false;

		Object window = e.getSource();

		if (window.equals(this.newGame)) {
			this.temp.clear();
		}
	}

	// a few not needed method from implemented
	// window listener

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowActivated(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowClosed(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowDeactivated(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowDeiconified(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowIconified(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * WindowAdapter due to extended JFrame
	 */

	public void windowOpened(WindowEvent e) {
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * MouseAdapter due to extended JFrame
	 */

	public void mouseClicked(MouseEvent e) {
	}

	/**
	 * Listens if mouse enters specified components Used for changing card
	 * button color and opacity
	 */

	public void mouseEntered(MouseEvent e) {

		CardButton button = (CardButton) e.getSource();

		// change opacity when card button is entered
		if (!button.isSelected()) {

			button.setOpacity(0.3f);
			button.repaint();
		}

	}

	/**
	 * Listens if mouse exits specified components Used for changing card button
	 * color and opacity
	 */

	public void mouseExited(MouseEvent e) {

		CardButton button = (CardButton) e.getSource();

		// change opacity when card button is exited if not selected
		if (!button.isSelected()) {

			button.setOpacity(1f);
			button.repaint();
		}

	}

	/**
	 * This method listens mouse presses, used to select and release card
	 * buttons
	 */

	public void mousePressed(MouseEvent e) {

		CardButton button = (CardButton) e.getSource();

		// if non selected button is clicked set new color to signify selected
		// button
		if (!button.isSelected()) {

			// if player selected hand card
			if (this.currentTable.getCurrentPlayer().getHand().contains(
					button.getCard())) {

				// if play card is not yet selected
				if (!this.alreadySelected) {

					// do action
					this.currentTable.getCurrentPlayer().setCardToPlay(
							button.getCard());
					this.alreadySelected = true;
					this.selectedPlayButton = button;

					// change color
					button.setOpacity(0.3f);
					button.setSelected(true);
					button.repaint();

				} else { // hand card already selected

					// warn player
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(this.contentPane,
							"Ei voi valita yhtä pelikorttia enempää!");
				}

			} else { // else player selected card on table

				// affect of the button, adds new selected card
				this.currentTable.addSelectedCard(button.getCard());

				// change color
				button.setOpacity(0.3f);
				button.setSelected(true);
				button.repaint();
			}
		} else { // else button is selected

			// if player selected hand card
			if (this.currentTable.getCurrentPlayer().getHand().contains(
					button.getCard())) {

				// release selected card
				this.currentTable.getCurrentPlayer().setCardToPlay(null);
				this.alreadySelected = false;
				this.selectedPlayButton = null;

			} else { // card is on table

				// affect of the button, removes selected card
				this.currentTable.removeSelectedCard(button.getCard());
			}

			// change color
			button.setOpacity(1f);
			button.setSelected(false);
			button.repaint();
		}
	}

	/**
	 * Not needed in this project, implemented cause could not extend
	 * MouseAdapter due to extended JFrame
	 */

	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * This method updates table representation during after cpu player's turn,
	 * used if more than 15 card's on table
	 */

	public void updateTableButtonsduringBot() {

		this.contentPane.getTablePanel().removeAll();
		this.contentPane.getCardsOnTable().clear();

		if (!this.currentTable.getPack().isEmpty()) {
			this.contentPane.addCardToTable(this.contentPane.getPack());
			this.contentPane.getTablePanel().add(this.contentPane.getPack());
		}

		for (Card card : this.currentTable.getCardsOnTable()) {
			this.contentPane.addCardToTable(new CardButton(this, card));
		}

		this.contentPane.getTablePanel().add(this.contentPane.getLeftButton());
		this.contentPane.getTablePanel().add(this.contentPane.getRightButton());
		this.contentPane.setFirstAsCurrentButton();
		this.contentPane.getTablePanel().add(
				this.contentPane.getCurrentButton());
	}

	/**
	 * This method updates table representation during after cpu player's turn,
	 * used if more less 15 card's on table
	 */

	public void updateTableCardsButtonsduringBot() {

		// remove old cards from table
		this.contentPane.getTablePanel().removeAll();
		this.contentPane.getCardsOnTable().clear();

		// add new cards to table

		// if pack not empty, add it
		if (!this.currentTable.getPack().isEmpty()) {
			this.contentPane.addCardToTable(this.contentPane.getPack());
		}

		for (Card card : this.currentTable.getCardsOnTable()) {
			this.contentPane.addCardToTable(new CardButton(this, card));
		}

	}
}
