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

    // Classe interna para facilitar a exibição (mantida)
    private static class UsuarioItem {
        String id;
        String nome; // Corresponde ao 'login' no servidor/DAO

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

        // --- AÇÕES ---

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaAdminMenu());

        // Ação Editar Senha
        btnEditarSenha.setOnAction(e -> {
            UsuarioItem selecionado = listaUsuariosView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um usuário para editar a senha.");
                return;
            }
            // Não permitir editar a senha do admin por esta tela (opcional, mas recomendado)
            if ("admin".equals(selecionado.nome)) {
                AlertaUtil.mostrarErro("Aviso", "A senha do usuário 'admin' não pode ser alterada por aqui.");
                return;
            }


            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Editar Senha");
            dialog.setHeaderText("Editando usuário: " + selecionado.nome);
            dialog.setContentText("Nova Senha:");

            dialog.showAndWait().ifPresent(novaSenha -> {
                if (novaSenha.trim().isEmpty()) {
                    AlertaUtil.mostrarErro("Erro", "A nova senha não pode estar em branco.");
                    return;
                }
                // Validação de senha no cliente
                if (novaSenha.length() < 3 || novaSenha.length() > 20 || !novaSenha.matches("^[a-zA-Z0-9]+$")) {
                    AlertaUtil.mostrarErro("Erro de Validação", "Senha inválida. Use 3-20 caracteres (apenas letras e números).");
                    return;
                }


                // Callback: (Boolean sucesso, String mensagem)
                ClienteSocket.getInstance().adminEditarUsuario(selecionado.id, novaSenha, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem); // Mensagem do servidor
                        } else {
                            AlertaUtil.mostrarErro("Erro na Edição", mensagem); // Mensagem do servidor
                        }
                        // Não recarrega a lista aqui, pois a senha não é visível
                    });
                });
            });
        });

        // Ação Excluir Usuário
        btnExcluir.setOnAction(e -> {
            UsuarioItem selecionado = listaUsuariosView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um usuário para excluir.");
                return;
            }
            // Proibir exclusão do admin
            if ("admin".equals(selecionado.nome)) {
                AlertaUtil.mostrarErro("Erro", "Você não pode excluir o usuário 'admin'.");
                return;
            }

            boolean confirmado = AlertaUtil.mostrarConfirmacao("Excluir Usuário", "Tem certeza que deseja excluir o usuário '" + selecionado.nome + "'? Esta ação também excluirá as reviews deste usuário."); // Mensagem atualizada
            if (confirmado) {
                // Callback: (Boolean sucesso, String mensagem)
                ClienteSocket.getInstance().adminExcluirUsuario(selecionado.id, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem); // Mensagem do servidor
                            carregarUsuarios(); // Atualiza a lista após exclusão
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Excluir", mensagem); // Mensagem do servidor
                        }
                    });
                });
            }
        });

        // Layout
        layout.getChildren().addAll(titleLabel, listaUsuariosView, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);

        // CSS
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            btnVoltar.getStyleClass().add("secondary-button");
            // Estilos opcionais para botões CRUD
            // btnEditarSenha.getStyleClass().add("secondary-button");
            // btnExcluir.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
        }

        carregarUsuarios(); // Carrega os dados ao criar a cena
        return scene;
    }

    // Método para carregar a lista de usuários
    private void carregarUsuarios() {
        // Callback: (Boolean sucesso, JsonElement dados, String mensagem)
        ClienteSocket.getInstance().adminListarUsuarios((sucesso, dados, mensagem) -> {
            Platform.runLater(() -> {
                listaUsuariosView.getItems().clear(); // Limpa a lista
                if (sucesso) {
                    // Verifica se 'dados' é um JsonArray não nulo e com elementos
                    if (dados != null && dados.isJsonArray() && dados.getAsJsonArray().size() > 0) {
                        for (JsonElement userElement : dados.getAsJsonArray()) {
                            JsonObject obj = userElement.getAsJsonObject();
                            // O servidor retorna "id" e "login". Usamos "login" como "nome" no Item.
                            if (obj.has("id") && obj.has("nome")) { // Verifica se as chaves existem
                                listaUsuariosView.getItems().add(
                                        new UsuarioItem(obj.get("id").getAsString(), obj.get("nome").getAsString())
                                );
                            } else {
                                System.err.println("Item de usuário inválido recebido: " + obj.toString());
                            }
                        }
                    } else {
                        // Caso de sucesso, mas sem usuários (além do admin talvez)
                        // Usa a mensagem do servidor se disponível
                        AlertaUtil.mostrarInformacao("Usuários", mensagem != null ? mensagem : "Nenhum outro usuário encontrado.");
                    }
                } else {
                    // Caso de falha (erro 500, 403 etc.)
                    AlertaUtil.mostrarErro("Erro ao Carregar Usuários", mensagem); // Mensagem do servidor
                }
            });
        });
    }
}