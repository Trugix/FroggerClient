import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private ServerSocket server = null;
    private Socket socketClient = null;
    private int porta = 1234;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Transfer statoServer;

    private FroggerModel serverModel = new FroggerModel(0);

    public PnlFrog getServerView() {
        return serverView;
    }

    private PnlFrog serverView;
    private boolean first = true;
    private JFrame serverFrame;

    private FroggerCtrl ctrl;

    public Client(FroggerCtrl ctrl) throws IOException {
        this.ctrl = ctrl;
    }

    Thread ricezione = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    statoServer = (Transfer) in.readObject();
                    serverModel.transferToModel(statoServer);
                    ctrl.model.setPunteggioAvversario(statoServer.punteggio);
                    if (first) {
                        first = false;
                        serverView = new PnlFrog(serverModel);
                        //serverView.ctrl = ctrl;
                        serverView.state = PnlFrog.STATE.GAME;
                        newWindow();
                    }
                    serverView.setEntities(serverModel.entities);
                    serverView.repaint();
                    if (serverModel.frog.getVite()<=0)
                        serverView.state = PnlFrog.STATE.GAME_OVER;
                    if(ctrl.frogView.state== PnlFrog.STATE.GAME_OVER && serverView.state== PnlFrog.STATE.GAME_OVER)
                    {
                        ctrl.frogView.state= PnlFrog.STATE.GAME_OVER_MULTI;
                        serverView.state = PnlFrog.STATE.GAME_OVER_MULTI;
                        ctrl.frogView.repaint();
                    }
                } catch (NullPointerException e)
                {
                    System.out.println("Nulla");
                    //quando non ci sono aggiornamenti e quindi statoServer e' null
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    System.out.println("CONNESSIONE INTERROTTA");
                    System.exit(0);
                }
            }
        }
    });
    
    public void connessione() {
        try {
            Scanner scanner= new Scanner(System.in);
            System.out.println("Inserisci i dati di connessione del server!\nScrivi l'IP e premi INVIO:");
            String ip = scanner.next();
            System.out.println("Scrivi la porta e premi INVIO:");
            porta = Integer.parseInt(scanner.next());
            System.out.println("[0] - Provo a connettermi al server...");
            Socket mioSocket = new Socket(ip,porta);
            System.out.println("[1] - Connesso!");
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
    public void newWindow()
    {
        serverFrame = new JFrame();
        serverFrame.setBounds(500, 0, 656, 1000);
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);
        serverFrame.setTitle("ServerView");
        serverFrame.add(serverView);
        serverFrame.setVisible(true);
    }

    public void send() {

        Transfer statoCorrente = ctrl.modelToTransfer(ctrl.model);
        try {
            out.writeObject(statoCorrente);
            out.reset();
          //todo  System.out.println("Sent");
        } catch (IOException e) {
         System.out.println(e);
        }

    }
}