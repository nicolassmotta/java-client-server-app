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
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TelaAdminFilmes {

    private SceneManager sceneManager;
    private ListView<FilmeItem> listaFilmesView = new ListView<>();
    private List<FilmeItem> masterList = new ArrayList<>();
    private ComboBox<String> filtroGenero = new ComboBox<>();

    private final List<String> GENEROS_PRE_CADASTRADOS = Arrays.asList(
            "Ação", "Aventura", "Comédia", "Drama", "Fantasia", "Ficção Científica",
            "Terror", "Romance", "Documentário", "Musical", "Animação"
    );

    private static class FilmeItem {
        String id, titulo, nota, qtdAvaliacoes, diretor, ano, sinopse;
        List<String> generos;
        public FilmeItem(String id, String titulo, String nota, String qtdAvaliacoes, List<String> generos, String diretor, String ano, String sinopse) {
            this.id = id; this.titulo = titulo; this.nota = nota; this.qtdAvaliacoes = qtdAvaliacoes;
            this.generos = generos; this.diretor = diretor; this.ano = ano; this.sinopse = sinopse;
        }
        @Override public String toString() { return titulo; }
    }

    public TelaAdminFilmes(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setStyle("-fx-background-color: #000000; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 5);");

        Label logo = new Label("VOTEFLIX ADMIN");
        logo.setStyle("-fx-font-size: 20px; -fx-padding: 0; -fx-text-fill: #E50914; -fx-font-weight: 900;");

        Button btnVoltar = new Button("Voltar ao Menu");
        btnVoltar.getStyleClass().add("secondary-button");
        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaAdminMenu());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(logo, spacer, btnVoltar);
        root.setTop(topBar);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Gerenciamento de Filmes");
        title.getStyleClass().add("subtitle-label");

        filtroGenero.getItems().add("Todos os Gêneros");
        filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS);
        filtroGenero.setValue("Todos os Gêneros");

        listaFilmesView.setCellFactory(p -> new ListCell<FilmeItem>() {
            @Override protected void updateItem(FilmeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.titulo + " (" + item.ano + ")");
                }
            }
        });

        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);

        HBox crudBar = new HBox(10);
        crudBar.setAlignment(Pos.CENTER);
        Button btnCriar = new Button("Novo Filme");
        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().add("secondary-button");
        Button btnExcluir = new Button("Excluir");
        btnExcluir.getStyleClass().add("secondary-button");
        btnExcluir.setStyle("-fx-border-color: #E50914; -fx-text-fill: #E50914;");
        Button btnReviews = new Button("Gerenciar Reviews");
        btnReviews.getStyleClass().add("secondary-button");

        crudBar.getChildren().addAll(btnCriar, btnEditar, btnExcluir, btnReviews);

        content.getChildren().addAll(title, filtroGenero, listaFilmesView, crudBar);
        root.setCenter(content);

        btnCriar.setOnAction(e -> abrirDialogFilme(null).ifPresent(j -> ClienteSocket.getInstance().adminCriarFilme(j, (s, m) -> Platform.runLater(() -> { if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m); }))));

        btnEditar.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            JsonObject j = new JsonObject(); j.addProperty("id", sel.id); j.addProperty("titulo", sel.titulo); j.addProperty("diretor", sel.diretor); j.addProperty("ano", sel.ano); j.addProperty("sinopse", sel.sinopse);
            JsonArray ga = new JsonArray(); sel.generos.forEach(ga::add); j.add("genero", ga);
            abrirDialogFilme(j).ifPresent(res -> ClienteSocket.getInstance().adminEditarFilme(res, (s, m) -> Platform.runLater(() -> { if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m); })));
        });

        btnExcluir.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            if(AlertaUtil.mostrarConfirmacao("Excluir", "Tem certeza?")) ClienteSocket.getInstance().adminExcluirFilme(sel.id, (s, m) -> Platform.runLater(() -> { if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m); }));
        });

        btnReviews.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if(sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            abrirDialogGerenciarReviews(sel);
        });

        filtroGenero.setOnAction(e -> atualizarListaExibida());

        Scene scene = new Scene(root, 900, 650);
        if(getClass().getResource("/styles.css") != null) scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        carregarFilmes();
        return scene;
    }

    private Optional<JsonObject> abrirDialogFilme(JsonObject filmeExistente) {
        boolean modoEdicao = filmeExistente != null;
        Dialog<JsonObject> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        dialog.setTitle(modoEdicao ? "Editar Filme" : "Criar Novo Filme");
        dialog.setHeaderText(modoEdicao ? "Altere os dados do filme" : "Preencha os dados do novo filme");

        ButtonType btnSalvar = new ButtonType(modoEdicao ? "Salvar" : "Criar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, btnCancelar);

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(15); grid.setPadding(new Insets(30));

        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(75);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField txtTitulo = new TextField(); txtTitulo.setPromptText("Título do Filme");
        TextField txtDiretor = new TextField(); txtDiretor.setPromptText("Nome do Diretor");
        TextField txtAno = new TextField(); txtAno.setPromptText("Ano de Lançamento");
        TextArea txtSinopse = new TextArea(); txtSinopse.setPromptText("Sinopse detalhada...");
        txtSinopse.setWrapText(true);

        Label lblTitulo = new Label("Título:"); lblTitulo.getStyleClass().add("label");
        Label lblDiretor = new Label("Diretor:"); lblDiretor.getStyleClass().add("label");
        Label lblAno = new Label("Ano:"); lblAno.getStyleClass().add("label");
        Label lblSinopse = new Label("Sinopse:"); lblSinopse.getStyleClass().add("label");
        Label lblGeneros = new Label("Gêneros:"); lblGeneros.getStyleClass().add("label");

        grid.add(lblTitulo, 0, 0); grid.add(txtTitulo, 1, 0);
        grid.add(lblDiretor, 0, 1); grid.add(txtDiretor, 1, 1);
        grid.add(lblAno, 0, 2); grid.add(txtAno, 1, 2);
        grid.add(lblSinopse, 0, 3); grid.add(txtSinopse, 1, 3);

        VBox generosBox = new VBox(5);
        generosBox.setPadding(new Insets(10));
        generosBox.setStyle("-fx-background-color: #2b2b2b;");
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String genero : GENEROS_PRE_CADASTRADOS) {
            CheckBox cb = new CheckBox(genero);
            checkBoxes.add(cb); generosBox.getChildren().add(cb);
        }

        ScrollPane scrollGeneros = new ScrollPane(generosBox);
        scrollGeneros.setFitToWidth(true);
        scrollGeneros.setPrefHeight(150);

        grid.add(lblGeneros, 0, 4); grid.add(scrollGeneros, 1, 4);

        if (modoEdicao) {
            txtTitulo.setText(filmeExistente.get("titulo").getAsString());
            txtDiretor.setText(filmeExistente.get("diretor").getAsString());
            txtAno.setText(filmeExistente.get("ano").getAsString());
            txtSinopse.setText(filmeExistente.get("sinopse").getAsString());
            if (filmeExistente.has("genero")) {
                JsonArray gens = filmeExistente.getAsJsonArray("genero");
                Set<String> set = new HashSet<>();
                gens.forEach(g -> set.add(g.getAsString()));
                checkBoxes.forEach(cb -> { if(set.contains(cb.getText())) cb.setSelected(true); });
            }
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(800, 700);

        dialog.setResultConverter(btn -> {
            if (btn == btnSalvar) {
                String t = txtTitulo.getText(), d = txtDiretor.getText(), a = txtAno.getText(), s = txtSinopse.getText();
                List<String> gList = checkBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
                JsonObject j = new JsonObject();
                if(modoEdicao) j.addProperty("id", filmeExistente.get("id").getAsString());
                j.addProperty("titulo", t); j.addProperty("diretor", d); j.addProperty("ano", a); j.addProperty("sinopse", s);
                JsonArray ga = new JsonArray(); gList.forEach(ga::add); j.add("genero", ga);
                return j;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void abrirDialogGerenciarReviews(FilmeItem filme) {
        Dialog<Void> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setTitle("Reviews: " + filme.titulo);
        dialog.setHeaderText("Detalhes Completos para Administração");

        ListView<JsonObject> listaReviews = new ListView<>();
        listaReviews.setCellFactory(param -> new ListCell<JsonObject>() {
            @Override
            protected void updateItem(JsonObject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String autor = item.get("nome_usuario").getAsString();
                    String nota = item.get("nota").getAsString();
                    String data = item.has("data") ? item.get("data").getAsString() : "Data N/A";
                    String editado = item.has("editado") && item.get("editado").getAsString().equalsIgnoreCase("true") ? "SIM" : "NÃO";
                    String titulo = item.get("titulo").getAsString();
                    String desc = item.get("descricao").getAsString();

                    VBox box = new VBox(5);
                    box.setStyle("-fx-padding: 10; -fx-border-color: #444; -fx-border-radius: 5;");

                    Label header = new Label(String.format("Autor: %s | Nota: %s | Data: %s | Editado: %s", autor, nota, data, editado));
                    header.setStyle("-fx-text-fill: #E50914; -fx-font-weight: bold; -fx-font-size: 14px;");

                    Label body = new Label(String.format("Título: %s\nReview: %s", titulo, desc));
                    body.setStyle("-fx-text-fill: white;");
                    body.setWrapText(true);

                    box.getChildren().addAll(header, body);
                    setGraphic(box);
                }
            }
        });

        Button btnApagar = new Button("Apagar Selecionada");
        btnApagar.setStyle("-fx-background-color: #E50914; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        HBox bottomBox = new HBox(btnApagar);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(15, 0, 0, 0));

        ClienteSocket.getInstance().enviarBuscarFilmePorId(filme.id, (sucesso, dados, msg) -> {
            Platform.runLater(() -> {
                listaReviews.getItems().clear();
                if (sucesso && dados != null && dados.isJsonObject()) {
                    JsonObject obj = dados.getAsJsonObject();
                    if (obj.has("reviews")) {
                        JsonArray arr = obj.getAsJsonArray("reviews");
                        for (JsonElement el : arr) {
                            listaReviews.getItems().add(el.getAsJsonObject());
                        }
                    }
                }
                if (listaReviews.getItems().isEmpty()) {
                    listaReviews.setPlaceholder(new Label("Sem avaliações."));
                }
            });
        });

        btnApagar.setOnAction(e -> {
            JsonObject sel = listaReviews.getSelectionModel().getSelectedItem();
            if (sel != null) {
                String idR = sel.get("id").getAsString();
                if (AlertaUtil.mostrarConfirmacao("Apagar", "Tem certeza?")) {
                    ClienteSocket.getInstance().enviarExcluirReview(idR, (s, m) -> Platform.runLater(() -> {
                        if (s) {
                            AlertaUtil.mostrarInformacao("Sucesso", "Review apagada.");
                            dialog.close();
                        }
                        else AlertaUtil.mostrarErro("Erro", m);
                    }));
                }
            } else {
                AlertaUtil.mostrarErro("Erro", "Selecione uma review para apagar.");
            }
        });

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(listaReviews);
        mainLayout.setBottom(bottomBox);
        mainLayout.setPadding(new Insets(10));

        mainLayout.setPrefSize(900, 700);

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().setPrefSize(900, 700);

        ButtonType btnFechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnFechar);

        dialog.showAndWait();
    }

    private void carregarFilmes() {
        masterList.clear(); listaFilmesView.getItems().clear();
        ClienteSocket.getInstance().enviarListarFilmes((s, d, m) -> Platform.runLater(() -> {
            if (s && d.isJsonArray()) {
                for (JsonElement el : d.getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    List<String> gl = new ArrayList<>();
                    if(o.has("genero")) o.getAsJsonArray("genero").forEach(x -> gl.add(x.getAsString()));
                    masterList.add(new FilmeItem(
                            o.get("id").getAsString(), o.get("titulo").getAsString(),
                            o.has("nota") ? o.get("nota").getAsString() : "0", "0", gl,
                            o.has("diretor") ? o.get("diretor").getAsString() : "",
                            o.has("ano") ? o.get("ano").getAsString() : "",
                            o.has("sinopse") ? o.get("sinopse").getAsString() : ""
                    ));
                }
                atualizarListaExibida();
            }
        }));
    }

    private void atualizarListaExibida() {
        List<FilmeItem> filtrada = new ArrayList<>();
        String gen = filtroGenero.getValue();
        if (gen == null || "Todos os Gêneros".equals(gen)) filtrada.addAll(masterList);
        else for (FilmeItem i : masterList) if (i.generos.contains(gen)) filtrada.add(i);
        listaFilmesView.getItems().setAll(filtrada);
    }
}
