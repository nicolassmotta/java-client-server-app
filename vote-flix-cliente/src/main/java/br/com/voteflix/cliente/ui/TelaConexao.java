package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.net.URL;

public class TelaConexao {

    private SceneManager sceneManager;

    public TelaConexao(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        // Carregamento seguro da Logo
        InputStream logoStream = getClass().getResourceAsStream("/logo.jpg");
        if (logoStream != null) {
            ImageView logo = new ImageView(new Image(logoStream));
            logo.setFitHeight(80);
            logo.setPreserveRatio(true);
            layout.getChildren().add(logo);
        } else {
            System.err.println("Recurso 'logo.jpg' não encontrado.");
        }

        Label titleLabel = new Label("Bem-vindo ao VoteFlix");

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("IP do Servidor");
        ipField.setMaxWidth(250);

        TextField portaField = new TextField("22222");
        portaField.setPromptText("Porta");
        portaField.setMaxWidth(250);

        Button conectarButton = new Button("Conectar");
        Label statusLabel = new Label();

        conectarButton.setOnAction(e -> {
            String ip = ipField.getText();
            try {
                int porta = Integer.parseInt(portaField.getText());
                boolean conectado = ClienteSocket.iniciarConexao(ip, porta, System.out::println);
                if (conectado) {
                    sceneManager.mostrarTelaInicial();
                } else {
                    statusLabel.setText("Falha ao conectar. Verifique os dados e se o servidor está online.");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Porta inválida. Insira um número.");
            }
        });

        layout.getChildren().addAll(titleLabel, new Label("IP do Servidor:"), ipField, new Label("Porta:"), portaField, conectarButton, statusLabel);

        Scene scene = new Scene(layout, 800, 600);

        // Carregamento seguro do CSS
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            statusLabel.getStyleClass().add("status-label");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
            layout.setStyle("-fx-background-color: #F0F0F0;"); // Fallback
        }

        return scene;
    }
}