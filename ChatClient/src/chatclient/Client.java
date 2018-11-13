/* 

 */
package chatclient;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

public class Client extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String version = "Beta 1.0";

    private JFrame frmChat;
    private JFrame frmLogin;
    private JTextField input;
    private JTextField input2;
    private JTextArea salida;
    private JLabel label1;
    private JButton boton;
    private JScrollPane scrollPane;
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    private static InetSocketAddress socketAddr;
    private static InputStream is;
    private static OutputStream os;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static Socket cliSocket;
    Client window;

    static String text;
    static String text1;
    static String text2;

    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws IOException {
        //Inicializamos el cliente y mostramos la ventana de login
        Client window = new Client(args);
        window.frmLogin.setVisible(true);
        //Inicializamos un nuevo Socket
        cliSocket = new Socket();

        try {

            //Conectamos el socket anterior a la ip y puerto de los args
            cliSocket.connect(socketAddr);
            //Inicializamos el inputstream y outputstream
            is = cliSocket.getInputStream();
            os = cliSocket.getOutputStream();

            //Inicializamos el array de bytes, máximo 100 bytes
            byte[] msg = new byte[100];
            String str;
            text = "";

            //Hacemos un do while infinito que romperemos a mano usando break
            do {
                //Leemos continuamente el mensaje recibido por el inputstream
                //y lo asignamos al String recibido
                is.read(msg);
                String recibido = new String(msg);
                //Si el ChatThread nos ha devuelto "NOMBREINTRODUCIDOVALIDO", significará
                //que el nombre no está en uso y podemos usarlo. En caso de ser así, ocultamos
                //la ventana de login y abrimos la de chat, y para finalizar rompemos el bucle.
                if (recibido.trim().equals("NOMBREINTRODUCIDOVALIDO")) {
                    window.frmLogin.setVisible(false);
                    window.frmChat.setVisible(true);
                    msg = new byte[100];
                    break;

                    //Si no hemos recibido lo anterior, mostramos un mensaje con lo recibido 
                    //y seguimos el bucle hasta que recibamos el String anterior.
                } else {
                    JOptionPane.showMessageDialog(null, new String(msg));
                    window.input2.setText("");
                    window.input2.requestFocusInWindow();
                    msg = new byte[100];
                }
            } while (true);

            //Una vez pasado el login, hacemos un bucle que se romperá cuando el usuario 
            //introduzca "SALIR".
            //Si no lo introduce, haremos focus en el input, leeremosel inputstream y mostraremos
            //lo leido en el TextArea "Salida". Para finalizar reinicializamos el array msg
            //Si el texto introducido equivale al comando CHANGENAME+nombre, cambiaremos el título de la ventana actual
            do {
                window.input.requestFocusInWindow();
                is.read(msg);
                str = new String(msg);
                if (str.trim().length() > 10 && str.trim().substring(0, 10).equals("CHANGENAME")) {
                    window.frmChat.setTitle("Chat: <" + str.trim().substring(10, str.trim().length()) + ">");
                    msg = new byte[100];
                } else {
                    window.salida.append(str);
                    msg = new byte[100];
                }
            } while (!text.equals("SALIR"));

            //Si hemos salido del bucle anterior, significará que el usuario ha introducido SALIR,
            //por lo tanto, cerramos el socket, el JFrame del Chat y salimos
            cliSocket.close();
            window.frmChat.dispose();
            System.exit(0);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            window.salida.setText(e.getMessage());
        }
    }

    //La clase Client leerá los argumentos introducidos, para mediante un switch
    //asignar la dirección ip y el puerto
    public Client(String[] args) {
        int port = -1;
        switch (args.length) {
            case 0:
                System.out.println("Uso: java -jar chatclient.jar ip|host [puerto]");
                System.exit(0);
                break;
            case 1:
                port = 5667;
                break;
            case 2:
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Error: debe especificarse un puerto");
                    System.exit(-1);
                }
                break;
        }

        socketAddr = null;
        try {
            socketAddr = new InetSocketAddress(InetAddress.getByName(args[0]), port);
        } catch (UnknownHostException e) {
            System.out.println("Error: host no encontrado / sintaxis incorrecta");
            System.exit(-1);
        }

        //inicialización de objetos del form
        login();
        chat();
    }

    private void chat() {
        frmChat = new JFrame();
        frmChat.setTitle("Chat Local");
        frmChat.setResizable(false);
        frmChat.setBounds(100, 100, 441, 411);
        frmChat.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmChat.getContentPane().setLayout(null);
        frmChat.setLocation(dim.width / 2 - frmChat.getSize().width / 2, dim.height / 2 - frmChat.getSize().height / 2);

        salida = new JTextArea();
        salida.setEditable(false);
        salida.setBounds(10, 11, 414, 335);

        DefaultCaret caret = (DefaultCaret) salida.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        scrollPane = new JScrollPane(salida, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBounds(10, 11, 414, 335);
        frmChat.getContentPane().add(scrollPane);
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());

        input = new JTextField();
        input.setBounds(10, 354, 414, 20);
        frmChat.getContentPane().add(input);
        input.setColumns(100);

        input.addActionListener((ActionEvent arg0) -> {
            text = input.getText().trim();
            try {
                if (!text.equals("") && text != null) {
                    os.write(text.getBytes());
                    os.flush();
                }
            } catch (IOException e) {
                salida.append("Error: no se puede enviar");
            }
            input.setText(null);
        });

    }

    //Ventana para loguearse
    private void login() {
        frmLogin = new JFrame();
        frmLogin.setTitle("Login");
        frmLogin.setResizable(false);
        frmLogin.setBounds(50, 50, 300, 300);
        frmLogin.setLocation(dim.width / 2 - frmLogin.getSize().width / 2, dim.height / 2 - frmLogin.getSize().height / 2);
        frmLogin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmLogin.getContentPane().setLayout(null);

        input2 = new JTextField();
        input2.setBounds(75, 110, 150, 30);
        frmLogin.getContentPane().add(input2);
        input2.setColumns(10);

        label1 = new JLabel();
        label1.setBounds(75, 80, 150, 30);
        frmLogin.getContentPane().add(label1);
        label1.setText("Nombre:");

        boton = new JButton();
        boton.setBounds(110, 180, 80, 30);
        frmLogin.getContentPane().add(boton);
        boton.setText("Entrar");

        input2.addActionListener((ActionEvent arg0) -> {
            text = input2.getText().trim();
            if (!text.equals("") && text != null) {
                try {
                    os.write(text.getBytes());
                    os.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Nombre incorrecto");
            }
        });

        boton.addActionListener((ActionEvent arg0) -> {
            text = input2.getText().trim();
            if (!text.equals("") && text != null) {
                try {
                    os.write(text.getBytes());
                    os.flush();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Nombre incorrecto");
            }
        });

    }
}
