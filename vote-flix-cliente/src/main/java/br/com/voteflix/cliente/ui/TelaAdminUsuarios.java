package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;

public class TelaAdminUsuarios {

    private SceneManager sceneManager;
    private ListView<UsuarioItem> listaUsuariosView = new ListView<>();

    private static class UsuarioItem {
        String id;
        String nome;

        public UsuarioItem(String id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        @Override
        public String toString() {
            return nome + " (ID: " + id + ")";
        }
    }

    public TelaAdminUsuarios(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Gerenciar Usuários");
        VBox.setVgrow(listaUsuariosView, Priority.ALWAYS);

        Button btnEditarSenha = new Button("Editar Senha");
        btnEditarSenha.setPrefWidth(180);
        Button btnExcluir = new Button("Excluir Usuário");
        btnExcluir.setPrefWidth(180);

        HBox crudButtons = new HBox(10, btnEditarSenha, btnExcluir);
        crudButtons.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setPrefWidth(380);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaAdminMenu());

        btnEditarSenha.setOnAction(e -> {
            UsuarioItem selecionado = listaUsuariosView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um usuário para editar a senha.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog();

            if (getClass().getResource("/styles.css") != null) {
                dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            }

            dialog.setTitle("Editar Senha");
            dialog.setHeaderText("Editando usuário: " + selecionado.nome);
            dialog.setContentText("Nova Senha:");

            dialog.showAndWait().ifPresent(novaSenha -> {
                ClienteSocket.getInstance().adminEditarUsuario(selecionado.id, novaSenha, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                        } else {
                            AlertaUtil.mostrarErro("Erro na Edição", mensagem);
                        }
                    });
                });
            });
        });

        btnExcluir.setOnAction(e -> {
            UsuarioItem selecionado = listaUsuariosView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um usuário para excluir.");
                return;
            }

            boolean confirmado = AlertaUtil.mostrarConfirmacao("Excluir Usuário", "Tem certeza que deseja excluir o usuário '" + selecionado.nome + "'? Esta ação também excluirá as reviews deste usuário.");
            if (confirmado) {
                ClienteSocket.getInstance().adminExcluirUsuario(selecionado.id, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                            carregarUsuarios();
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Excluir", mensagem);
                        }
                    });
                });
            }
        });

        layout.getChildren().addAll(titleLabel, listaUsuariosView, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            btnVoltar.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
        }

        carregarUsuarios();
        return scene;
    }

    private void carregarUsuarios() {
        ClienteSocket.getInstance().adminListarUsuarios((sucesso, dados, mensagem) -> {
            Platform.runLater(() -> {
                listaUsuariosView.getItems().clear();
                if (sucesso) {
                    if (dados != null && dados.isJsonArray() && dados.getAsJsonArray().size() > 0) {
                        for (JsonElement userElement : dados.getAsJsonArray()) {
                            JsonObject obj = userElement.getAsJsonObject();
                            if (obj.has("id") && obj.has("nome")) {
                                listaUsuariosView.getItems().add(
                                        new UsuarioItem(obj.get("id").getAsString(), obj.get("nome").getAsString())
                                );
                            } else {
                                System.err.println("Item de usuário inválido recebido: " + obj.toString());
                            }
                        }
                    } else {
                        AlertaUtil.mostrarInformacao("Usuários", mensagem != null ? mensagem : "Nenhum outro usuário encontrado.");
                    }
                } else {
                    AlertaUtil.mostrarErro("Erro ao Carregar Usuários", mensagem);
                }
            });
        });
    }
}