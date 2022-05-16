import javax.swing.Timer;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FroggerCtrl implements KeyListener, MouseListener, Serializable
{
	
	PnlFrog frogView;
	public FroggerModel model;
	private int nFrame=0;
	private final Random random = new Random();
	private int timerPrize = randTemp();
	private boolean first = true;
	private NPC npcContact;
	private boolean contact;
	
	private Prize precedente;

	private Client client;
	private boolean multiplayer = false;

	private Timer t= new Timer(33, (e) ->
	{

		try {
			nextFrame();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		if (first)
		{
			initialization();
		}
	});
	
	public FroggerCtrl(FroggerModel model) throws IOException
	{
		this.model = model;
		this.frogView = new PnlFrog(model);
		frogView.addKeyListener(this);
		frogView.addMouseListener(this);

		client = new Client(this);
	}
	
	private void initialization()
	{
		this.first = false;
		for (int j = 0; j < model.prizes.size(); j++)
		{
			Prize prize1 = model.prizes.get(j);
			if (prize1.isBonus())
			{
				prize1.stepNext(frogView.destinations);

				for (int i = 0; i < model.prizes.size(); i++)
				{
					Prize prize2 = model.prizes.get(i);
					if (prize1.hitbox.intersects(prize2.hitbox) && prize1.p.getX() != prize2.p.getX())
						precedente = prize2;
				}
				swapPrize(prize1);
			}
		}
	}
	
	
	private void nextFrame() throws IOException {
		model.tempo--;

		contact = false;

		npcContact = model.NPCs.get(0);

		if(model.frog.isMoving())
		{
			nFrame++;
			model.frog.nextSlide();
			if (nFrame>=5) {
				nFrame = 0;
				model.frog.setMoving(false);
			}
		}else {
			model.frog.rotate(model.frog.getDirection());
		}

		for (Turtle t : model.turtles)
		{
			t.immersion();
		}



		int size = model.NPCs.size();
		ExecutorService service = Executors.newFixedThreadPool(4);
		
		service.submit(() -> moveNpc(0, size / 4));
		service.submit(() -> moveNpc(size / 4, size / 2));
		service.submit(() -> moveNpc(size / 2, size * 3 / 4));
		service.submit(() -> moveNpc(size * 3 / 4, size));
		
		service.shutdown();

		try
		{
			service.awaitTermination(3, TimeUnit.MILLISECONDS);
			checkCollision(model.frog);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		if (!npcContact.deathTouch && this.contact)
		{
			model.frog.stepNext(npcContact.dx);
		}
		
		if (model.frog.getVite() <= 0)
		{
			frogView.state = PnlFrog.STATE.GAME_OVER;
			t.stop();
		}
		
		checkTime(model.frog);
		if (model.frog.p.getY() >= 1200)
			checkPrize(model.frog);
		

		updatePrize();
		
		updateSkull();
		
		frogView.setEntities(model.entities);

		if (multiplayer)
			client.send();
		frogView.repaint();
	}

	private void moveNpc(int start, int end)
	{
		for (int i = start; i < end; i++)
		{
			NPC npc = model.NPCs.get(i);
			npc.stepNext();
			if (npc.dx > 0)
			{
				if (npc.p.getX() - npc.getDimx() > 1020)
				{
					npc.p.setX(-npc.getDimx() - 20);
				}
			}
			else
			{
				if (npc.p.getX() + npc.getDimx() < -20)
				{
					npc.p.setX(1020);
				}
			}

			if (model.frog.hitbox.intersects(npc.hitbox))
			{
				this.contact = true;
				this.npcContact = npc;
			}
		}
	}


	private void updateSkull()
	{
		for (Skull s : model.skulls)
		{
			if (s.getTimeToLive() > 0)
			{
				model.entities.add(s);
			}
			else
			{
				model.entities.remove(s);
			}
			s.setTimeToLive(s.getTimeToLive() - 1);
		}
	}

	private void checkCollision(Frog frog)
	{
		if ((this.contact && this.npcContact.deathTouch) || (!this.contact && frog.p.getY() >= 701 && frog.p.getY() <= 1200))
		{
			updateMorte(frog);
		}
	}


	private void updatePrize()
	{
		timerPrize--;
		if(model.prizes.size()==0)
		{
			frogView.state= PnlFrog.STATE.GAME_OVER;
		}
		if (timerPrize <= 40)
		{
			if (timerPrize % 6 >= 3)
			{
				for (Prize p : model.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite("fly");
					}
				}
			}
			else
			{
				for (Prize p : model.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite("void");
					}
				}
			}
			if (timerPrize <= 0)
			{
				timerPrize = randTemp();

				for (int i = 0; i < model.prizes.size(); i++)
				{
					if (model.prizes.get(i).isBonus())
					{
						model.prizes.get(i).stepNext(frogView.destinations);
						swapPrize(model.prizes.get(i));
					}
				}
			}
		}
	}

	private void swapPrize(Prize bonus)
	{
		model.prizes.add(precedente);
		model.entities.add(precedente);
		for (int i = 0; i < model.prizes.size(); i++)
		{
			if (bonus.hitbox.intersects(model.prizes.get(i).hitbox) && bonus.p.getX() != model.prizes.get(i).p.getX())
			{
				precedente = model.prizes.get(i);
				model.prizes.remove(precedente);
				model.entities.remove(precedente);
			}
		}
	}

	private void updateMorte(Frog frog)
	{
		model.skulls.add(new Skull(frog.p.getX(), frog.p.getY(), 0, "skull", 0, 0));
		if (frog.p.getY() > 700 && frog.p.getY() < 1200)
		{
			Sound.soundMorteAcqua();
		}
		else
		{
			Sound.soundMorteAuto();
		}
		try
		{
			nFrame=0;
			frog.morte();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		resetTempo();
	}


	private void checkTime(Frog frog)
	{
		if (model.tempo == 110)
		{
			Sound.soundTicToc();
		}
		if (model.tempo <= 0)
			updateMorte(frog);

	}

	private void checkPrize(Frog frog)
	{

		boolean save = false;

		for (Prize p : model.prizes)
		{
			if (frog.hitbox.intersects(p.hitbox))
			{

				updatePoint(frog, p.getPoint());

				for (int i = 0; i < frogView.destinations.size(); i++)
				{
					if (distance(frog.p, frogView.destinations.get(i)) <= 100)
						frogView.destinations.remove(i);
				}

				if (p.isBonus())
				{
					resetBonus(p);
				}
				else
				{
					p.setSprite("lilyPadFrog");
					p.setHitbox(null);
					model.prizes.remove(p);
				}

				try
				{
					frog.resetPosition();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				resetTempo();

				save = true;

				break;
			}
		}

		if (!save)
			updateMorte(frog);
	}

	private void resetBonus(Prize bonus)
	{

		bonus.stepNext(frogView.destinations);

		timerPrize = randTemp();
		model.entities.add(precedente);
		precedente.setSprite("lilyPadFrog");
		precedente.setHitbox(null);

		for (int i = 0; i < model.prizes.size(); i++)
		{
			if (model.prizes.size() == 1)
			{
				model.prizes.add(precedente);
				model.entities.add(precedente);
				model.prizes.remove(bonus);
				model.entities.remove(bonus);
			}
			else if (bonus.hitbox.intersects(model.prizes.get(i).hitbox) && bonus.p.getX() != model.prizes.get(i).p.getX())
			{
				precedente = model.prizes.get(i);
				model.prizes.remove(precedente);
				model.entities.remove(precedente);
			}
		}
	}


	private int randTemp()
	{
		return random.nextInt(150) + 100;
	}


	private double distance(Entity.Position p1, Entity.Position p2)
	{
		return Math.sqrt(Math.pow((p1.getX() - p2.getX()), 2) + Math.pow((p1.getY() - p2.getY()), 2));
	}


	/**
	 * Aggiorno il punteggio della rana in base a quello che ha fatto
	 *
	 * @param frog,  La rana da aggiornare
	 * @param point, I punti base dello sprite raggiunto
	 */
	private void updatePoint(Frog frog, int point)
	{
		model.setPoints(model.getPoints() + point + 100 * frog.getVite() + 5 * model.tempo);
		Sound.soundPoint();
	}


	/**
	 * Resetta il tempo ogni volta che viene chiamato
	 */
	private void resetTempo()
	{
		model.tempo = 500; //todo mettere costanti ovunque
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		try
		{
			if (!model.frog.isMoving())
				model.moveFrog(e.getKeyCode());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (frogView.state == PnlFrog.STATE.MENU)
			if(frogView.getPlayButton().contains(e.getX()/frogView.s,e.getY()/(frogView.s)-1500))
			{
				frogView.state = PnlFrog.STATE.GAME;
				frogView.repaint();
				t.start();
			}
		if(frogView.getMultiButton().contains(e.getX()/frogView.s,e.getY()/(frogView.s)-1500))
		{
			frogView.state = PnlFrog.STATE.GAME;
			frogView.repaint(); //todo da togliere o sistemare
			multiplayer = true;
			client.connessione();
			t.start();
		}
		if (frogView.getQuitButton().contains(e.getX()/frogView.s,e.getY()/(frogView.s)-1500))
			System.exit(0);
	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	public static BufferedImage associaSprite (String spriteID)
	{
		switch (spriteID)
		{
			case "frogUp":
				return FroggerModel.spritesFrog[0];

			case "frogRight":
				return FroggerModel.spritesFrog[1];

			case "frogDown":
				return FroggerModel.spritesFrog[2];

			case "frogLeft":
				return FroggerModel.spritesFrog[3];

			case "frogMovUp":
				return FroggerModel.spritesFrogMov[0];

			case "frogMovRight":
				return FroggerModel.spritesFrogMov[1];

			case "frogMovDown":
				return FroggerModel.spritesFrogMov[2];

			case "frogMovLeft":
				return FroggerModel.spritesFrogMov[3];

			case "truck":
				return FroggerModel.spriteCarro;

			case "bulldozer":
				return FroggerModel.spriteBulldozer;

			case "autoSport":
				return FroggerModel.spriteAutoSport;

			case "police":
				return FroggerModel.spritePolice;

			case "formula1":
				return FroggerModel.spriteFormula1;

			case "formula2":
				return FroggerModel.spriteFormula2;

			case "log3":
				return FroggerModel.spriteLog3;

			case "log4":
				return FroggerModel.spriteLog4;

			case "log6":
				return FroggerModel.spriteLog6;

			case "turtle1":
				return FroggerModel.spritesTurtle[0];

			case "turtle2":
				return FroggerModel.spritesTurtle[1];

			case "turtle3":
				return FroggerModel.spritesTurtle[2];

			case "fly":
				return FroggerModel.spriteFly;

			case "lilyPad":
				return FroggerModel.spriteLilyPad;

			case "lilyPadFrog":
				return FroggerModel.spriteFrogLily;

			case "skull":
				return FroggerModel.spriteSkull;

			case "void":
			default:
				return FroggerModel.spriteVoid;

		}
	}

	public Transfer modelToTransfer (FroggerModel model)
	{
		return new Transfer(model.entities, model.tempo, model.getPoints(),model.frog.getVite());
	}

	public void startGame ()
	{
		frogView.state = PnlFrog.STATE.GAME;
	}

}