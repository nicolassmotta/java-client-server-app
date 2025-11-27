package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import br.com.voteflix.cliente.security.TokenStorage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import java.net.URL;

public class TelaAdminMenu {
    private SceneManager sceneManager;

    public TelaAdminMenu(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(30);
        layout.setPadding(new Insets(50));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("PAINEL ADMIN");
        titleLabel.getStyleClass().add("title-label");

        Label subTitle = new Label("Gerenciamento do Sistema");
        subTitle.getStyleClass().add("subtitle-label");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);

        VBox cardFilmes = criarCard("Gerenciar Filmes", "Adicionar ou remover filmes.", "🎞️");
        VBox cardUsers = criarCard("Gerenciar Usuários", "Resetar senhas ou banir.", "⚙");

        grid.add(cardFilmes, 0, 0);
        grid.add(cardUsers, 1, 0);

        Button logoutButton = new Button("Sair do Modo Admin");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setPrefWidth(250);

        cardFilmes.setOnMouseClicked(e -> sceneManager.mostrarTelaAdminFilmes());
        cardUsers.setOnMouseClicked(e -> sceneManager.mostrarTelaAdminUsuarios());

        logoutButton.setOnAction(e -> {
            ClienteSocket socket = ClienteSocket.getInstance();
            if (socket != null) socket.solicitarLogoutEFechamento(() -> sceneManager.mostrarTelaConexao());
            else { TokenStorage.clearToken(); sceneManager.mostrarTelaConexao(); }
        });

        layout.getChildren().addAll(titleLabel, subTitle, grid, new Separator(), logoutButton);

        Scene scene = new Scene(layout, 900, 650);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) scene.getStylesheets().add(cssResource.toExternalForm());

        return scene;
    }

    private VBox criarCard(String titulo, String desc, String emoji) {
        VBox card = new VBox(10);
        card.getStyleClass().add("menu-card");
        card.setPrefSize(240, 160);

        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 36px; -fx-text-fill: #E50914;");

        Label t = new Label(titulo);
        t.getStyleClass().add("menu-card-title");

        Label d = new Label(desc);
        d.getStyleClass().add("menu-card-desc");
        d.setWrapText(true);
        d.setAlignment(Pos.CENTER);

        card.getChildren().addAll(icon, t, d);
        return card;
    }
}