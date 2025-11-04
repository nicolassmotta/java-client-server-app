package br.com.voteflix.cliente;

import br.com.voteflix.cliente.net.ClienteSocket; // Certifique-se que o import está correto
import br.com.voteflix.cliente.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = new SceneManager(primaryStage);
        sceneManager.mostrarTelaConexao(); // A aplicação começa pela tela de conexão

        primaryStage.show();

        // Garante que a conexão seja fechada ao sair
        primaryStage.setOnCloseRequest(e -> {
            if (ClienteSocket.getInstance() != null) {
                ClienteSocket.getInstance().fecharConexao();
            }
        });
    }
}