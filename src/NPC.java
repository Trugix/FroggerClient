import java.awt.*;
import java.awt.image.BufferedImage;

public class NPC extends Entity {

	
	boolean deathTouch;
	public NPC(int x, int y, int dx, String spriteID, int dimx, int dimy, boolean deathTouch) {
		super(x, y, dx, spriteID, dimx, dimy);
		this.deathTouch=deathTouch;
		
	}

	@Override
	public void stepNext()
	{
			this.p.setX(this.p.getX() + this.dx);
			this.hitbox=new Rectangle(this.p.getX(), this.p.getY(), dimx, dimy);
	}

}
