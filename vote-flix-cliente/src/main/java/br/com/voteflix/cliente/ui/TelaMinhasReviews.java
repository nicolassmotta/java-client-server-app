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
        String id, idFilme, tituloReview, nota, descricao, data, editado;

        public ReviewItem(String id, String idFilme, String tituloReview, String nota, String descricao, String data, String editado) {
            this.id = id;
            this.idFilme = idFilme;
            this.tituloReview = tituloReview;
            this.nota = nota;
            this.descricao = descricao;
            this.data = data;
            this.editado = editado;
        }
        @Override
        public String toString() {
            String edit = "true".equalsIgnoreCase(editado) ? " (Editado)" : "";
            return String.format("%s (Nota: %s) - %s%s", tituloReview, nota, data, edit);
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
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField txtTitulo = new TextField(reviewExistente.tituloReview);
        txtTitulo.setPromptText("Título da sua avaliação (até 50 chars)");
        TextArea txtDescricao = new TextArea(reviewExistente.descricao);
        txtDescricao.setPromptText("Sua opinião sobre o filme... (até 250 chars)");
        txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>();
        cbNota.getItems().addAll(1, 2, 3, 4, 5);
        try { cbNota.setValue(Integer.parseInt(reviewExistente.nota)); } catch (Exception e) { cbNota.getSelectionModel().selectFirst(); }
        grid.add(new Label("Título:"), 0, 0); grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Nota:"), 0, 1); grid.add(cbNota, 1, 1);
        grid.add(new Label("Descrição:"), 0, 2); grid.add(txtDescricao, 1, 2);
        GridPane.setVgrow(txtDescricao, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 300);
        Platform.runLater(txtTitulo::requestFocus);
        dialog.setResultConverter(btn -> {
            if(btn == salvarButtonType) {
                String t = txtTitulo.getText().trim(), d = txtDescricao.getText().trim();
                Integer n = cbNota.getValue();
                if(t.isEmpty() || n == null || d.isEmpty()) { AlertaUtil.mostrarErro("Erro", "Campos obrigatórios."); return null; }
                if(t.length() > 50) { AlertaUtil.mostrarErro("Erro", "Título > 50 chars."); return null; }
                if(d.length() > 250) { AlertaUtil.mostrarErro("Erro", "Descrição > 250 chars."); return null; }
                JsonObject j = new JsonObject();
                j.addProperty("id", reviewExistente.id); j.addProperty("titulo", t);
                j.addProperty("nota", n.toString()); j.addProperty("descricao", d);
                return j;
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
        titleLabel.getStyleClass().add("title-label");

        VBox.setVgrow(listaReviewsView, Priority.ALWAYS);
        listaReviewsView.setPlaceholder(new Label("Você ainda não fez nenhuma review."));

        listaReviewsView.setCellFactory(param -> new ListCell<ReviewItem>() {
            private VBox content = new VBox(5);
            private Label lblTitulo = new Label();
            private Label lblDescricao = new Label();
            private Label lblData = new Label();
            {
                lblTitulo.getStyleClass().add("list-cell-review-titulo");
                lblDescricao.getStyleClass().add("list-cell-review-desc");
                lblData.setStyle("-fx-font-size: 10pt; -fx-text-fill: #888888;");
                content.getChildren().addAll(lblTitulo, lblData, lblDescricao);
                content.setPadding(new Insets(5));
            }
            @Override
            protected void updateItem(ReviewItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                } else {
                    String edit = "true".equalsIgnoreCase(item.editado) ? " (Editado)" : "";
                    lblTitulo.setText(item.tituloReview + " (Nota: " + item.nota + ")");
                    lblData.setText("Data: " + item.data + edit);
                    lblDescricao.setText("\"" + item.descricao + "\"");
                    setGraphic(content);
                }
            }
        });

        Button btnEditar = new Button("Editar Review");
        Button btnExcluir = new Button("Excluir Review");
        Button btnVoltar = new Button("Voltar");

        btnEditar.setPrefWidth(180); btnExcluir.setPrefWidth(180); btnVoltar.setPrefWidth(380);
        HBox crudButtons = new HBox(10, btnEditar, btnExcluir);
        crudButtons.setAlignment(Pos.CENTER);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        btnEditar.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione uma review."); return; }
            abrirDialogEditarReview(sel).ifPresent(json -> {
                ClienteSocket.getInstance().enviarEditarReview(json.get("id").getAsString(), json.get("titulo").getAsString(), json.get("descricao").getAsString(), json.get("nota").getAsString(), (s, m) -> Platform.runLater(() -> {
                    if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            });
        });

        btnExcluir.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione uma review."); return; }
            if(AlertaUtil.mostrarConfirmacao("Excluir", "Tem certeza?")) {
                ClienteSocket.getInstance().enviarExcluirReview(sel.id, (s, m) -> Platform.runLater(() -> {
                    if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            }
        });

        layout.getChildren().addAll(titleLabel, listaReviewsView, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);
        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            btnVoltar.getStyleClass().add("secondary-button");
        }
        carregarMinhasReviews();
        return scene;
    }

    private void carregarMinhasReviews() {
        ClienteSocket.getInstance().enviarListarReviewsUsuario((sucesso, dados, mensagem) -> {
            Platform.runLater(() -> {
                listaReviewsView.getItems().clear();
                if (sucesso) {
                    if (dados != null && dados.isJsonArray()) {
                        for (JsonElement el : dados.getAsJsonArray()) {
                            JsonObject obj = el.getAsJsonObject();
                            String dataStr = obj.has("data") ? obj.get("data").getAsString() : "";
                            String editadoStr = obj.has("editado") ? obj.get("editado").getAsString() : "false";
                            listaReviewsView.getItems().add(new ReviewItem(
                                    obj.get("id").getAsString(), obj.get("id_filme").getAsString(),
                                    obj.get("titulo").getAsString(), obj.get("nota").getAsString(),
                                    obj.get("descricao").getAsString(), dataStr, editadoStr
                            ));
                        }
                    }
                } else {
                    AlertaUtil.mostrarErro("Erro", mensagem);
                }
            });
        });
    }
}