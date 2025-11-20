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
import javafx.scene.layout.ColumnConstraints;
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
            this.id = id; this.idFilme = idFilme; this.tituloReview = tituloReview; this.nota = nota; this.descricao = descricao; this.data = data; this.editado = editado;
        }
        @Override public String toString() {
            String edit = "true".equalsIgnoreCase(editado) ? " (Editado)" : "";
            return String.format("%s (Nota: %s) - %s%s", tituloReview, nota, data, edit);
        }
    }

    public TelaMinhasReviews(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private Optional<JsonObject> abrirDialogEditarReview(ReviewItem reviewExistente) {
        Dialog<JsonObject> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setTitle("Editar Avaliação");
        dialog.setHeaderText("Modifique sua avaliação");
        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(70);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField txtTitulo = new TextField(reviewExistente.tituloReview);
        TextArea txtDescricao = new TextArea(reviewExistente.descricao); txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>(); cbNota.getItems().addAll(1, 2, 3, 4, 5);
        try { cbNota.setValue(Integer.parseInt(reviewExistente.nota)); } catch (Exception e) { cbNota.getSelectionModel().selectFirst(); }

        grid.add(new Label("Título:"), 0, 0); grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Nota:"), 0, 1); grid.add(cbNota, 1, 1);
        grid.add(new Label("Descrição:"), 0, 2); grid.add(txtDescricao, 1, 2);
        GridPane.setVgrow(txtDescricao, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 300);
        Platform.runLater(txtTitulo::requestFocus);

        dialog.setResultConverter(btn -> {
            if(btn == btnSalvar) {
                String t = txtTitulo.getText().trim(), d = txtDescricao.getText().trim();
                Integer n = cbNota.getValue();
                if(t.isEmpty() || n == null || d.isEmpty()) { AlertaUtil.mostrarErro("Erro", "Preencha tudo."); return null; }
                if(t.length() > 50) { AlertaUtil.mostrarErro("Erro", "Título > 50 chars."); return null; }
                if(d.length() > 250) { AlertaUtil.mostrarErro("Erro", "Descrição > 250 chars."); return null; }
                JsonObject j = new JsonObject();
                j.addProperty("id", reviewExistente.id); j.addProperty("titulo", t); j.addProperty("nota", n.toString()); j.addProperty("descricao", d);
                return j;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    public Scene criarCena() {
        VBox layout = new VBox(15); layout.setPadding(new Insets(30)); layout.setAlignment(Pos.CENTER);
        Label lbl = new Label("Minhas Avaliações"); lbl.getStyleClass().add("title-label");
        VBox.setVgrow(listaReviewsView, Priority.ALWAYS);
        listaReviewsView.setPlaceholder(new Label("Você ainda não fez nenhuma review."));

        listaReviewsView.setCellFactory(p -> new ListCell<ReviewItem>() {
            private VBox c = new VBox(5); private Label t = new Label(), d = new Label(), dt = new Label();
            { t.getStyleClass().add("list-cell-review-titulo"); d.getStyleClass().add("list-cell-review-desc"); dt.setStyle("-fx-font-size: 10pt; -fx-text-fill: #888888;"); c.getChildren().addAll(t, dt, d); c.setPadding(new Insets(5)); }
            @Override protected void updateItem(ReviewItem item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) { setText(null); setGraphic(null); }
                else {
                    String edit = "true".equalsIgnoreCase(item.editado) ? " (Editado)" : "";
                    t.setText(item.tituloReview + " (Nota: " + item.nota + ")");
                    dt.setText("Data: " + item.data + edit);
                    d.setText("\"" + item.descricao + "\"");
                    setGraphic(c);
                }
            }
        });

        Button btnEditar = new Button("Editar Review"), btnExcluir = new Button("Excluir Review"), btnVoltar = new Button("Voltar");
        btnEditar.setPrefWidth(180); btnExcluir.setPrefWidth(180); btnVoltar.setPrefWidth(380);
        HBox crud = new HBox(10, btnEditar, btnExcluir); crud.setAlignment(Pos.CENTER);
        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        btnEditar.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione uma review."); return; }
            abrirDialogEditarReview(sel).ifPresent(json -> ClienteSocket.getInstance().enviarEditarReview(json.get("id").getAsString(), json.get("titulo").getAsString(), json.get("descricao").getAsString(), json.get("nota").getAsString(), (s, m) -> Platform.runLater(() -> { if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m); })));
        });

        btnExcluir.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione uma review."); return; }
            if(AlertaUtil.mostrarConfirmacao("Excluir", "Tem certeza?")) ClienteSocket.getInstance().enviarExcluirReview(sel.id, (s, m) -> Platform.runLater(() -> { if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m); }));
        });

        layout.getChildren().addAll(lbl, listaReviewsView, crud, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);
        if(getClass().getResource("/styles.css") != null) { scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); btnVoltar.getStyleClass().add("secondary-button"); }
        carregarMinhasReviews();
        return scene;
    }

    private void carregarMinhasReviews() {
        ClienteSocket.getInstance().enviarListarReviewsUsuario((s, d, m) -> Platform.runLater(() -> {
            listaReviewsView.getItems().clear();
            if(s && d.isJsonArray()) {
                for(JsonElement el : d.getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    String dt = o.has("data") ? o.get("data").getAsString() : "", ed = o.has("editado") ? o.get("editado").getAsString() : "false";
                    listaReviewsView.getItems().add(new ReviewItem(o.get("id").getAsString(), o.get("id_filme").getAsString(), o.get("titulo").getAsString(), o.get("nota").getAsString(), o.get("descricao").getAsString(), dt, ed));
                }
            } else AlertaUtil.mostrarErro("Erro", m);
        }));
    }
}