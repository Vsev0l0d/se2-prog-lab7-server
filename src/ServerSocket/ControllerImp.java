package ServerSocket;

import Exceptions.DatabaseException;
import Interfaces.CollectionManager;
import Interfaces.Controller;
import Interfaces.DatabaseManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import Interfaces.Decrypting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ControllerImp implements Controller {
    private Socket clientSocket; //сокет для общения
    private ServerSocket server; // серверсокет
    private ObjectInputStream in; // поток чтения из сокета
    private final CollectionManager collectionManager;
    private final DatabaseManager databaseManager;
    private final Decrypting decrypting;
    private final Logger logger = LoggerFactory.getLogger(ControllerImp.class);

    @Inject
    public ControllerImp(CollectionManager collectionManager, DatabaseManager databaseManager, Decrypting decrypt) {
        this.collectionManager = collectionManager;
        this.databaseManager = databaseManager;
        this.decrypting = decrypt;
    }

    @Override
    public void run(String strPort){
        try {
            try {
                int port = 0;
                collectionManager.initList();
                logger.info("Создана пустая коллекция");
                try {
                    databaseManager.loadCollectionFromDatabase(collectionManager);
                } catch (DatabaseException e) {
                    logger.error("Ошибка при выгрузке коллекции: " + e);
                }
                try {
                    port = Integer.parseInt(strPort);
                } catch (NumberFormatException ex) {
                    logger.info("Ошибка! Неправильный формат порта.");
                    System.exit(0);
                }

                server = new ServerSocket(port);
                logger.info("Сервер запущен!");
                while (true) {
                    clientSocket = server.accept();
                    logger.info("А я все думал, когда же ты появишься: " + clientSocket);
                    try {
                        while (true) {
                            in = new ObjectInputStream(clientSocket.getInputStream());
                            Object o = in.readObject();
                            decrypting.decrypt(o, clientSocket);
                        }

                    } catch (EOFException | SocketException ex) {
                        logger.info("Клиент " + clientSocket + " того, откинулся...");
                    } catch (InterruptedException | DatabaseException e) {
                        e.printStackTrace();
                    } finally {
                        clientSocket.close();
                        if (in != null) { in.close(); }
                    }
                }
            } finally {
                if (clientSocket != null) { clientSocket.close(); }
                logger.info("Сервер закрыт!");
                server.close();
            }
        } catch (IOException | ClassNotFoundException | NullPointerException e) {
            e.printStackTrace();
            logger.error(String.valueOf(e));
        }
    }
}