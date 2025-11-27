package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.net.URL;

public class TelaConexao {

    private SceneManager sceneManager;

    public TelaConexao(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("VOTEFLIX");
        titleLabel.getStyleClass().add("title-label");
        Label sub = new Label("Conectar ao Servidor");
        sub.getStyleClass().add("subtitle-label");

        VBox formBox = new VBox(15);
        formBox.setMaxWidth(350);
        formBox.setStyle("-fx-background-color: rgba(0,0,0,0.5); -fx-padding: 30; -fx-background-radius: 8;");
        formBox.setAlignment(Pos.CENTER);

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("IP do Servidor");

        TextField portaField = new TextField("22222");
        portaField.setPromptText("Porta");

        Button conectarButton = new Button("Conectar");
        conectarButton.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #e50914;");

        conectarButton.setOnAction(e -> {
            String ip = ipField.getText();
            try {
                int porta = Integer.parseInt(portaField.getText());
                statusLabel.setText("Tentando conectar...");
                boolean conectado = ClienteSocket.iniciarConexao(ip, porta, System.out::println);
                if (conectado) {
                    sceneManager.mostrarTelaInicial();
                } else {
                    statusLabel.setText("Falha na conexão. Servidor offline?");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Porta inválida.");
            }
        });

        formBox.getChildren().addAll(ipField, portaField, conectarButton, statusLabel);
        layout.getChildren().addAll(titleLabel, sub, formBox);

        Scene scene = new Scene(layout, 900, 650);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) scene.getStylesheets().add(cssResource.toExternalForm());

        return scene;
    }
}