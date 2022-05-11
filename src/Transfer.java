import java.util.ArrayList;

public class Transfer
{
    ArrayList<Entity> entities;
    int time;
    int punteggio;

    public Transfer (ArrayList<Entity> entities, int time, int punteggio)
    {
        this.entities= entities;
        this.time = time;
        this.punteggio= punteggio;
    }
}

