package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ListCell;
import java.net.URL;
import java.util.Optional;

public class TelaMinhasReviews {

    private SceneManager sceneManager;
    private ListView<ReviewItem> listaReviewsView = new ListView<>();

    private static class ReviewItem {
        String id;
        String idFilme;
        String tituloReview;
        String nota;
        String descricao;

        public ReviewItem(String id, String idFilme, String tituloReview, String nota, String descricao) {
            this.id = id;
            this.idFilme = idFilme;
            this.tituloReview = tituloReview;
            this.nota = nota;
            this.descricao = descricao;
        }

        @Override
        public String toString() {
            // Exibição simplificada sem o título do filme
            return "\"" + tituloReview + "\" (Nota: " + nota + ")";
        }
    }

    public TelaMinhasReviews(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private Optional<JsonObject> abrirDialogEditarReview(ReviewItem reviewExistente) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Editar Avaliação");
        dialog.setHeaderText("Modifique sua avaliação");

        ButtonType salvarButtonType = new ButtonType("Salvar Alterações", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtTitulo = new TextField(reviewExistente.tituloReview);
        txtTitulo.setPromptText("Título da sua avaliação (até 50 chars)");
        TextArea txtDescricao = new TextArea(reviewExistente.descricao);
        txtDescricao.setPromptText("Sua opinião sobre o filme... (até 250 chars)");
        txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>();
        cbNota.getItems().addAll(1, 2, 3, 4, 5);
        try {
            cbNota.setValue(Integer.parseInt(reviewExistente.nota)); // Preenche a nota atual
        } catch (NumberFormatException e) {
            System.err.println("Nota inválida no ReviewItem: " + reviewExistente.nota);
            cbNota.getSelectionModel().selectFirst(); // Seleciona a primeira nota como fallback
        }
        cbNota.setPromptText("Nota (1-5)");

        grid.add(new Label("Título:"), 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Nota:"), 0, 1);
        grid.add(cbNota, 1, 1);
        grid.add(new Label("Descrição:"), 0, 2);
        grid.add(txtDescricao, 1, 2);
        GridPane.setVgrow(txtDescricao, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 300);
        Platform.runLater(txtTitulo::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                // Validações
                String titulo = txtTitulo.getText().trim();
                String descricao = txtDescricao.getText().trim();
                Integer nota = cbNota.getValue();

                if (titulo.isEmpty() || nota == null || descricao.isEmpty()) {
                    AlertaUtil.mostrarErro("Erro de Validação", "Todos os campos são obrigatórios.");
                    return null; // Mantém o dialog aberto
                }
                if (titulo.length() > 50) {
                    AlertaUtil.mostrarErro("Erro de Validação", "O título excede o limite de 50 caracteres.");
                    return null;
                }
                if (descricao.length() > 250) {
                    AlertaUtil.mostrarErro("Erro de Validação", "A descrição excede o limite de 250 caracteres.");
                    return null;
                }

                JsonObject reviewJson = new JsonObject();
                reviewJson.addProperty("id", reviewExistente.id); // ID da review sendo editada
                reviewJson.addProperty("titulo", titulo);
                reviewJson.addProperty("nota", nota.toString()); // Envia nota como String
                reviewJson.addProperty("descricao", descricao);
                return reviewJson;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Minhas Avaliações");
        VBox.setVgrow(listaReviewsView, Priority.ALWAYS);
        listaReviewsView.setPlaceholder(new Label("Você ainda não fez nenhuma review."));

        // --- NOVA CÉLULA CUSTOMIZADA ---
        listaReviewsView.setCellFactory(param -> new ListCell<ReviewItem>() {
            private VBox content = new VBox(5);
            private Label lblTitulo = new Label();
            private Label lblDescricao = new Label();

            {
                // *** MUDANÇA AQUI: Usando classes CSS ***
                lblTitulo.getStyleClass().add("list-cell-review-titulo");
                lblDescricao.getStyleClass().add("list-cell-review-desc");
                // *** FIM DA MUDANÇA ***

                lblDescricao.setWrapText(true);
                content.getChildren().addAll(lblTitulo, lblDescricao);
                content.setPadding(new Insets(5)); // Espaçamento interno da célula
            }

            @Override
            protected void updateItem(ReviewItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Preenche os labels com os dados do ReviewItem
                    lblTitulo.setText(item.tituloReview + " (Nota: " + item.nota + ")");
                    lblDescricao.setText("\"" + item.descricao + "\"");
                    setGraphic(content);
                }
            }
        });

        listaReviewsView.setPlaceholder(new Label("Você ainda não fez nenhuma review."));

        Button btnEditar = new Button("Editar Review");
        btnEditar.setPrefWidth(180);
        Button btnExcluir = new Button("Excluir Review");
        btnExcluir.setPrefWidth(180);

        HBox crudButtons = new HBox(10, btnEditar, btnExcluir);
        crudButtons.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setPrefWidth(380);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        btnEditar.setOnAction(e -> {
            ReviewItem selecionada = listaReviewsView.getSelectionModel().getSelectedItem();
            if (selecionada == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione uma review para editar.");
                return;
            }
            abrirDialogEditarReview(selecionada).ifPresent(reviewJson -> {
                String idReview = reviewJson.get("id").getAsString();
                String titulo = reviewJson.get("titulo").getAsString();
                String descricao = reviewJson.get("descricao").getAsString();
                String nota = reviewJson.get("nota").getAsString();

                ClienteSocket.getInstance().enviarEditarReview(idReview, titulo, descricao, nota, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem); // Mantém o alerta para confirmação de EDIÇÃO
                            carregarMinhasReviews(); // Atualiza a lista
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Editar", mensagem); // Mensagem do servidor
                        }
                    });
                });
            });
        });

        btnExcluir.setOnAction(e -> {
            ReviewItem selecionada = listaReviewsView.getSelectionModel().getSelectedItem();
            if (selecionada == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione uma review para excluir.");
                return;
            }

            boolean confirmado = AlertaUtil.mostrarConfirmacao("Excluir Review", "Tem certeza que deseja excluir sua review '" + selecionada.tituloReview + "'?");
            if (confirmado) {
                ClienteSocket.getInstance().enviarExcluirReview(selecionada.id, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem); // Mantém o alerta para confirmação de EXCLUSÃO
                            carregarMinhasReviews(); // Atualiza a lista
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Excluir", mensagem); // Mensagem do servidor
                        }
                    });
                });
            }
        });

        // Layout
        layout.getChildren().addAll(titleLabel, listaReviewsView, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600); // Tamanho padronizado

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            btnVoltar.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
        }

        carregarMinhasReviews(); // Carrega os dados ao criar a cena
        return scene;
    }

    private void carregarMinhasReviews() {
        ClienteSocket.getInstance().enviarListarReviewsUsuario((sucesso, dados, mensagem) -> {
            Platform.runLater(() -> {
                listaReviewsView.getItems().clear(); // Limpa a lista
                if (sucesso) {
                    if (dados != null && dados.isJsonArray() && !dados.getAsJsonArray().isEmpty()) {
                        JsonArray reviewsArray = dados.getAsJsonArray();
                        for (JsonElement reviewElement : reviewsArray) {
                            JsonObject obj = reviewElement.getAsJsonObject();

                            if (obj.has("id") && obj.has("id_filme") && obj.has("titulo") && obj.has("nota") && obj.has("descricao")) {
                                listaReviewsView.getItems().add(
                                        new ReviewItem(
                                                obj.get("id").getAsString(),
                                                obj.get("id_filme").getAsString(),
                                                obj.get("titulo").getAsString(),
                                                obj.get("nota").getAsString(),
                                                obj.get("descricao").getAsString()
                                        )
                                );
                            } else {
                                System.err.println("Item de review inválido recebido (verificar chaves JSON): " + obj.toString());
                            }
                        }
                    } else if (dados == null || (dados.isJsonArray() && dados.getAsJsonArray().size() == 0) || (dados.isJsonObject() && dados.getAsJsonObject().size() == 0) ) {
                        System.out.println("Nenhuma review encontrada para o usuário."); // Log opcional
                    } else {
                        System.err.println("Resposta inesperada ao listar reviews: " + (dados != null ? dados.toString() : "null")); // Log de erro
                    }
                } else {
                    AlertaUtil.mostrarErro("Erro ao Carregar Reviews", mensagem); // Mensagem do servidor
                }
            });
        });
    }
}