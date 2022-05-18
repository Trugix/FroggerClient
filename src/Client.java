import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean first = true;

    private FroggerCtrl ctrl;
    private FroggerModel serverModel = new FroggerModel(0); //crea un'istanza di model che verrà  usata per la finestra del server

    public PnlFrog getServerView() {
        return serverView;
    }

    private PnlFrog serverView;
    private boolean stop = false;


    public Client(FroggerCtrl ctrl) throws IOException {
        this.ctrl = ctrl;
    }

    /**
     * Thread che ascolta per update da parte client
     */
    Thread ricezione = new Thread(new Runnable() {
        @Override
        public void run() {
            while(!stop) {
                try {
                    Transfer statoServer = (Transfer) in.readObject();  //cast dell'input come Transfer
                    serverModel.transferToModel(statoServer); //chiamata che passa i dati di transfer al model usato per disegnare la schermata del 2ndo giocatore
                    serverModel.setPunteggioAvversario(ctrl.getModel().getPoints());
                    if (first) { //nella prima iterazione crea il nuovo panel e la nuova finestra, inizializzandola allo stato GAME
                        first=false;
                        serverView = new PnlFrog(serverModel);
                        serverView.setState(PnlFrog.STATE.GAME);
                        newWindow();
                    }
                    serverView.setEntities(serverModel.getEntities());
                    serverView.repaint();
                    if (serverModel.getFrog().getVite()<=0 || serverModel.getDestinazioni()==5) {//se l'avversario finisce le vite ooccupa tutte le destinazioni il suo panel passa a GAME_OVER e il suo punteggio viene salvato
                        serverView.setState(PnlFrog.STATE.GAME_OVER);
                        ctrl.getModel().setPunteggioAvversario(serverModel.getPoints()); //aggiorna la variabile usata per calcolare chi ha vinto alla fine del gioco
                        stop = true;
                    }
                    if(ctrl.getFrogView().getState() == PnlFrog.STATE.GAME_OVER && (serverView.getState() == PnlFrog.STATE.GAME_OVER)){ //se entrambi i giocatori sono a GAME_OVER allora si passa alla schermata finale
                        ctrl.getFrogView().setState(PnlFrog.STATE.GAME_OVER_MULTI);
                        ctrl.getFrogView().repaint();
                    }
                }
                catch (Exception e) {
                    System.out.println("ERRORE NELLA COMUNICAZIONE CON IL CLIENT");
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }
    });

    /**
     * Metodo che inizializza la connessione con il client
     */
    public void connessione() {
        try {
            Scanner scanner= new Scanner(System.in);
            System.out.println("Inserisci i dati di connessione del server!\nScrivi l'IP e premi INVIO:");
            String ip = scanner.next();
            System.out.println("Scrivi la porta e premi INVIO:");
            int porta = Integer.parseInt(scanner.next());
            System.out.println("Provo a connettermi al server...");
            Socket mioSocket = new Socket(ip, porta);
            System.out.println("Connesso");
            InputStream inputStream = mioSocket.getInputStream();
            OutputStream outputStream = mioSocket.getOutputStream();
            in = new ObjectInputStream(inputStream);
            out = new ObjectOutputStream(outputStream);
            ricezione.start();
        } catch (UnknownHostException e) {
            System.err.println("Host sconosciuto");
            System.out.println(e);
        } catch (Exception e) {
            System.err.println("Impossibile stabilire la connessione");
            System.out.println(e);
        }
    }

    /**
     * crea una nuova finestra con la vista del client
     */
    public void newWindow()
    {
        JFrame serverFrame = new JFrame();
        serverFrame.setBounds(750, 0, 493, 750);
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        serverFrame.setTitle("Avversario");
        serverFrame.add(serverView);
        serverFrame.setVisible(true);
        serverFrame.setFocusable(false);
        serverFrame.setIconImage(FroggerModel.spritesFrog[2]);
    }

    /**
     * manda in output i dati necessari a disegnare la finestra del server sul lato client
     */
    public void send() {
        Transfer statoCorrente = ctrl.modelToTransfer(ctrl.getModel()); //transforma il model attuale in un trasfer da scriver in output
        try {
            out.writeObject(statoCorrente);
            out.reset();
        } catch (IOException e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }
}