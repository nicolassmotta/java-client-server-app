package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import br.com.voteflix.cliente.security.TokenStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import java.net.URL;

public class TelaMenu {
    private SceneManager sceneManager;

    public TelaMenu(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Menu Principal");

        Button listarFilmesButton = new Button("Ver Filmes");
        listarFilmesButton.setPrefWidth(250);

        Button listarMinhasReviewsButton = new Button("Ver Minhas Reviews");
        listarMinhasReviewsButton.setPrefWidth(250);

        Button verDadosButton = new Button("Ver Meus Dados");
        verDadosButton.setPrefWidth(250);

        Button editarButton = new Button("Editar Minha Senha");
        editarButton.setPrefWidth(250);

        Button excluirButton = new Button("Excluir Minha Conta");
        excluirButton.setPrefWidth(250);

        Button logoutButton = new Button("Logout");
        logoutButton.setPrefWidth(250);

        listarFilmesButton.setOnAction(e -> sceneManager.mostrarTelaListarFilmes());

        listarMinhasReviewsButton.setOnAction(e -> sceneManager.mostrarTelaMinhasReviews());

        verDadosButton.setOnAction(e -> {
            ClienteSocket.getInstance().enviarListarDados((sucesso, dados, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso) {
                        if (dados != null) {
                            String login = null;
                            if (dados.isJsonPrimitive() && dados.getAsJsonPrimitive().isString()) {
                                login = dados.getAsString();
                            }

                            if (login != null) {
                                Dialog<Void> dialog = new Dialog<>();
                                dialog.setTitle("Meus Dados");
                                dialog.setHeaderText("Informações da Conta");

                                GridPane grid = new GridPane();
                                grid.setHgap(10);
                                grid.setVgap(10);
                                grid.setPadding(new Insets(20, 100, 10, 10));

                                TextField txtLogin = new TextField(login);
                                txtLogin.setEditable(false);

                                grid.add(new Label("Login:"), 0, 0);
                                grid.add(txtLogin, 1, 0);

                                dialog.getDialogPane().setContent(grid);
                                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                                dialog.showAndWait();
                            } else {
                                AlertaUtil.mostrarInformacao("Meus Dados", mensagem);
                                System.err.println("Recebido sucesso em Listar Dados, mas formato inesperado: " + dados.toString());
                            }
                        } else {
                            AlertaUtil.mostrarInformacao("Meus Dados", mensagem);
                        }
                    } else {
                        AlertaUtil.mostrarErro("Erro na Consulta", mensagem);
                    }
                });
            });
        });

        editarButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Editar Senha");
            dialog.setHeaderText("Digite sua nova senha:");
            dialog.setContentText("Nova Senha:");

            dialog.showAndWait().ifPresent(novaSenha -> {
                ClienteSocket.getInstance().enviarEdicaoUsuario(novaSenha, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Edição de Senha", mensagem);
                        } else {
                            AlertaUtil.mostrarErro("Erro na Edição", mensagem);
                        }
                    });
                });
            });
        });

        excluirButton.setOnAction(e -> {
            boolean confirmado = AlertaUtil.mostrarConfirmacao("Excluir Conta", "Tem certeza que deseja excluir sua conta? Esta ação é irreversível.");
            if (confirmado) {
                ClienteSocket.getInstance().enviarExcluirUsuario((sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                            sceneManager.mostrarTelaConexao();
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Excluir", mensagem);
                            sceneManager.mostrarTelaConexao();
                        }
                    });
                });
            }
        });

        logoutButton.setOnAction(e -> {
            ClienteSocket socket = ClienteSocket.getInstance();
            if (socket != null) {
                socket.solicitarLogoutEFechamento(() -> sceneManager.mostrarTelaConexao());
            } else {
                TokenStorage.clearToken();
                sceneManager.mostrarTelaConexao();
            }
        });

        layout.getChildren().addAll(
                titleLabel,
                listarFilmesButton,
                listarMinhasReviewsButton,
                new Separator(),
                verDadosButton,
                editarButton,
                new Separator(),
                excluirButton,
                logoutButton
        );

        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            listarFilmesButton.getStyleClass().add("secondary-button");
            listarMinhasReviewsButton.getStyleClass().add("secondary-button");
            verDadosButton.getStyleClass().add("secondary-button");
            editarButton.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
            layout.setStyle("-fx-background-color: #F0F0F0;");
        }

        return scene;
    }
}