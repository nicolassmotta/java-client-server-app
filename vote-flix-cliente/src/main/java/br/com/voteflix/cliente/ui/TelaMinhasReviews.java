package br.com.voteflix.cliente.ui;

import br.com.voteflix.cliente.net.ClienteSocket;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
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
    }

    public TelaMinhasReviews(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setStyle("-fx-background-color: #000000; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 5);");

        Label titleLabel = new Label("Minhas Avaliações");
        titleLabel.getStyleClass().add("title-label");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-padding: 0;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoltar = new Button("Voltar ao Menu");
        btnVoltar.getStyleClass().add("secondary-button");
        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        topBar.getChildren().addAll(titleLabel, spacer, btnVoltar);
        root.setTop(topBar);

        VBox centerContent = new VBox(15);
        centerContent.setPadding(new Insets(30));

        listaReviewsView.setPlaceholder(new Label("Você ainda não avaliou nenhum filme."));
        VBox.setVgrow(listaReviewsView, Priority.ALWAYS);

        listaReviewsView.setCellFactory(p -> new ListCell<ReviewItem>() {
            private VBox card = new VBox(8);
            private Label titulo = new Label();
            private Label nota = new Label();
            private Label data = new Label();
            private Label desc = new Label();
            private HBox header = new HBox(10);

            {
                card.setPadding(new Insets(10));
                titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;");
                nota.setStyle("-fx-background-color: transparent; -fx-border-color: #E50914; -fx-text-fill: #E50914; -fx-padding: 2 6; -fx-border-radius: 4; -fx-font-weight: bold;");
                data.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
                desc.setStyle("-fx-text-fill: #ccc; -fx-font-style: italic;");
                desc.setWrapText(true);

                header.setAlignment(Pos.CENTER_LEFT);
                header.getChildren().addAll(titulo, nota);
                card.getChildren().addAll(header, data, desc);
            }

            @Override protected void updateItem(ReviewItem item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null) {
                    setText(null);
                    setGraphic(null);
                }
                else {
                    String edit = "true".equalsIgnoreCase(item.editado) ? " (Editado)" : "";
                    titulo.setText(item.tituloReview);
                    nota.setText(item.nota + "/5");
                    data.setText("Postado em: " + item.data + edit);
                    desc.setText("\"" + item.descricao + "\"");
                    setGraphic(card);
                }
            }
        });

        HBox actionsBar = new HBox(15);
        actionsBar.setAlignment(Pos.CENTER);
        actionsBar.setPadding(new Insets(10));

        Button btnEditar = new Button("Editar Selecionada");
        btnEditar.setPrefWidth(200);
        Button btnExcluir = new Button("Excluir Selecionada");
        btnExcluir.setPrefWidth(200);
        btnExcluir.getStyleClass().add("secondary-button");
        btnExcluir.setStyle("-fx-border-color: #E50914; -fx-text-fill: #E50914;");

        actionsBar.getChildren().addAll(btnEditar, btnExcluir);
        centerContent.getChildren().addAll(listaReviewsView, actionsBar);
        root.setCenter(centerContent);

        btnEditar.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Atenção", "Selecione uma review para editar."); return; }
            abrirDialogEditarReview(sel).ifPresent(json -> ClienteSocket.getInstance().enviarEditarReview(json.get("id").getAsString(), json.get("titulo").getAsString(), json.get("descricao").getAsString(), json.get("nota").getAsString(), (s, m) -> Platform.runLater(() -> { if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m); })));
        });

        btnExcluir.setOnAction(e -> {
            ReviewItem sel = listaReviewsView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Atenção", "Selecione uma review para excluir."); return; }
            if(AlertaUtil.mostrarConfirmacao("Excluir", "Tem certeza? Isso não pode ser desfeito.")) ClienteSocket.getInstance().enviarExcluirReview(sel.id, (s, m) -> Platform.runLater(() -> { if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarMinhasReviews(); } else AlertaUtil.mostrarErro("Erro", m); }));
        });

        Scene scene = new Scene(root, 900, 650);
        if(getClass().getResource("/styles.css") != null) scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        carregarMinhasReviews();
        return scene;
    }

    private Optional<JsonObject> abrirDialogEditarReview(ReviewItem reviewExistente) {
        Dialog<JsonObject> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setTitle("Editar Avaliação");
        dialog.setHeaderText("Modifique sua avaliação");
        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        ColumnConstraints col1 = new ColumnConstraints(); col1.setMinWidth(70);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField txtTitulo = new TextField(reviewExistente.tituloReview);
        TextArea txtDescricao = new TextArea(reviewExistente.descricao); txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>(); cbNota.getItems().addAll(1, 2, 3, 4, 5);
        try { cbNota.setValue(Integer.parseInt(reviewExistente.nota)); } catch (Exception e) { cbNota.getSelectionModel().selectFirst(); }

        Label lblT = new Label("Título:"); lblT.getStyleClass().add("label");
        Label lblN = new Label("Nota:"); lblN.getStyleClass().add("label");
        Label lblD = new Label("Descrição:"); lblD.getStyleClass().add("label");

        grid.add(lblT, 0, 0); grid.add(txtTitulo, 1, 0);
        grid.add(lblN, 0, 1); grid.add(cbNota, 1, 1);
        grid.add(lblD, 0, 2); grid.add(txtDescricao, 1, 2);
        GridPane.setVgrow(txtDescricao, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 400);
        Platform.runLater(txtTitulo::requestFocus);

        dialog.setResultConverter(btn -> {
            if(btn == btnSalvar) {
                String t = txtTitulo.getText().trim(), d = txtDescricao.getText().trim();
                Integer n = cbNota.getValue();
                if(t.isEmpty() || n == null || d.isEmpty()) return null;
                JsonObject j = new JsonObject();
                j.addProperty("id", reviewExistente.id); j.addProperty("titulo", t); j.addProperty("nota", n.toString()); j.addProperty("descricao", d);
                return j;
            }
            return null;
        });
        return dialog.showAndWait();
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