package br.com.voteflix.servidor.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GerenciadorSocketServidor implements Runnable {

    private final int porta;
    private final Consumer<String> logger;
    private boolean rodando = true;
    private ServerSocket serverSocket;
    private final ExecutorService poolDeThreads = Executors.newCachedThreadPool();
    private final GerenciadorUsuarios gerenciadorUsuarios;

    public GerenciadorSocketServidor(int porta, Consumer<String> logger, GerenciadorUsuarios gerenciadorUsuarios) {
        this.porta = porta;
        this.logger = logger;
        this.gerenciadorUsuarios = gerenciadorUsuarios;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(porta);
            logger.accept("Servidor ouvindo na porta " + porta);

            while (rodando) {
                Socket clientSocket = serverSocket.accept();

                logger.accept("-------------------- NOVA CONEXÃO --------------------");
                logger.accept("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

                GerenciadorCliente gerenciadorCliente = new GerenciadorCliente(clientSocket, logger, gerenciadorUsuarios);

                poolDeThreads.submit(gerenciadorCliente);
            }
        } catch (Exception e) {
            if (rodando) {
                logger.accept("Erro no servidor principal: " + e.getMessage());
            }
        }
    }

    public void parar() {
        rodando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            poolDeThreads.shutdown();
        } catch (Exception e) {
            logger.accept("Erro ao parar o servidor: " + e.getMessage());
        }
        logger.accept("Servidor parado.");
    }
}
