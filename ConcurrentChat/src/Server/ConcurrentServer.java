package Server;

import utils.ConsoleColor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class ConcurrentServer {

    private final Logger logger = Logger.getLogger(ConcurrentServer.class.getName());
    private final int PORT;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private String messageLogPath;

    private boolean consoleLoggingActive = true;

    public static void main(String[] args) {
        ConcurrentServer concurrentServer = new ConcurrentServer(9001);
        concurrentServer.setupLogger();
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
        String formattedMessage = senderColor.getCode() + senderName + ": " + ConsoleColor.DEFAULT.getCode() + message;
        System.out.println(formattedMessage);
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
            persistMessageJournal(senderName + ": " + message);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "UNABLE TO LOG MESSAGE " + e.getMessage());
        }
    }

    public void directMessageToRecipient(String message, String senderName, ClientHandler recipientName) {
        ClientHandler recipient = findClientHandlerByName(recipientName.getClientSimpleName());
        if (recipient != null) {
            try {
                recipient.sendMessage(ConsoleColor.WHISPER.getCode() + "<whisper>" +senderName + ": " + ConsoleColor.DEFAULT.getCode() +  message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.log(Level.INFO, "Recipient not found: " + recipientName);
        }
    }

    public void directMessage(String message, ClientHandler recipient) {
        String formattedMessage = ConsoleColor.ADMIN.getCode() + "<whisper>ADMIN: " + ConsoleColor.DEFAULT.getCode() +  message;
        try {
            recipient.sendMessage(formattedMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void broadcastServerMessage(String message) {
        String formattedMessage = ConsoleColor.ADMIN.getCode()  + "ADMIN: " + message + ConsoleColor.DEFAULT.getCode();

        for (ClientHandler clientHandler : clientHandlers) {

                try {
                    clientHandler.sendMessage(formattedMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

        }
        try {
            persistMessageJournal("ADMIN: " + message);
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
                .map(c ->  "'n/IP/p: " + c.getClientSimpleNameIpAndPORT() + "'\n")
                .collect(Collectors.joining(""));
        logger.log(Level.INFO, "Clients connected:\n" + allNames);
    }

    private void setupLogger() {
        try {

            String logFilePath = "logs/server.log";

            File logDir = new File(logFilePath).getParentFile();
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            FileHandler fileHandler = new FileHandler(logFilePath, true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(consoleLoggingActive);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to set up logger: " + e.getMessage(), e);
        }
    }
    public void kickClient(String clientName) {
        clientName = clientName.trim();
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getClientSimpleNameIpAndPORT().equalsIgnoreCase(clientName)) {

                try {

                    logger.log(Level.INFO, "Kicking client: " + clientHandler.getClientSimpleName());
                    logger.log(Level.INFO, "Untracking client: " + clientHandler.getClientSimpleName());
                    clientHandler.sendMessageSingleLine(ConsoleColor.ERROR_WARNING.getCode() + "Connection closed... reason: KICKED" + ConsoleColor.DEFAULT.getCode());
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
        messageLogPath = "logs/chathistory.log";

        File logDir = new File(messageLogPath).getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        PrintWriter writer = new PrintWriter(new FileWriter(messageLogPath, true), true);
        writer.println("|"
                + LocalDateTime.now().
                format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"))
                + "| "
                + message);
    }

    public synchronized String readFromMessageJournal() {
        StringBuilder stringBuilder = new StringBuilder();
        messageLogPath = "logs/chathistory.log";

        File logDir = new File(messageLogPath).getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
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

    public  String readLastEntriesFromJournal(int numEntries) {
        messageLogPath = "logs/chathistory.log";

        File logDir = new File(messageLogPath).getParentFile();
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        StringBuilder result = new StringBuilder();
        try (FileReader fileReader = new FileReader(messageLogPath);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {

            Deque<String> lastEntries = new ArrayDeque<>();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if (lastEntries.size() == numEntries) {
                    lastEntries.poll();
                }
                lastEntries.offer(line);
            }

            while (!lastEntries.isEmpty()) {
                result.insert(0, lastEntries.pollLast() + "\n");            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ConsoleColor.CHAT_HISTORY.getCode() + result.toString() + ConsoleColor.DEFAULT.getCode();
    }

    public synchronized void clearChatlogHistory() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(messageLogPath, false));
            writer.close();
            logger.log(Level.INFO, "Chat log history cleared successfully.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to clear chat log history: " + e.getMessage());
        }
    }

    public ClientHandler findClientHandlerByName(String name) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (clientHandler.getClientSimpleName().equalsIgnoreCase(name)) {
                return clientHandler;
            }
        }
        return null; // Return null if the client handler with the specified name is not found
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
                processConsoleCommand(input);
            }
        }
        private void processConsoleCommand(String input) {
            if (input.startsWith("@")) {
                int colonIndex = input.indexOf(':');
                if (colonIndex != -1) {
                    String recipientNames = input.substring(1, colonIndex).trim();
                    String message = input.substring(colonIndex + 1).trim(); // Message starts after the colon

                    String[] recipientNameArray = recipientNames.split("\\s*,\\s*");
                    for (String recipientName : recipientNameArray) {
                        ClientHandler recipient = findClientHandlerByName(recipientName);
                        if (recipient != null) {
                            directMessage(message, recipient);
                        } else {
                            logger.log(Level.INFO, "Recipient not found: " + recipientName);
                        }
                    }
                } else {
                    logger.log(Level.INFO, "Invalid format. Please use: @recipientName1 recipientName2: message");
                }
            }
            else if (input.startsWith("/")) {
                String[] tokens = input.split(" ", 2);
                String command = tokens[0];
                String arguments = tokens.length > 1 ? tokens[1] : "";

                switch (command) {
                    case "/kick":
                        kickClient(arguments.trim());
                        break;
                    case "/users":
                        logConnected();
                        break;
                    case "/clearhistory":
                        clearChatlogHistory();
                        break;
                    case "/showhistory":
                        System.out.println(readFromMessageJournal());
                        break;
                    case "/togglelogs":
                        consoleLoggingActive = !consoleLoggingActive;
                        logger.setUseParentHandlers(consoleLoggingActive);
                        break;
                    case "/shout":
                        broadcastServerMessage(arguments);
                        break;
                    default:
                        logger.log(Level.INFO, "Unknown command: " + command);
                }
            } else {
                logger.log(Level.INFO, "Console input: " + input);
            }
        }



    }
}

