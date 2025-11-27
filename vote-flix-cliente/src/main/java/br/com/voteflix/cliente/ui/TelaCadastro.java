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

public class TelaCadastro {

    private SceneManager sceneManager;

    public TelaCadastro(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Crie sua Conta");
        titleLabel.getStyleClass().add("title-label");

        VBox formBox = new VBox(15);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 30; -fx-background-radius: 8;");
        formBox.setAlignment(Pos.CENTER);

        TextField loginField = new TextField();
        loginField.setPromptText("Escolha um usuário");

        PasswordField senhaField = new PasswordField();
        senhaField.setPromptText("Escolha uma senha");

        Button cadastrarButton = new Button("Cadastrar");
        cadastrarButton.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("Voltar ao Login");
        backButton.getStyleClass().add("secondary-button");
        backButton.setMaxWidth(Double.MAX_VALUE);

        formBox.getChildren().addAll(loginField, senhaField, cadastrarButton, backButton);

        cadastrarButton.setOnAction(e -> {
            String login = loginField.getText();
            String senha = senhaField.getText();

            ClienteSocket.getInstance().enviarCadastro(login, senha, (sucesso, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso) {
                        AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                        sceneManager.mostrarTelaLogin();
                    } else {
                        AlertaUtil.mostrarErro("Falha", mensagem);
                    }
                });
            });
        });

        backButton.setOnAction(e -> sceneManager.mostrarTelaLogin());

        layout.getChildren().addAll(titleLabel, formBox);

        Scene scene = new Scene(layout, 900, 650);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) scene.getStylesheets().add(cssResource.toExternalForm());

        return scene;
    }
}