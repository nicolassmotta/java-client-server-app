package br.com.voteflix.servidor;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.net.GerenciadorUsuarios;
import br.com.voteflix.servidor.net.GerenciadorSocketServidor;

import java.util.function.Consumer;

public class Servidor {

    private final int porta;
    private final Consumer<String> logger;
    private GerenciadorSocketServidor gerenciadorSocketServidor;
    private Thread threadDoServidor;
    private final GerenciadorUsuarios gerenciadorUsuarios;

    public Servidor(int porta, Consumer<String> logger, GerenciadorUsuarios gerenciadorUsuarios) {
        this.porta = porta;
        this.logger = logger;
        this.gerenciadorUsuarios = gerenciadorUsuarios;
    }

    public void iniciar() {
        gerenciadorSocketServidor = new GerenciadorSocketServidor(porta, logger, gerenciadorUsuarios);
        threadDoServidor = new Thread(gerenciadorSocketServidor);
        threadDoServidor.setDaemon(true);
        threadDoServidor.start();

        testarConexaoBanco();
    }

    public void parar() {
        if (gerenciadorSocketServidor != null) {
            gerenciadorSocketServidor.parar();
        }
        if (threadDoServidor != null) {
            threadDoServidor.interrupt();
        }
        logger.accept("Servidor finalizado pelo painel de controle.");
    }

    private void testarConexaoBanco() {
        new Thread(() -> {
            try (java.sql.Connection conn = ConexaoBancoDados.obterConexao()) {
                if (conn != null) {
                    logger.accept("Banco de dados conectado com sucesso!");
                } else {
                    logger.accept("ERRO: Falha ao conectar ao banco de dados.");
                }
            } catch (Exception e) {
                logger.accept("ERRO ao testar conexão com o banco: " + e.getMessage());
            }
        }).start();
    }
}
