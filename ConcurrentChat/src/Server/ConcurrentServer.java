package Server;

import utils.ConsoleColor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConcurrentServer {

    private final Logger logger = Logger.getLogger(ConcurrentServer.class.getName());
    private final int PORT;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final String messageLogPath = "chatlog.txt";

    public static void main(String[] args) {
        ConcurrentServer concurrentServer = new ConcurrentServer(9001);
        concurrentServer.run();
    }
    public ConcurrentServer(int PORT) {
        this.PORT = PORT;
    }
    public void run() {
        if (!bindServer(PORT)) {
            logger.log(Level.SEVERE, "Failed to bind server to PORT: " + PORT);
            return;
        }

        logger.log(Level.INFO, "###SERVER START:SERVER ON IP " + serverSocket.getInetAddress().getCanonicalHostName() + serverSocket.getLocalPort());

        Thread consoleInputThread = new Thread(new ConsoleInputHandler());
        consoleInputThread.start();

        while (serverSocket.isBound()) {
            logger.log(Level.INFO, "WAITING FOR CLIENT CONNECTION...");
            acceptClientAndTrack(serverSocket);
            logger.log(Level.INFO, "New client accepted and tracked.");
        }
    }
    private boolean bindServer(int PORT) {
        try {
            serverSocket = new ServerSocket(PORT);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to bind server to PORT: " + PORT, e);
            return false;
        }
    }
    private void acceptClientAndTrack(ServerSocket serverSocket) {
        try {
            Socket clientSocket = serverSocket.accept();
            logger.log(Level.INFO, "CLIENT ACCEPTED FROM " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            ClientHandler clientHandler = new ClientHandler(clientSocket, this, logger);
            clientHandlers.add(clientHandler);
            executorService.execute(clientHandler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error accepting client connection: " + e.getMessage(), e);
        }
    }
    public void broadcastMessage(String message, ClientHandler sender) {
        String senderName = sender.getClientSimpleName();
        ConsoleColor senderColor = sender.getColor();
        String formattedMessage = senderColor.getCode() + senderName + ": " + ConsoleColor.RESET.getCode() + message;

        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler != sender) {
                try {
                    clientHandler.sendMessage(formattedMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            persistMessageJournal(senderName + ": " +message);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "UNABLE TO LOG MESSAGE " + e.getMessage());
        }
    }
    public boolean removeClient(ClientHandler clientHandler) {

        clientHandlers.remove(clientHandler);

        return true;
    }
    public void logConnected(){
        String allNames = clientHandlers.stream()
                .map(c ->  "'n/p: " + c.getClientSimpleNameIpAndPORT() + "'\n")
                .collect(Collectors.joining(""));
        logger.log(Level.INFO, "Clients connected:\n" + allNames);

    }
    public void kickClient(String clientName) {
        clientName = clientName.trim();
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getClientSimpleNameIpAndPORT().equalsIgnoreCase(clientName)) {

                try {

                    logger.log(Level.INFO, "Kicking client: " + clientHandler.getClientSimpleName());
                    logger.log(Level.INFO, "Untracking client: " + clientHandler.getClientSimpleName());
                    clientHandler.sendMessageSingleLine(ConsoleColor.RED.getCode() + "Connection closed... reason: KICKED" + ConsoleColor.RESET.getCode());
                    if (clientHandler.shutdown() && removeClient(clientHandler)){
                        logger.log(Level.INFO, "User kicked and untracked...");
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                finally {
                    logConnected();
                    }
                return;
            }
        }
        logger.log(Level.WARNING, "Client '" + clientName + "' not found.");
    }

    public boolean checkIfNameExists(String clientName) {
        if (clientHandlers.isEmpty()) {
            return false; // If the list is empty, the name doesn't exist
        }
        for (ClientHandler ch : clientHandlers) {
            String name = ch.getClientSimpleName();
            if (name.equalsIgnoreCase(clientName)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void persistMessageJournal(String message) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(messageLogPath, true), true);
        writer.println("|" + new Date().getTime() + "| " + message);
    }

    public synchronized String readFromMessageJournal() {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(messageLogPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "FILE NOT FOUND -> path " + messageLogPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO -> " + e.getMessage());
        }
        return stringBuilder.toString();
    }

    private class ConsoleInputHandler implements Runnable {
        @Override
        public void run() {
            handleConsoleInput();
        }

        private void handleConsoleInput() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.startsWith("kick ")) {
                    String clientName = input.substring(5);
                    kickClient(clientName);
                } else if (input.startsWith("names")) {
                    logConnected();
                } else {
                    logger.log(Level.INFO, "Console input: " + input);
                }
            }
        }
    }


}

