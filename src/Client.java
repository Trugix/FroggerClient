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
    private PnlFrog serverView;
    private boolean first = true;
    private JFrame serverFrame;

    private FroggerCtrl ctrl;

    public Client(FroggerCtrl ctrl) throws IOException {
        this.ctrl = ctrl;
        this.serverView=new PnlFrog(ctrl);
    }

    Thread ricezione = new Thread(new Runnable() {
        @Override
        public void run() {
            while(true) {
                try {
                    statoServer = (Transfer) in.readObject();
                    serverModel.transferToModel(statoServer);
                    if (first)
                    {
                        serverView = new PnlFrog(serverModel);
                        first = false;
                    }
                    serverView.repaint();
                } catch (Exception e) {
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
            newWindow();
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

    public void send() throws IOException {
        Transfer statoCorrente = ctrl.modelToTransfer(ctrl.model);
        out.writeObject(statoCorrente);
        out.reset();
    }
}