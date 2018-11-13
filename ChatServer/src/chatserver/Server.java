/* 

 */
package chatserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static String version = "Beta 1.0";
    private static ServerSocket srvSocket;
    private static String line;

    public static void main(String[] args) throws IOException {
        int port = -1;
        int maxConnections = 50;

        Socket[] connections = new Socket[maxConnections];
        String[] users = new String[maxConnections];

        System.out.println("Chat Server - " + version);

        //Switch case para leer los argumentos introducidos
        switch (args.length) {
            case 0:
                port = 5667;
                break;
            case 1:
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Uso: java -jar chatserver.jar [puerto]");
                    System.exit(-1);
                }
                break;
        }

        InetSocketAddress socketAddr = null;
        socketAddr = new InetSocketAddress(port);

        srvSocket = null;
        try {
            //inicializa y enlaza el socket a socketAddr
            srvSocket = new ServerSocket();

            srvSocket.bind(socketAddr);
        } catch (IOException e) {
            System.out.println("Error: imposible conectar");
            System.exit(-1);
        }

        System.out.println("[SERVER ON] Escuchando en el puerto " + port + " ...\n");

        /* Recorre "non-stop" el array circular de conexiones en busca 
         * de una posición disponible vacía para asignar el
         * nuevo socket y poner en marcha un nuevo hilo gestor */
        int idSocket = 0;
        while (srvSocket != null) {
            try {
                if (connections[idSocket] == null) {

                    // TODO: acepto Socket y lo entro en el array (conexión)
                    Socket socket = srvSocket.accept();
                    connections[idSocket] = socket;
                    // TODO: asignar nombre usuario provisional
                    users[idSocket] = "Anonimo";
                    ChatThread c = new ChatThread(idSocket, users, connections, maxConnections);
                    // TODO: visualizar información sobre la nueva conexión
                    System.out.println("CONEXION: [" + idSocket+ "] conectado");
                    // TODO: poner en marcha un nuevo hilo
                    c.start();

                }
                idSocket = (idSocket + 1) % maxConnections;
            } catch (IOException e) {
                System.out.println("Error: fallo E/S servidor");
                System.exit(-1);
            }
        }
        srvSocket.close();
    }
}
