package Client;

import java.io.*;
import java.net.Socket;

public class ConcurrentClient {
    public static void main(String[] args) {
        new ConcurrentClient().start();
    }

    public void start() {
        try {
            Socket socket = new Socket("localhost", 9001);

            Thread serverReaderThread = new Thread(new ClientMessageReader(socket));
            serverReaderThread.start();

            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
            OutputStream outputStream = socket.getOutputStream();
            String userInput;
            while ((userInput = userInputReader.readLine()) != null) {
                outputStream.write((userInput + "\n").getBytes());
                outputStream.flush();
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
