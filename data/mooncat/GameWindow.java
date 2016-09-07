package moonCatGUI;

import java.awt.Dimension;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import moonCat.Area;
import moonCat.Creature;
import moonCat.Game;
import moonCat.GameObject;
import moonCat.Item;
import moonCat.MoonCat;
import moonCat.Rekvisita;
import moonCat.Weapon;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GameWindow extends JFrame implements Runnable {

	private Screen screen;

	private Game game;

	private long nextFrame;

	private long currentTime;

	private long lastWhen;

	private MoonCat mc;

	private MoonBar bar;

	private boolean left = false, right = false, up = false, down = false;

	private boolean w = false, a = false, v = false, d = false, space = false;

	private boolean z = false, x = false, c = false;

	private Animation toUpdate;

	private Area currentArea;

	private String animType;

	private int state;

	private int x0, y0;

	private double x1, y1, oxygen, waterPerc, vx, vy;

	private boolean isGoingRight, isOnGround;

	private Rekvisita rec;

	public GameWindow(String name) {

		super(name);

		game = new Game();
		screen = new Screen(game, this);
		mc = game.getMoonCat();
		bar = new MoonBar(mc);

		JPanel contentPane = new JPanel();
		GridBagLayout b = new GridBagLayout();
		contentPane.setLayout(b);
		GridBagConstraints g = new GridBagConstraints();

		g.gridheight = 1;
		g.gridwidth = 2;
		g.gridx = 0;
		g.gridy = 0;
		contentPane.add(bar, g);

		g.gridheight = 100;
		g.gridwidth = 2;
		g.gridx = 0;
		g.gridy = 1;
		contentPane.add(screen, g);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setContentPane(contentPane);
		setVisible(true);
		pack();

		addKeyListener(new KeyAdapter() {

			/**
			 * This method is called when key is pressed
			 * 
			 * @param ke
			 *            event invoked by key
			 */

			public void keyPressed(KeyEvent ke) {

				lastWhen = ke.getWhen();

				if (state != Creature.DEAD) {

					if (state == Creature.PASSIVE) {

						mc.changeState(Creature.NORMAL);
						mc.stopAnimation();
						mc.setID(isGoingRight ? "basic" : "basic_left");
					}

					switch (ke.getKeyCode()) {

					case KeyEvent.VK_ESCAPE:

						// System.out.println(Thread.activeCount());
						mc.turnItemToWeapon();

						break;

					case KeyEvent.VK_LEFT:
						left = true;

						if (game.inventoryShown())
							mc.getInventory().changeX(-1);
						else {

							if (state == Creature.NORMAL
									|| state == Creature.PASSIVE) {

								if (!z)
									mc.changeState(Creature.WALK);
								else
									mc.changeState(Creature.RUN);

							} else if (state == Creature.DUCK)
								mc.changeState(Creature.ROAM);

							if (!currentArea.canMove(mc, x0, y0)) {

								mc.changeState(Creature.ROAM);

							}
						}

						break;

					case KeyEvent.VK_RIGHT:
						right = true;

						if (game.inventoryShown())
							mc.getInventory().changeX(1);
						else {

							if (state == Creature.NORMAL
									|| state == Creature.PASSIVE) {

								if (!z)
									mc.changeState(Creature.WALK);
								else
									mc.changeState(Creature.RUN);

							} else if (state == Creature.DUCK)
								mc.changeState(Creature.ROAM);

							if (!currentArea.canMove(mc, x0, y0)) {

								mc.changeState(Creature.ROAM);

							}
						}

						break;

					case KeyEvent.VK_UP:
						up = true;

						if (game.inventoryShown())
							mc.getInventory().changeY(-1);
						else {
							
							if (currentArea.canClimb(mc, x0, y0)
									&& !mc.hasItemInHand())
								mc.changeState(Creature.CLIMB);
							
							game.changeArea();
						}

						break;

					case KeyEvent.VK_DOWN:
						down = true;

						if (game.inventoryShown())
							mc.getInventory().changeY(1);
						else {

							if (currentArea.canClimb(mc, x1, y1) && !isOnGround
									&& !mc.hasItemInHand())
								mc.changeState(Creature.CLIMB);

							else if (state == Creature.WALK)
								mc.changeState(Creature.ROAM);
						}

						break;

					case KeyEvent.VK_W:
						w = true;

						Sounds.playSound("sounds/tick.wav");

						if (game.inventoryShown()) {
							game.showInvetory(false);

						} else {
							game.showInvetory(true);
						}

						break;

					case KeyEvent.VK_A:
						a = true;

						if (game.inventoryShown()) {

							if (mc.hasItemInHand()) {

								mc.putFromHandToInventory();

							} else {
								if (state != Creature.CLIMB)
									mc.takeFromInventory();
							}

						} else {

							if (mc.hasItemInHand())
								mc.releaseItem(currentArea);
							else {
								if (state != Creature.CLIMB)
									mc.grapItem(currentArea, true);
							}

						}

						break;

					case KeyEvent.VK_V:
						v = true;

						if (state != Creature.CLIMB)
							mc.nextItem();

						break;

					case KeyEvent.VK_D:
						d = true;

						if (mc.hasItemInHand())
							mc.releaseItem(currentArea);

						break;

					case KeyEvent.VK_SPACE:
						space = true;

						if (state != Creature.SWIM && state != Creature.DUCK
								&& state != Creature.ROAM) {

							if (state != Creature.CLIMB
									&& state != Creature.HANG)
								mc.changeState(Creature.JUMP);

							mc.setID(isGoingRight ? "jump" : "jump_left");
						}

						break;

					case KeyEvent.VK_Z:
						z = true;

						if (state == Creature.CLIMB) {

							if (!left && !right)
								mc.changeState(Creature.NORMAL);
							else
								mc.changeState(Creature.WALK);

							mc.setID(isGoingRight ? "basic" : "basic_left");
						}

						break;

					case KeyEvent.VK_X:
						x = true;

						mc.use();

						if (currentArea.canHang(mc, x1, y1))
							mc.changeState(Creature.HANG);

						break;

					case KeyEvent.VK_C:
						c = true;

						if (mc.hasItemInHand()) {

							if (!mc.hasWeapon()) {

								Item cast = mc.cast(currentArea);

								if (cast != null)
									currentArea.explode(game, cast);

							} else {

								if (mc.shoot()) {

									Item bullet;

									if (isGoingRight) {

										bullet = new Weapon(x1 + 190, y1 + 70, 0,
												"bullet", mc.getPriority() + 1);
										bullet.setVx(15);
										bullet.setVy(-1);

									} else {

										bullet = new Weapon(x1 - 80, y1 + 70, Math.PI,
												"bullet", mc.getPriority() + 1);
										bullet.setVx(-15);
										bullet.setVy(-1);

									}

									currentArea.addGameObject(bullet, null);
								}

							}
						}

						break;

					default:
						break;
					}
				}
			}

			/**
			 * This method is called when key is released
			 */

			public void keyReleased(KeyEvent ke) {

				if (state != Creature.DEAD) {

					switch (ke.getKeyCode()) {

					case KeyEvent.VK_ESCAPE:

						break;

					case KeyEvent.VK_LEFT:
						left = false;

						mc.setVx(0);
						if (mc.animationIsOn()) {
							mc.stopAnimation();
						}

						switch (state) {
						case Creature.WALK:

							if (!right)
								mc.changeState(Creature.NORMAL);

							break;

						case Creature.ROAM:

							mc.changeState(Creature.DUCK);

							break;

						case Creature.CLIMB:

							break;

						default:
							break;
						}

						break;

					case KeyEvent.VK_RIGHT:
						right = false;

						mc.setVx(0);
						if (mc.animationIsOn()) {
							mc.stopAnimation();
						}

						switch (state) {
						case Creature.WALK:

							if (!left)
								mc.changeState(Creature.NORMAL);

							break;

						case Creature.ROAM:

							mc.changeState(Creature.DUCK);

							break;

						case Creature.CLIMB:

							break;

						default:
							break;
						}

						break;

					case KeyEvent.VK_UP:
						up = false;

						if (mc.animationIsOn()) {
							mc.stopAnimation();
						}

						switch (state) {

						case Creature.NORMAL:

							mc.setID(isGoingRight ? "basic" : "basic_left");

							break;

						default:
							break;
						}

						break;

					case KeyEvent.VK_DOWN:
						down = false;

						switch (state) {

						case Creature.DUCK:

							mc.changeState(Creature.NORMAL);

							if (!currentArea.canMove(mc, x0, y0)) {
								mc.changeState(Creature.DUCK);
							} else {

								mc.setID(isGoingRight ? "basic" : "basic_left");
							}

							break;

						case Creature.ROAM:

							mc.changeState(Creature.WALK);

							if (!currentArea.canMove(mc, x0, y0)) {
								mc.changeState(Creature.ROAM);
							}

							break;

						default:

							if (mc.animationIsOn()) {
								mc.stopAnimation();

							}
							break;
						}

						break;

					case KeyEvent.VK_W:
						w = false;

						break;

					case KeyEvent.VK_A:
						a = false;

						break;

					case KeyEvent.VK_V:
						v = false;

						break;

					case KeyEvent.VK_D:
						d = false;

						if (mc.getPushItem() != null)
							if (currentArea.isOnGround(mc.getPushItem()))
								mc.getPushItem().setVx(0);

						mc.stopPush(currentArea);

						mc.stopAnimation();
						mc.setID(isGoingRight ? "basic" : "basic_left");

						break;

					case KeyEvent.VK_SPACE:
						space = false;

						break;

					case KeyEvent.VK_Z:
						z = false;

						break;

					case KeyEvent.VK_X:
						x = false;

						break;

					case KeyEvent.VK_C:
						c = false;

						break;

					default:
						break;
					}
				}
			}

		});
	}

	/**
	 * This method works as game runner
	 */

	public void run() {

		nextFrame = System.currentTimeMillis();
		lastWhen = System.currentTimeMillis();
		//Sounds.playMP3Sound("sounds/Heat.mp3"); // Saundi, ei vielä toimi,
		// toimiihan! ;)

		// game loop
		while (true) {

			currentTime = System.currentTimeMillis();

			// wait for next frame
			if (nextFrame > currentTime) {
				try {

					Thread.sleep(nextFrame - currentTime);
				} catch (InterruptedException e) {
				}
			}

			// poista turhat animaatiot
			game.removeFinishedAnimations();

			// calculate physics
			while (System.currentTimeMillis() >= nextFrame) {

				// päivitä kissan tila
				this.currentArea = game.getCurrentArea();
				this.state = mc.getState();
				this.isGoingRight = mc.isGoingRight();
				this.x0 = mc.igetX();
				this.y0 = mc.igetY();
				this.x1 = mc.getX();
				this.y1 = mc.getY();
				this.isOnGround = currentArea.isOnGround(mc);
				this.oxygen = mc.getOxygen();
				this.waterPerc = currentArea.getWaterPercentage(mc);
				this.vx = mc.getVx();
				this.vy = mc.getVy();

				if (mc.getAnimation() != null)
					this.animType = mc.getAnimation().getType();

				switch (state) {

				case Creature.NORMAL:

					// tarkista passiivisuus
					checkPassiveState();

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					this.currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.CLIMB:

					// tarkista passiivisuus
					checkPassiveState();

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.HANG:

					// tarkista passiivisuus
					checkPassiveState();

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.DEAD:

					if (waterPerc > 90.0 && waterPerc < 100.0 && vy > 2)
						game.addFocusedAnimation(x0, y0 - 50, "splash", true);

					if (vx != 0 && isOnGround)
						mc.setVx(0);

					if (waterPerc > 60.0) {

						// liikuta vedessä, jossa vedenvastus ja noste
						currentArea.moveObject(mc, 1, -0.05 * vx, -0.00065
								* waterPerc - 0.05 * vy);

					} else
						currentArea.moveObject(mc, 1, 0, 0);

					rec.move(0, -2);

					break;

				case Creature.DUCK:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					if (isOnGround)
						mc.setVx(0);

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
							this.rec = new Rekvisita(x0, y0, 0, "effect", 5);
							this.rec.playAnimation(new Animation(rec, "angel"));
							currentArea.addGameObject(rec, null);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.PASSIVE:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.ROAM:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
							game.addFocusedAnimation(x0, y0, "angel", true);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.RUN:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.SWIM:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					// tarkista happitilanne
					if (oxygen == 0) {
						mc.die(currentArea);
						this.rec = new Rekvisita(x0, y0, 0, "effect", 5);
						this.rec.playAnimation(new Animation(rec, "angel"));
						currentArea.addGameObject(rec, null);
					}

					mc.addOxygen(-0.05);

					// liikuta vedessä, jossa vedenvastus ja noste
					currentArea.moveObject(mc, 1, -0.05 * vx, -0.00065
							* waterPerc - 0.05 * vy);

					break;

				case Creature.WALK:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				case Creature.PUSH:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					if (mc.getPushItem() != null) {
						currentArea.moveObject(mc.getPushItem(), 1, 0, 0);
					}

					break;

				case Creature.JUMP:

					if (vy > 0 && isOnGround) {
						mc.setID(isGoingRight ? "basic" : "basic_left");
						mc.changeState(Creature.NORMAL);
					}

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;

				default:

					// jos on vedessä aseta uimaan
					checkWaterState();

					// tarkista kontrollit
					checkControls();

					currentArea.moveObject(mc, 1, 0, 0);

					// tarkista happitilanne
					if (oxygen < mc.getMaxOxygen()) {

						// tarkista happitilanne
						if (oxygen == 0) {
							mc.die(currentArea);
						}

						if (waterPerc < 60.0) {
							mc.addOxygen(0.1);

						}
					}

					break;
				}

				// liikuta muitaXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
				for (GameObject o : currentArea.getCreatures()) {
					currentArea.moveObject(o, 1, 0, 0);
				}

				for (GameObject o : currentArea.getItems()) {

					if (o.getVx() != 0 || o.getVy() != 0
							|| !currentArea.isOnGround(o)) {

						currentArea.moveObject(o, 1, 0, 0);

						double waterPercentage = currentArea
								.getWaterPercentage(o);

						if (waterPercentage > 80.0 && waterPercentage < 95.0
								&& o.getVy() > 2) {
							game.addFocusedAnimation(o.igetX() - 50,
									o.igetY() - 100, "splash", true);
						}
					}

					if (currentArea.isOnGround(o)) {
						o.setVx(0);
						o.stopSpinning();
					}

				}

				// päivitä animaatiot

				// päivitä taka-alalla olevat
				for (GameObject ob : currentArea.getBackObjects()) {

					toUpdate = ob.getAnimation();

					if (ob.animationIsOn()
							&& toUpdate.getNextFrameTime() <= nextFrame)
						toUpdate.changePic();
				}

				// päivitä etualalla olevat
				for (GameObject ob : currentArea.getFrontObjects()) {

					toUpdate = ob.getAnimation();

					if (ob.animationIsOn()
							&& toUpdate.getNextFrameTime() <= nextFrame)
						toUpdate.changePic();
				}

				// päivitä olennot
				for (GameObject ob : currentArea.getCreatures()) {

					toUpdate = ob.getAnimation();

					if (ob.animationIsOn()
							&& toUpdate.getNextFrameTime() <= nextFrame)
						toUpdate.changePic();
				}

				// päivitä mooncat

				toUpdate = mc.getAnimation();

				if (mc.animationIsOn()
						&& toUpdate.getNextFrameTime() <= nextFrame)
					toUpdate.changePic();

				// päivitä efektit

				for (GameObject effect : game.getBackFocusedAnimations()) {

					toUpdate = effect.getAnimation();

					if (toUpdate != null
							&& toUpdate.getNextFrameTime() <= nextFrame)
						toUpdate.changePic();
				}

				for (GameObject effect : game.getFocusedAnimations()) {

					toUpdate = effect.getAnimation();

					if (toUpdate != null
							&& toUpdate.getNextFrameTime() <= nextFrame)
						toUpdate.changePic();

				}

				nextFrame += 10; // ms

			}

			waitForPaint();
			
			// finally update screen
			if (screen.finished){
				screen.repaint();
			}

			if (bar.finished)
				bar.repaint();
		}
	}

	public void checkControls() {

		if (!game.inventoryShown()) {

			if (up) {

				if (!down) {

					switch (state) {

					case Creature.CLIMB:

						mc.setVx(0);
						mc.setVy(-mc.getClimbSpeed());
						currentArea.climb(mc, 1);

						if (!mc.animationIsOn()) {
							mc.playAnimation(new Animation(mc, "climp"));
						} else {

							if (!animType.equals("climp")) {

								// sen sijaan että katkaistaisiin edellinen
								// animaatio ja
								// aloitettaisiin
								// uusi, asetetaankin animaatioon uusi looppi ja
								// päivitetään tyyppi
								mc.setID("climp");
							}

						}

						break;

					case Creature.NORMAL:

						if (!mc.hasItemInHand()) {

							mc.grapItem(currentArea, false);
							mc.setID("back");
						}

						break;

					case Creature.PASSIVE:
						mc.changeState(Creature.NORMAL);

						break;

					case Creature.SWIM:

						if (currentArea.isWater(x0 + 30, y0 + 55))
							mc.setVy(-mc.getSwimSpeed());

						if (isGoingRight) {

							mc.playAni(oxygen > 20 ? "swim" : "swim_lack");

						} else {

							mc.playAni(oxygen > 20 ? "swim_left"
									: "swim_lack_left");
						}

						break;

					case Creature.HANG:

						mc.setVx(0);
						mc.setVy(-mc.getClimbSpeed());
						currentArea.hang(mc, 1);

						break;

					default:
						break;
					}
				}
			}

			if (down) {

				if (!up) {

					switch (state) {

					case Creature.NORMAL:

						if (isGoingRight) {

							if (mc.animationIsOn())
								mc.setID("duck");
							else
								mc.playAnimation(new Animation(mc, "duck"));

						} else {

							if (mc.animationIsOn())
								mc.setID("duck_left");
							else
								mc
										.playAnimation(new Animation(mc,
												"duck_left"));
						}

						mc.changeState(Creature.DUCK);

						break;

					case Creature.JUMP:

						if (isGoingRight) {

							if (mc.animationIsOn())
								mc.setID("duck");
							else
								mc.playAnimation(new Animation(mc, "duck"));

						} else {

							if (mc.animationIsOn())
								mc.setID("duck_left");
							else
								mc
										.playAnimation(new Animation(mc,
												"duck_left"));
						}

						mc.changeState(Creature.DUCK);

						break;

					case Creature.CLIMB:

						mc.setVx(0);
						mc.setVy(mc.getClimbSpeed());
						currentArea.climb(mc, 1);

						if (!mc.animationIsOn()) {
							mc.playAnimation(new Animation(mc, "climp"));
						} else {
							// on jokin animaatio

							if (!animType.equals("climp")) {

								// sen sijaan että katkaistaisiin edellinen
								// animaatio ja
								// aloitettaisiin
								// uusi, asetetaankin animaatioon uusi looppi ja
								// päivitetään tyyppi
								mc.setID("climp");
							}
						}

						break;

					case Creature.ROAM:

						if (isGoingRight) {

							if (!animType.equals("roam_throw"))

								mc.playAni(mc.hasItemInHand() ? "roam_hold"
										: "roam");

						} else {

							if (!animType.equals("roam_throw_left"))

								mc
										.playAni(mc.hasItemInHand() ? "roam_hold_left"
												: "roam_left");

						}

						break;

					case Creature.SWIM:

						mc.setVy(mc.getSwimSpeed());

						if (isGoingRight) {

							mc.playAni(oxygen > 20 ? "swim" : "swim_lack");

						} else {

							mc.playAni(oxygen > 20 ? "swim_left"
									: "swim_lack_left");
						}

						break;

					case Creature.HANG:

						mc.setVy(mc.getClimbSpeed());
						currentArea.hang(mc, 1);

						break;

					default:
						break;
					}

				}
			}

			if (right) {

				if (!left) {

					mc.goRight(true);

					switch (state) {

					case Creature.WALK:

						mc.setVx(mc.getWalkSpeed());

						if (!mc.animationIsOn()) {

							mc.playAnimation(new Animation(mc, "walk_right"));
						} else {
							// muutoin on jokin animaatio
							if (!animType.equals("walk_right")
									&& !animType.equals("walk_throw"))
								mc.setID("walk_right");
						}

						if (z)
							mc.changeState(Creature.RUN);

						break;

					case Creature.JUMP:

						if (vx <= mc.getWalkSpeed())
							mc.setVx(mc.getWalkSpeed());

						if (mc.getID().equals("mooncat_jump_left"))
							mc.setID("jump");

						break;

					case Creature.CLIMB:

						mc.setVx(mc.getClimbSpeed());
						mc.setVy(0);
						currentArea.climb(mc, 1);

						break;

					case Creature.ROAM:

						if (isOnGround)
							mc.setVx(mc.getRoamSpeed());

						if (!mc.animationIsOn()) {
							mc.playAnimation(new Animation(mc, mc
									.hasItemInHand() ? "roam_hold" : "roam"));
						}

						if (!down) {

							mc.changeState(Creature.WALK);
							if (!currentArea.canMove(mc, x0, y0))
								mc.changeState(Creature.ROAM);
						}

						break;

					case Creature.NORMAL:

						mc.changeState(Creature.WALK);

						break;

					case Creature.DUCK:

						if (!down) {
							if (!mc.hasItemInHand())
								mc.changeState(Creature.ROAM);

						}

						break;

					case Creature.RUN:

						if (isOnGround)
							mc.setVx(vx + 0.04);

						if (vx > 4)
							mc.setVx(4);

						if (!mc.animationIsOn()) {

							mc.playAnimation(new Animation(mc, "walk_right",
									130));
						} else {
							// muutoin on jokin animaatio
							if (!animType.equals("walk_right")
									&& !animType.equals("walk_throw")) {
								mc.setID("walk_right");
								mc.getAnimation().changeSpeedTo(130);
							}
						}

						if (!z) {

							mc.changeState(Creature.WALK);
							mc.getAnimation().changeSpeedToNormal();
						}

						break;

					case Creature.SWIM:

						mc.setVx(mc.getSwimSpeed());
						mc.playAni(oxygen > 20 ? "swim" : "swim_lack");

						break;

					case Creature.PUSH:

						if (mc.getPushItem() != null) {
							if (mc.getPushItem().igetX() < x0)
								mc.stopPush(currentArea);

							else {

								mc.setVx(mc.getPushSpeed());

								mc.playAni("push");
							}
						}

						break;

					case Creature.HANG:

						mc.setVx(mc.getClimbSpeed());
						mc.setVy(0);
						currentArea.hang(mc, 1);

						break;

					default:
						break;
					}
				}
			}

			if (left) {

				if (!right) {

					mc.goRight(false);

					switch (state) {

					case Creature.WALK:

						mc.setVx(-mc.getWalkSpeed());

						if (!mc.animationIsOn()) {

							mc.playAnimation(new Animation(mc, "walk_left"));
						} else {
							// muutoin on jokin animaatio
							if (!animType.equals("walk_left")
									&& !animType.equals("walk_throw_left"))
								mc.setID("walk_left");

						}

						if (z)
							mc.changeState(Creature.RUN);

						break;

					case Creature.JUMP:

						if (vx >= -mc.getWalkSpeed())
							mc.setVx(-mc.getWalkSpeed());

						if (mc.getID().equals("mooncat_jump"))
							mc.setID("jump_left");

						break;

					case Creature.ROAM:

						if (isOnGround)
							mc.setVx(-mc.getRoamSpeed());

						if (!mc.animationIsOn()) {
							mc.playAnimation(new Animation(mc, mc
									.hasItemInHand() ? "roam_hold_left"
									: "roam_left"));
						}

						if (!down) {

							mc.changeState(Creature.WALK);
							if (!currentArea.canMove(mc, x0, y0))
								mc.changeState(Creature.ROAM);
						}

						break;

					case Creature.CLIMB:

						mc.setVx(-mc.getClimbSpeed());
						mc.setVy(0);
						currentArea.climb(mc, 1);

						break;

					case Creature.NORMAL:

						mc.changeState(Creature.WALK);

						break;

					case Creature.DUCK:

						if (!down) {
							mc.changeState(Creature.ROAM);
						}

						break;

					case Creature.RUN:

						if (isOnGround)
							mc.setVx(vx - 0.04);

						if (vx < -4)
							mc.setVx(-4);

						if (!mc.animationIsOn()) {
							mc
									.playAnimation(new Animation(mc,
											"walk_left", 130));

						} else {
							// muutoin on jokin animaatio
							if (!animType.equals("walk_left")
									&& !animType.equals("walk_throw_left")) {

								mc.setID("walk_left");
								mc.getAnimation().changeSpeedTo(130);
							}
						}

						if (!z) {

							mc.changeState(Creature.WALK);
							mc.getAnimation().changeSpeedToNormal();
						}

						break;

					case Creature.SWIM:

						mc.setVx(-mc.getSwimSpeed());
						mc
								.playAni(oxygen > 20 ? "swim_left"
										: "swim_lack_left");

						break;

					case Creature.PUSH:

						if (mc.getPushItem() != null) {
							if (mc.getPushItem().igetX() > x0)
								mc.stopPush(currentArea);

							else {

								mc.setVx(-mc.getPushSpeed());

								mc.playAni("push_left");
							}
						}

						break;

					case Creature.HANG:

						mc.setVx(-mc.getClimbSpeed());
						mc.setVy(0);
						currentArea.hang(mc, 1);

						break;

					default:
						break;
					}
				}
			}

			if (w) {

			}
			if (a) {

			}
			if (v) {

			}
			if (d) {

				if (state != Creature.PUSH) {

					mc.startPush(currentArea.getItemAt(mc.getPushPointX(), mc
							.getPushPointY()), currentArea);

				} else {
					if (mc.getPushItem() != null)
						mc.push(currentArea);
				}
			}

			if (space) {

				if (state == Creature.JUMP) {

					if (isOnGround) {
						mc.setVy(-mc.getJumpSpeed());
					}

				} else if (state == Creature.CLIMB || state == Creature.HANG) {
					mc.setVy(-mc.getJumpSpeed());
					mc.changeState(Creature.JUMP);
					mc.setID(isGoingRight ? "jump" : "jump_left");
				}
			}
		}

	}

	/**
	 * This method checks if moonCat is in water
	 */

	public void checkWaterState() {

		if (state != Creature.SWIM) {

			if (waterPerc > 60.0 && state != Creature.CLIMB
					&& state != Creature.DUCK && state != Creature.ROAM) {

				// play splash
				if (vy > 2)
					game.addFocusedAnimation(x0 - 20, y0 - 50, "splash", true);

				mc.changeState(Creature.SWIM);

				if (!mc.isBubbling())
					mc.startBubble(game);

				if (mc.animationIsOn()) {

					if (isGoingRight) {

						if (!animType.equals("swim"))
							mc.setID("swim");
					} else {

						if (!animType.equals("swim_left"))
							mc.setID("swim_left");
					}

				} else {
					mc.playAnimation(new Animation(mc, isGoingRight ? "swim"
							: "swim_left"));
				}
			} else if (state == Creature.DUCK || state == Creature.ROAM) {

				if (waterPerc > 90.0) {

					mc.changeState(Creature.SWIM);

					// play splash
					if (vy > 2) {

						game.addFocusedAnimation(x0 - 20, y0 - 50, "splash",
								true);
					}

					if (!mc.isBubbling())
						mc.startBubble(game);

				} else {

					if (waterPerc > 60.0) {
						mc.addOxygen(-0.05);
					}
				}
			}

		} else {

			if (state != Creature.CLIMB) {

				if (isOnGround) {

					if (!currentArea.isWater(x0 + 30, y0 + 60)) {

						if (!down) {
							mc.changeState(Creature.NORMAL);

							if (right || left)
								mc.changeState(Creature.WALK);
							else
								mc.changeState(Creature.NORMAL);
						}
					}
				}
			}
		}
	}

	public void checkPassiveState() {

		// jos yli 5 sekuntia passivinen
		if (System.currentTimeMillis() - lastWhen > 5000) {

			if (mc.hasItemInHand())
				mc.releaseItem(currentArea);

			mc.changeState(Creature.PASSIVE);
			mc.playAni("passive");

		}
	}

	public synchronized void waitForPaint() {

		while (!screen.finished) {

			try {
				this.wait();

			} catch (InterruptedException e) {
			}
		}
	}

	public synchronized void notifyMainThread() {
		this.notify();
	}

}
