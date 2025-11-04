package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import br.com.voteflix.cliente.security.TokenStorage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.net.URL;

public class TelaAdminMenu {
    private SceneManager sceneManager;

    public TelaAdminMenu(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Painel Administrador");

        Button gerenciarFilmesButton = new Button("Gerenciar Filmes");
        gerenciarFilmesButton.setPrefWidth(250);

        Button gerenciarUsuariosButton = new Button("Gerenciar Usuários");
        gerenciarUsuariosButton.setPrefWidth(250);

        Button logoutButton = new Button("Logout");
        logoutButton.setPrefWidth(250);

        gerenciarFilmesButton.setOnAction(e -> sceneManager.mostrarTelaAdminFilmes());

        gerenciarUsuariosButton.setOnAction(e -> sceneManager.mostrarTelaAdminUsuarios());

        logoutButton.setOnAction(e -> {
            ClienteSocket socket = ClienteSocket.getInstance();
            if (socket != null) {
                // Chama o novo método que envia LOGOUT e, no callback, muda a tela
                socket.solicitarLogoutEFechamento(() -> sceneManager.mostrarTelaConexao());
            } else {
                // Se o socket já for nulo, apenas limpa o token e muda a tela
                TokenStorage.clearToken();
                sceneManager.mostrarTelaConexao();
            }
        });


        layout.getChildren().addAll(
                titleLabel,
                gerenciarFilmesButton,
                gerenciarUsuariosButton,
                new Separator(),
                logoutButton
        );

        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            gerenciarFilmesButton.getStyleClass().add("secondary-button");
            gerenciarUsuariosButton.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
            layout.setStyle("-fx-background-color: #F0F0F0;");
        }

        return scene;
    }
}