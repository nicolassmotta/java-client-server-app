package br.com.voteflix.servidor;

import br.com.voteflix.servidor.dao.UsuarioDAO;
import br.com.voteflix.servidor.net.GerenciadorUsuarios;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Collection;
import java.util.function.Consumer;

public class AplicacaoServidor extends Application {

    private final TextArea areaDeLog = new TextArea();
    private final ListView<String> visualizacaoUsuariosConectados = new ListView<>();
    private final Button botaoIniciar = new Button("Iniciar Servidor");
    private final Button botaoLimparLog = new Button("Limpar Log");
    private final TextField campoPorta = new TextField("22222");

    private Servidor servidor;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage palcoPrincipal) {
        palcoPrincipal.setTitle("Painel de Controle - Servidor VoteFlix");

        configurarInterface(palcoPrincipal);
        configurarAcoes();

        palcoPrincipal.show();
    }

    private void configurarInterface(Stage palcoPrincipal) {
        areaDeLog.setEditable(false);
        areaDeLog.setWrapText(true);
        areaDeLog.setStyle("-fx-font-family: monospace; -fx-font-size: 10pt;");

        VBox painelEsquerdo = new VBox(10, new Label("Usuários Ativos:"), visualizacaoUsuariosConectados);
        VBox.setVgrow(visualizacaoUsuariosConectados, Priority.ALWAYS);
        painelEsquerdo.setPadding(new Insets(10));

        VBox painelDireito = new VBox(10, new Label("Log de Atividades:"), areaDeLog);
        VBox.setVgrow(areaDeLog, Priority.ALWAYS);
        painelDireito.setPadding(new Insets(10));

        SplitPane painelDividido = new SplitPane();
        painelDividido.getItems().addAll(painelEsquerdo, painelDireito);
        painelDividido.setDividerPositions(0.3);

        HBox controlePorta = new HBox(10, new Label("Porta:"), campoPorta);
        HBox controleBotoes = new HBox(10, botaoIniciar, botaoLimparLog);
        VBox controlesSuperiores = new VBox(10, controlePorta, controleBotoes);
        controlesSuperiores.setPadding(new Insets(10));

        BorderPane raiz = new BorderPane();
        raiz.setTop(controlesSuperiores);
        raiz.setCenter(painelDividido);

        Scene cena = new Scene(raiz, 800, 600);
        cena.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        palcoPrincipal.setScene(cena);
    }

    private void configurarAcoes() {
        botaoIniciar.setOnAction(evento -> {
            try {
                new UsuarioDAO().criarAdminSeNaoExistir();
                int porta = Integer.parseInt(campoPorta.getText());
                iniciarLogicaServidor(porta);
                botaoIniciar.setDisable(true);
                campoPorta.setDisable(true);
            } catch (NumberFormatException e) {
                logar("ERRO: Porta inválida. Por favor, insira um número.");
            }
        });

        botaoLimparLog.setOnAction(e -> areaDeLog.clear());

        Platform.runLater(() ->
                botaoIniciar.getScene().getWindow().setOnCloseRequest(evento -> {
                    if (servidor != null) {
                        servidor.parar();
                    }
                    Platform.exit();
                })
        );
    }

    private void iniciarLogicaServidor(int porta) {
        Consumer<String> logger = mensagem -> Platform.runLater(() -> logar(mensagem));

        Consumer<Collection<String>> atualizadorListaUsuarios = logins ->
                Platform.runLater(() -> visualizacaoUsuariosConectados.getItems().setAll(logins));

        GerenciadorUsuarios gerenciadorUsuarios = new GerenciadorUsuarios(atualizadorListaUsuarios);

        servidor = new Servidor(porta, logger, gerenciadorUsuarios);
        servidor.iniciar();

        logar("Servidor iniciado na porta " + porta);
    }

    private void logar(String mensagem) {
        areaDeLog.appendText(mensagem + "\n");
    }
}