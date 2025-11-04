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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;

public class TelaCadastro {

    private SceneManager sceneManager;

    public TelaCadastro(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Crie sua Conta");

        TextField loginField = new TextField();
        loginField.setPromptText("Escolha um login (3-20 caracteres, letras/números)"); // Adicionada dica de validação
        loginField.setMaxWidth(300);

        PasswordField senhaField = new PasswordField();
        senhaField.setPromptText("Crie uma senha (3-20 caracteres, letras/números)"); // Adicionada dica de validação
        senhaField.setMaxWidth(300);

        Button cadastrarButton = new Button("Cadastrar");
        cadastrarButton.setPrefWidth(145);

        Button backButton = new Button("Voltar");
        backButton.setPrefWidth(145);

        HBox buttonLayout = new HBox(10, backButton, cadastrarButton);
        buttonLayout.setAlignment(Pos.CENTER);

        cadastrarButton.setOnAction(e -> {
            String login = loginField.getText();
            String senha = senhaField.getText();

            ClienteSocket.getInstance().enviarCadastro(login, senha, (sucesso, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso) {
                        AlertaUtil.mostrarInformacao("Sucesso", mensagem); // Não concatena mais strings extras
                        sceneManager.mostrarTelaLogin();
                    } else {
                        AlertaUtil.mostrarErro("Falha no Cadastro", mensagem);
                    }
                });
            });
        });

        backButton.setOnAction(e -> sceneManager.mostrarTelaInicial());

        layout.getChildren().addAll(titleLabel, new Label("Login:"), loginField, new Label("Senha:"), senhaField, buttonLayout);

        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            backButton.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
            layout.setStyle("-fx-background-color: #F0F0F0;");
        }

        return scene;
    }
}