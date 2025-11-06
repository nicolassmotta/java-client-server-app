package br.com.voteflix.cliente.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL;

public class TelaInicial {

    private SceneManager sceneManager;

    public TelaInicial(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        InputStream logoStream = getClass().getResourceAsStream("/logo.jpg");
        if (logoStream != null) {
            ImageView logo = new ImageView(new Image(logoStream));
            logo.setFitHeight(100);
            logo.setPreserveRatio(true);
            layout.getChildren().add(logo);
        } else {
            System.err.println("Recurso 'logo.jpg' não encontrado.");
        }

        Button btnLogin = new Button("Login");
        btnLogin.setPrefWidth(200);

        Button btnCadastro = new Button("Cadastrar");
        btnCadastro.setPrefWidth(200);

        btnLogin.setOnAction(e -> sceneManager.mostrarTelaLogin());
        btnCadastro.setOnAction(e -> sceneManager.mostrarTelaCadastro());

        layout.getChildren().addAll(btnLogin, btnCadastro);

        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            btnCadastro.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
            layout.setStyle("-fx-background-color: #F0F0F0;");
        }

        return scene;
    }
}