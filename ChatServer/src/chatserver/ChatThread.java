/* 

 */
package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class ChatThread extends Thread {

    private final int id;
    private final int maxConnections;
    private final Socket[] conns;
    private final String[] users;
    private String nombre;
    private Socket socket;
    private String buff = "";
    private int idUsuarioEncontrado = 0;

    // Conector del hilo
    public ChatThread(int id, String[] users, Socket[] conns, int maxConnections) {
        this.maxConnections = maxConnections;
        this.users = users;
        this.nombre = users[id];
        this.conns = conns;
        this.id = id;
    }

    @Override
    public void run() {
        socket = conns[id];
        try {
            // Incialización de InputStream y OutputStream
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // Declaramos e inicializamos en array de bytes "msg"
            byte[] msg;
            msg = new byte[100];
            //String nombre2 que recogerá el nombre introducido por el usuario
            String nombre2;
            //Hacemos un bucle infinito el cual romperemos cuando el usuario haya introducido
            //un nombre que no esté en uso
            do {
                //Leemos lo que entra por el InputStream
                is.read(msg);
                //Asignamos a nombre2 el resultado de msg pasado a String
                nombre2 = new String(msg);
                //Buscamos el nombre introducido por el usuario y comprobamos si existe
                //Si existe lo sacamos por pantalla.
                //El mensaje le saltará al cliente en MessageDialog
                if (findUser(nombre2.trim())) {
                    os.write(("El nombre introducido está en uso, elija otro").getBytes());
                    msg = new byte[100];
                } else {
                    //Si no existe, rompemos el bucle y podemos continuar
                    break;
                }
            } while (true);
            //El nombre es válido, así que se lo pasaremos al cliente en forma de texto
            //para que lo sepa y cambie de la ventana del Login a la del chat
            os.write("NOMBREINTRODUCIDOVALIDO".getBytes());

            //Asignamos el nombre introducido al atributo nombre y lo metemos en el array de nombres
            this.nombre = new String(msg);
            this.users[this.id] = this.nombre;

            //Mandamos el comando CHANGENAME+nombre. El cliente cuando lea este comando,
            //cambiara el título de la ventana para poner el nombre del usuario
            os.write(("CHANGENAME" + nombre).getBytes());

            //Damos a bienvenida al usuario
            os.write(("\nBienvenido " + nombre).getBytes());

            //Le damos la información necesaria para utilizar el chat
            os.write(("\nIntroduce SHOW para ver los comandos disponibles").getBytes());
            os.write(("\nIntroduce SALIR para desconectarte").getBytes());

            //Mediante la función resendAll informamos a todos los usuarios de la entrada al chat del nuevo usuario.
            resendAll("\n" + nombre + " ha entrado al chat");

            //Pasamos a leer y enviar los mensajes
            //Mientras el usuario del hilo actual esté conectado y no haya introducido la palabra SALIR...
            while (this.conns[this.id].isConnected() && !buff.trim().equals("SALIR") && buff.trim()!= null) {
                msg = new byte[100];

                //Leemos cualquier mensaje que haya en el flujo de entrada
                is.read(msg);
                //Y se lo asignamos al String buff
                buff = new String(msg);

                //Si buff es >= 3 y las 3 primeras letras equivalen a "msg", llamaremos a la función de enviar mensajes.
                if (buff.trim().length() >= 3 && buff.trim().substring(0, 3).equals("msg")) {
                    enviarMensaje(buff);
                    //Si buff es >= 3 y las 3 primeras letras equivalen a "ren", llamaremos a la función de cambiar el nombre.
                } else if (buff.trim().length() >= 3 && buff.trim().substring(0, 3).equals("ren")) {
                    cambiarNombre(buff);
                } else {
                    //Si lo introducido no coincide con los 2 if anteriores, pasamos a ver lo que ha introducido el usuario.
                    switch (buff.trim()) {
                        //Si equivale a "list" llamamos a la función lista()
                        case "list":
                            lista();
                            System.out.println("COMANDO: [" + id + "] ha usado list");
                            break;
                        //Si el usuario ha introducido "SHOW" le mostraremos las opciones disponibles
                        case "SHOW":
                            String help = "\nSALIR - Desconectarse del chat"
                                    + "\nSHOW - Ver ayuda"
                                    + "\nmsg - Enviar mensaje privado (msg + destinatario)"
                                    + "\nren - Cambiarse de nombre (ren + nombre)"
                                    + "\nlist - Muestra todos los usuarios conectados";
                            os.write(help.getBytes());
                            System.out.println("COMANDO: [" + id + "] ha usado SHOW");
                            break;
                        case "SALIR":
                            break;
                        case "":
                            throw new IOException();
                        //Si no ha introducido nada de lo anterior, significa que es un mensaje corriente
                        //estinado al chat, así que lo envíamos por broadcast a todos con resendAll
                        default:
                                System.out.println("[" + id + "] <" + nombre + "> " + buff);
                                resendAll("\n<" + nombre + "> " + buff);
                            break;
                    }
                }
            }
            //Si el bucle se ha roto porque se ha introducido la palabra SALIR, informamos a
            //todos por broadcast que el actual usuario ha abandonado.
            if (buff.trim().equals("SALIR")) {
                resendAll("\n" + nombre + " ha abandonado el chat.");
            }
            //Cerramos la conexión y le damos valor null al array para que quede libre
            this.conns[this.id].close();
            this.conns[this.id] = null;
            System.out.println("EVENTO: [" + id + "] se ha desconectado");

            //En caso de que nos salte una excepción, haremos lo mismo que antes 
            //para liberar espacio en los arrays.
        } catch (IOException e) {
            System.out.println("EVENTO: [" + id + "] cierre inesperado.");
            try {
                resendAll("\n" + nombre + " ha abandonado el chat.");

                //resendAll("\n" + nombre + " ha abandonado el chat.");
            } catch (IOException ex) {
            }
            this.users[this.id] = null;
            this.conns[this.id] = null;

            //Se lo decimos a todos los usuarios
            //Para finalizar, interrumpimos el hilo.
            Thread.currentThread().interrupt();

        }
    }

    // Reenvíamos el String msg a todos los clientes conectados
    //Para ello recorremos el array conns y comprobamos los que están conectados
    //A los que están conectados se les envía una copia del mensaje mediante su OutputStream
    public void resendAll(String msg2) throws IOException {
        for (int i = 0; i < this.maxConnections; i++) {
            if (this.conns[i] != null) {
                if (this.conns[i].isConnected()) {
                    OutputStream os = this.conns[i].getOutputStream();
                    os.write(msg2.getBytes());
                }
            }
        }
    }

    //Método para buscar usuarios.
    //Pasamos el nombre del usuario en un String usr
    //Recorremos el array users comprobando si el nombre introducido 
    //corresponde con el de algún valor del array
    //Devolvemos si lo encontramos o no
    private boolean findUser(String usr) {
        idUsuarioEncontrado = 0;
        boolean encontrado = false;
        int i = 0;
        while (!encontrado && i < this.maxConnections) {
            if (this.conns[i] != null) {
                if (this.conns[i].isConnected()) {
                    if (this.users[i].trim().equals(usr)) {
                        idUsuarioEncontrado = i;
                        encontrado = true;
                    }
                }
            }
            if (!encontrado) {
                i++;
            }
        }
        return encontrado;
    }

    //Método para enviar un mensaje privado
    //Recibimos un String buff el cual dividimos por palabras las cuales introduciremos
    //en un array de String.
    //Si el usuario que aparece en el "buff" existe, cogeremos el resto de palabras 8desde el destinatario hasta el final)
    //Las agruparemos en una frase dividiéndolas por espacios.
    //Enviamos esas palabras al destinatario y al emisor mediante su OutputStream
    private void enviarMensaje(String buff) throws IOException {
        String[] palabras = buff.split(" ");
        OutputStream os = socket.getOutputStream();
        if (findUser(palabras[1])) {
            String destinatario = palabras[1];
            palabras = Arrays.copyOfRange(palabras, 2, palabras.length);
            String mensaje = String.join(" ", palabras);
            os.write(("\n*" + this.nombre + " -> " + destinatario + "* " + mensaje).getBytes());
            os = conns[idUsuarioEncontrado].getOutputStream();
            os.write(("\n*" + this.nombre + "* " + mensaje).getBytes());
            os.flush();
        } else {
            os.write("\nUsuario no encontrado".getBytes());
            os.flush();
        }
    }

    //Dividimos el String introducido, para saber cual es el nombre que quiere ponerse el usuario.
    //Si el usuario no existe y si los datos introducidos son correctos, cambiamos el nombre en el hilo actual
    //y en el array users. Finalmente informamos del cambio a todos los usuarios.
    private void cambiarNombre(String buff) throws IOException {
        String[] palabras = buff.split(" ");
        OutputStream os = socket.getOutputStream();
        if (palabras[1] == null || palabras.length > 2) {
            os.write("\nEscriba ren + nombre".getBytes());
            os.flush();
        } else if (findUser(palabras[1].trim())) {
            os.write("\nEse nombre ya está en uso".getBytes());
            os.flush();
        } else {
            String nombreViejo = nombre;
            this.nombre = palabras[1];
            users[this.id] = palabras[1];
            os.write(("CHANGENAME" + nombre).trim().getBytes());
            resendAll("\n<" + nombreViejo + "< ha cambiado el nombre a <" + nombre + ">");
            System.out.println("\n<" + nombreViejo + "< ha cambiado el nombre a <" + nombre + ">");
        }

    }

    //Mediante un for miramos los usuarios conectados y mostramos por pantalla
    //El ID, nombre y la IP
    private void lista() throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write("\nLista de usuarios conectados:".getBytes());
        os.flush();
        for (int i = 0; i < this.maxConnections; i++) {
            if (this.conns[i] != null && this.conns[i].isConnected()) {
                String conectado = "\nID: " + i + " Nombre: " + this.users[i]
                        + " IP: " + this.conns[i].getRemoteSocketAddress().toString();
                os.write(conectado.getBytes());
                os.flush();
            }
        }
        System.out.println("");
    }
}
