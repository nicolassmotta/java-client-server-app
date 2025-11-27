package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import br.com.voteflix.cliente.security.TokenStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.net.URL;

public class TelaMenu {
    private SceneManager sceneManager;

    public TelaMenu(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("VOTEFLIX");
        titleLabel.getStyleClass().add("title-label");

        Label welcomeLabel = new Label("O que você quer assistir hoje?");
        welcomeLabel.getStyleClass().add("subtitle-label");

        header.getChildren().addAll(titleLabel, welcomeLabel);
        root.setTop(header);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(40));

        VBox cardFilmes = criarCardMenu("Catálogo de Filmes", "Explore, avalie e descubra.", "🎬");
        VBox cardReviews = criarCardMenu("Minhas Avaliações", "Gerencie suas críticas.", "⭐");
        VBox cardDados = criarCardMenu("Meu Perfil", "Visualize seus dados.", "👤");
        VBox cardSenha = criarCardMenu("Segurança", "Altere sua senha.", "🔒");

        grid.add(cardFilmes, 0, 0);
        grid.add(cardReviews, 1, 0);
        grid.add(cardDados, 0, 1);
        grid.add(cardSenha, 1, 1);

        root.setCenter(grid);

        VBox footer = new VBox(15);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20, 0, 0, 0));

        Button btnExcluir = new Button("Excluir Minha Conta");
        btnExcluir.getStyleClass().add("secondary-button");
        btnExcluir.setStyle("-fx-border-color: #E50914; -fx-text-fill: #E50914;");
        btnExcluir.setPrefWidth(200);

        Button btnLogout = new Button("Sair do Sistema");
        btnLogout.getStyleClass().add("secondary-button");
        btnLogout.setPrefWidth(200);

        footer.getChildren().addAll(new Separator(), btnExcluir, btnLogout);
        root.setBottom(footer);

        cardFilmes.setOnMouseClicked(e -> sceneManager.mostrarTelaListarFilmes());
        cardReviews.setOnMouseClicked(e -> sceneManager.mostrarTelaMinhasReviews());

        cardDados.setOnMouseClicked(e -> {
            ClienteSocket.getInstance().enviarListarDados((sucesso, dados, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso && dados != null) {
                        String login = dados.isJsonPrimitive() ? dados.getAsString() : "Usuário";
                        AlertaUtil.mostrarInformacao("Meus Dados", "Usuário Logado: " + login + "\nStatus: Ativo");
                    } else {
                        AlertaUtil.mostrarErro("Erro", mensagem);
                    }
                });
            });
        });

        cardSenha.setOnMouseClicked(e -> {
            TextInputDialog dialog = new TextInputDialog();
            if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialog.setTitle("Alterar Senha");
            dialog.setHeaderText("Segurança da Conta");
            dialog.setContentText("Nova Senha:");
            dialog.showAndWait().ifPresent(novaSenha -> {
                if(novaSenha.length() < 3) { AlertaUtil.mostrarErro("Erro", "Senha muito curta."); return; }
                ClienteSocket.getInstance().enviarEdicaoUsuario(novaSenha, (sucesso, mensagem) -> Platform.runLater(() -> {
                    if (sucesso) AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                    else AlertaUtil.mostrarErro("Erro", mensagem);
                }));
            });
        });

        btnExcluir.setOnAction(e -> {
            if (AlertaUtil.mostrarConfirmacao("Zona de Perigo", "Tem certeza? Sua conta e todas as reviews serão apagadas permanentemente.")) {
                ClienteSocket.getInstance().enviarExcluirUsuario((sucesso, mensagem) -> Platform.runLater(() -> {
                    if (sucesso) {
                        ClienteSocket.getInstance().fecharConexaoLocalmente();
                        sceneManager.mostrarTelaConexao();
                    } else AlertaUtil.mostrarErro("Erro", mensagem);
                }));
            }
        });

        btnLogout.setOnAction(e -> {
            ClienteSocket socket = ClienteSocket.getInstance();
            if (socket != null) socket.solicitarLogoutEFechamento(() -> sceneManager.mostrarTelaConexao());
            else { TokenStorage.clearToken(); sceneManager.mostrarTelaConexao(); }
        });

        Scene scene = new Scene(root, 900, 650);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) scene.getStylesheets().add(cssResource.toExternalForm());
        return scene;
    }

    private VBox criarCardMenu(String titulo, String subtitulo, String iconeEmoji) {
        VBox card = new VBox(10);
        card.getStyleClass().add("menu-card");
        card.setPrefSize(220, 140);

        Label icon = new Label(iconeEmoji);
        icon.setStyle("-fx-font-size: 32px;");

        Label title = new Label(titulo);
        title.getStyleClass().add("menu-card-title");

        Label desc = new Label(subtitulo);
        desc.getStyleClass().add("menu-card-desc");
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);

        card.getChildren().addAll(icon, title, desc);
        return card;
    }
}