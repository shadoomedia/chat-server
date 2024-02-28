package Server;

import utils.ConsoleColor;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ConcurrentServer server;
    private  ConsoleColor color;
    private String clientName;
    private String coloredName;

    private final Logger logger;

    public ClientHandler(Socket clientSocket, ConcurrentServer server, Logger logger) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.logger = logger;
        this.color = ConsoleColor.getRandomColor();
    }

    public ConsoleColor getColor() {
        return color;
    }

    @Override
    public void run() {
        logger.log(Level.INFO, Thread.currentThread().getName() + " ready and running");
        try {
            sendMessageSingleLine(color.ADMIN.getCode() + "Type your name here:" + ConsoleColor.DEFAULT.getCode());

            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputName;
            while (true) {
                inputName = reader.readLine();
                if (!server.checkIfNameExists(inputName)) {
                    clientName = inputName;
                    sendMessageSingleLine(ConsoleColor.ADMIN.getCode() + "Welcome " + this.getColor().getCode() + clientName + ConsoleColor.DEFAULT.getCode());
                    sendMessage(server.readLastEntriesFromJournal(10));
                    break;
                } else {
                    sendMessageSingleLine(ConsoleColor.ERROR_WARNING.getCode()+ "Enter another name -- already exists/ not allowed" + ConsoleColor.DEFAULT.getCode());
                }
            }

            logger.log(Level.INFO, clientName + " name entered for " + clientSocket.getInetAddress() + " PORT:" + getClientPort());
            coloredName = color.getCode() + clientName + ConsoleColor.DEFAULT.getCode();

            String clientMessage;
            while (!clientSocket.isClosed()) {
                try {
                    clientMessage = reader.readLine();
                    if (clientMessage == null || clientMessage.equals("/exit")) {
                        sendMessageSingleLine(ConsoleColor.ERROR_WARNING.getCode() + "Connection closed... reason: client /exit" + ConsoleColor.DEFAULT.getCode());
                        logger.log(Level.INFO, clientName + " left the server");
                        shutdown();
                        break;
                    }

                    if (clientMessage.startsWith("@")) {
                        int colonIndex = clientMessage.indexOf(':');
                        if (colonIndex != -1) {
                            String recipientNames = clientMessage.substring(1, colonIndex).trim();
                            String message = clientMessage.substring(colonIndex + 1).trim();

                            String[] recipientNameArray = recipientNames.split("\\s*,\\s*");
                            for (String recipientName : recipientNameArray) {
                                ClientHandler recipient = server.findClientHandlerByName(recipientName);
                                if (recipient != null) {
                                    server.directMessageToRecipient(message,this.getClientSimpleName(), recipient);
                                } else {
                                    sendMessageSingleLine(ConsoleColor.ERROR_WARNING.getCode() + "" + ConsoleColor.DEFAULT.getCode());
                                    logger.log(Level.INFO, "User not found: " + recipientName);
                                }
                            }
                        } else {
                            sendMessageSingleLine(ConsoleColor.ERROR_WARNING.getCode() + "Invalid whisper format. Please use: '@username/user names' + ':' + 'message'" + ConsoleColor.DEFAULT.getCode());
                        }
                        continue;
                    }

                    server.broadcastMessage(clientMessage, this);

                } catch (IOException e) {
                    break;
                }
            }
        }catch (IOException exception){
            exception.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                server.removeClient(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getClientNameColored() {
        return coloredName;
    }

    public String getClientSimpleName(){
        return clientName != null ? clientName : "unknown";
    }

    public String getClientSimpleNameIpAndPORT(){
        return  getClientSimpleName() + getClientAddress() + getClientPort();
    }

    public void sendMessage(String message) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        outputStream.write((message + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public void sendMessageSingleLine(String message) throws IOException {
        OutputStream outputStream = clientSocket.getOutputStream();
        outputStream.write((message + "\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public void sendMessageToRecipient(String message, ClientHandler recipient) {
        try {
            recipient.sendMessage(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getClientAddress() {
        InetAddress address = clientSocket.getInetAddress();
        return address != null ? address.getHostAddress() : null;
    }

    public int getClientPort() {
        int port = clientSocket.getPort();
        return port != 0 ? port : -1;
    }

    public boolean shutdown() throws IOException {
        clientName = null;
        clientSocket.close();
        return clientSocket.isClosed();
    }
}
