package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.net.URL;

public class TelaLogin {

    private SceneManager sceneManager;

    public TelaLogin(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);
        Label brandLabel = new Label("VOTEFLIX");
        brandLabel.getStyleClass().add("title-label");
        brandLabel.setStyle("-fx-font-size: 48px; -fx-padding: 0 0 30 0;");

        Label subtitle = new Label("Login");
        subtitle.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");

        VBox formBox = new VBox(15);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 30; -fx-background-radius: 8;");
        formBox.setAlignment(Pos.CENTER);

        TextField loginField = new TextField();
        loginField.setPromptText("Usuário");

        PasswordField senhaField = new PasswordField();
        senhaField.setPromptText("Senha");

        Button loginButton = new Button("Entrar");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Button cadastroButton = new Button("Novo por aqui? Cadastre-se");
        cadastroButton.getStyleClass().add("secondary-button");
        cadastroButton.setMaxWidth(Double.MAX_VALUE);
        cadastroButton.setStyle("-fx-border-color: transparent; -fx-text-fill: #999;");

        Button backButton = new Button("Voltar para Conexão");
        backButton.getStyleClass().add("secondary-button");
        backButton.setStyle("-fx-font-size: 12px;");

        formBox.getChildren().addAll(subtitle, loginField, senhaField, loginButton, cadastroButton);

        loginButton.setOnAction(e -> {
            String login = loginField.getText();
            String senha = senhaField.getText();

            ClienteSocket.getInstance().enviarLogin(login, senha, (sucesso, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso) {
                        if ("admin".equalsIgnoreCase(login)) {
                            sceneManager.mostrarTelaAdminMenu();
                        } else {
                            sceneManager.mostrarTelaMenu();
                        }
                    } else {
                        AlertaUtil.mostrarErro("Falha no Login", mensagem);
                    }
                });
            });
        });

        cadastroButton.setOnAction(e -> sceneManager.mostrarTelaCadastro());
        backButton.setOnAction(e -> sceneManager.mostrarTelaConexao());

        layout.getChildren().addAll(brandLabel, formBox, backButton);

        Scene scene = new Scene(layout, 900, 650);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        return scene;
    }
}