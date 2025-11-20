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
import java.net.URL;
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
    private Button btnAnterior = new Button("<< Anterior");
    private Button btnProximo = new Button("Próximo >>");
    private Label lblPagina = new Label("Página 1 de 1");
    private HBox painelPaginacao; // Variável declarada aqui para evitar o erro
    private static final int ITENS_POR_PAGINA = 10;
    private int paginaAtual = 0;

    private final List<String> GENEROS_PRE_CADASTRADOS = Arrays.asList(
            "Ação", "Aventura", "Comédia", "Drama", "Fantasia", "Ficção Científica",
            "Terror", "Romance", "Documentário", "Musical", "Animação"
    );

    private static class FilmeItem {
        String id, titulo, nota, qtdAvaliacoes, diretor, ano, sinopse;
        List<String> generos;

        public FilmeItem(String id, String titulo, String nota, String qtdAvaliacoes, List<String> generos, String diretor, String ano, String sinopse) {
            this.id = id;
            this.titulo = titulo;
            this.nota = nota;
            this.qtdAvaliacoes = qtdAvaliacoes;
            this.generos = generos;
            this.diretor = diretor;
            this.ano = ano;
            this.sinopse = sinopse;
        }
        @Override
        public String toString() { return titulo; }
    }

    public TelaAdminFilmes(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private Optional<JsonObject> abrirDialogFilme(JsonObject filmeExistente) {
        boolean modoEdicao = filmeExistente != null;
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle(modoEdicao ? "Editar Filme" : "Criar Novo Filme");
        dialog.setHeaderText(modoEdicao ? "Altere os dados do filme" : "Preencha os dados do novo filme");
        ButtonType btnTipoSalvar = new ButtonType(modoEdicao ? "Salvar Alterações" : "Criar Filme", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTipoSalvar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título do filme (3-30 chars)");
        TextField txtDiretor = new TextField();
        txtDiretor.setPromptText("Nome do diretor (3-30 chars)");
        TextField txtAno = new TextField();
        txtAno.setPromptText("Ano de lançamento (3-4 dígitos)");
        TextArea txtSinopse = new TextArea();
        txtSinopse.setPromptText("Breve sinopse do filme (max 250 chars)");
        txtSinopse.setWrapText(true);

        grid.add(new Label("Título:"), 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Diretor:"), 0, 1);
        grid.add(txtDiretor, 1, 1);
        grid.add(new Label("Ano:"), 0, 2);
        grid.add(txtAno, 1, 2);
        grid.add(new Label("Sinopse:"), 0, 3);
        grid.add(txtSinopse, 1, 3);
        GridPane.setVgrow(txtSinopse, Priority.ALWAYS);

        VBox generosBox = new VBox(5);
        generosBox.setPadding(new Insets(5));
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String genero : GENEROS_PRE_CADASTRADOS) {
            CheckBox cb = new CheckBox(genero);
            checkBoxes.add(cb);
            generosBox.getChildren().add(cb);
        }
        ScrollPane scrollGeneros = new ScrollPane(generosBox);
        scrollGeneros.setFitToWidth(true);
        scrollGeneros.setPrefHeight(100);
        grid.add(new Label("Gêneros:"), 0, 4);
        grid.add(scrollGeneros, 1, 4);

        if (modoEdicao) {
            txtTitulo.setText(filmeExistente.has("titulo") ? filmeExistente.get("titulo").getAsString() : "");
            txtDiretor.setText(filmeExistente.has("diretor") ? filmeExistente.get("diretor").getAsString() : "");
            txtAno.setText(filmeExistente.has("ano") ? filmeExistente.get("ano").getAsString() : "");
            txtSinopse.setText(filmeExistente.has("sinopse") ? filmeExistente.get("sinopse").getAsString() : "");

            if (filmeExistente.has("genero") && filmeExistente.get("genero").isJsonArray()) {
                JsonArray generosDoFilme = filmeExistente.getAsJsonArray("genero");
                Set<String> generosSet = new HashSet<>();
                for (JsonElement gen : generosDoFilme) {
                    generosSet.add(gen.getAsString());
                }
                for (CheckBox cb : checkBoxes) {
                    if (generosSet.contains(cb.getText())) {
                        cb.setSelected(true);
                    }
                }
            }
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(500);
        Platform.runLater(txtTitulo::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnTipoSalvar) {
                String titulo = txtTitulo.getText();
                String diretor = txtDiretor.getText();
                String anoStr = txtAno.getText();
                String sinopse = txtSinopse.getText();
                List<String> generosSelecionadosList = checkBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
                JsonObject filmeJson = new JsonObject();
                if (modoEdicao) filmeJson.addProperty("id", filmeExistente.get("id").getAsString());
                filmeJson.addProperty("titulo", titulo);
                filmeJson.addProperty("diretor", diretor);
                filmeJson.addProperty("ano", anoStr);
                filmeJson.addProperty("sinopse", sinopse);
                JsonArray generosJsonArray = new JsonArray();
                for (String genero : generosSelecionadosList) generosJsonArray.add(genero);
                filmeJson.add("genero", generosJsonArray);
                return filmeJson;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void abrirDialogGerenciarReviews(FilmeItem filme) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Gerenciar Reviews: " + filme.titulo);
        dialog.setHeaderText("Avaliações do filme (Selecione para apagar)");

        ListView<String> listaReviews = new ListView<>();
        List<JsonObject> reviewsData = new ArrayList<>();
        Button btnApagar = new Button("Apagar Review Selecionada");
        btnApagar.setDisable(true);
        btnApagar.getStyleClass().add("button");

        ClienteSocket.getInstance().enviarBuscarFilmePorId(filme.id, (sucesso, dados, msg) -> {
            Platform.runLater(() -> {
                // CORREÇÃO AQUI: Cast para JsonObject antes de usar .has() e .getAsJsonArray() com String
                if (sucesso && dados != null && dados.isJsonObject() && dados.getAsJsonObject().has("reviews")) {
                    JsonArray arr = dados.getAsJsonObject().getAsJsonArray("reviews");
                    for (JsonElement el : arr) {
                        JsonObject r = el.getAsJsonObject();
                        reviewsData.add(r);
                        String editado = r.has("editado") && "true".equalsIgnoreCase(r.get("editado").getAsString()) ? " (Editado)" : "";
                        String data = r.has("data") ? r.get("data").getAsString() : "";
                        listaReviews.getItems().add(
                                String.format("[%s] %s - %s (Nota: %s)%s", data, r.get("nome_usuario").getAsString(), r.get("titulo").getAsString(), r.get("nota").getAsString(), editado)
                        );
                    }
                    if(reviewsData.isEmpty()) listaReviews.getItems().add("Sem avaliações.");
                } else {
                    listaReviews.getItems().add("Erro ao carregar: " + msg);
                }
            });
        });

        listaReviews.getSelectionModel().selectedIndexProperty().addListener((obs, old, newVal) -> {
            btnApagar.setDisable(newVal.intValue() < 0 || reviewsData.isEmpty());
        });

        btnApagar.setOnAction(e -> {
            int idx = listaReviews.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < reviewsData.size()) {
                String idReview = reviewsData.get(idx).get("id").getAsString();
                if (AlertaUtil.mostrarConfirmacao("Confirmar", "Apagar esta review permanentemente?")) {
                    ClienteSocket.getInstance().enviarExcluirReview(idReview, (sucesso, msg) -> {
                        Platform.runLater(() -> {
                            if (sucesso) {
                                AlertaUtil.mostrarInformacao("Sucesso", "Review apagada.");
                                dialog.close();
                            } else {
                                AlertaUtil.mostrarErro("Erro", msg);
                            }
                        });
                    });
                }
            }
        });

        VBox box = new VBox(10, listaReviews, btnApagar);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void atualizarListaExibida() {
        List<FilmeItem> listaFiltrada = new ArrayList<>();
        String generoSelecionado = filtroGenero.getValue();
        if (generoSelecionado == null || "Todos os Gêneros".equals(generoSelecionado)) {
            listaFiltrada.addAll(masterList);
            listaFilmesView.setPlaceholder(new Label("Nenhum filme cadastrado no momento."));
        } else {
            for (FilmeItem item : masterList) {
                if (item.generos.contains(generoSelecionado)) {
                    listaFiltrada.add(item);
                }
            }
            listaFilmesView.setPlaceholder(new Label("Nenhum filme encontrado para o gênero '" + generoSelecionado + "'."));
        }
        int totalPaginas = (int) Math.ceil((double) listaFiltrada.size() / ITENS_POR_PAGINA);
        if (totalPaginas == 0) totalPaginas = 1;
        if (paginaAtual >= totalPaginas) paginaAtual = totalPaginas - 1;
        if (paginaAtual < 0) paginaAtual = 0;

        lblPagina.setText("Página " + (paginaAtual + 1) + " de " + totalPaginas);
        btnAnterior.setDisable(paginaAtual == 0);
        btnProximo.setDisable(paginaAtual >= totalPaginas - 1);

        int inicio = paginaAtual * ITENS_POR_PAGINA;
        int fim = Math.min(inicio + ITENS_POR_PAGINA, listaFiltrada.size());

        listaFilmesView.getItems().clear();
        if (inicio < fim) {
            listaFilmesView.getItems().addAll(listaFiltrada.subList(inicio, fim));
        }
    }

    public Scene criarCena() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        Label titleLabel = new Label("Gerenciar Filmes");
        titleLabel.getStyleClass().add("title-label");

        filtroGenero.getItems().clear();
        filtroGenero.getItems().add("Todos os Gêneros");
        filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS);
        filtroGenero.setValue("Todos os Gêneros");
        filtroGenero.setPrefWidth(380);

        painelPaginacao = new HBox(10, btnAnterior, lblPagina, btnProximo);
        painelPaginacao.setAlignment(Pos.CENTER);
        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);

        listaFilmesView.setCellFactory(param -> new ListCell<FilmeItem>() {
            private VBox content = new VBox(2);
            private Label lblTitulo = new Label();
            private Label lblInfo = new Label();
            {
                lblTitulo.getStyleClass().add("list-cell-filme-titulo");
                lblInfo.getStyleClass().add("list-cell-filme-info");
                content.getChildren().addAll(lblTitulo, lblInfo);
                content.setPadding(new Insets(5));
            }
            @Override
            protected void updateItem(FilmeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    lblTitulo.setText(item.titulo);
                    lblInfo.setText(String.format("Diretor: %s | Ano: %s", item.diretor, item.ano));
                    setGraphic(content);
                }
            }
        });

        Button btnCriar = new Button("Criar Filme");
        Button btnEditar = new Button("Editar Filme");
        Button btnExcluir = new Button("Excluir Filme");
        Button btnReviews = new Button("Gerenciar Reviews");

        btnCriar.setPrefWidth(120);
        btnEditar.setPrefWidth(120);
        btnExcluir.setPrefWidth(120);
        btnReviews.setPrefWidth(150);

        HBox crudButtons = new HBox(10, btnCriar, btnEditar, btnExcluir, btnReviews);
        crudButtons.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setPrefWidth(380);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaAdminMenu());

        btnCriar.setOnAction(e -> {
            abrirDialogFilme(null).ifPresent(json -> {
                ClienteSocket.getInstance().adminCriarFilme(json, (s, m) -> Platform.runLater(() -> {
                    if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            });
        });

        btnEditar.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            JsonObject json = new JsonObject();
            json.addProperty("id", sel.id); json.addProperty("titulo", sel.titulo);
            json.addProperty("diretor", sel.diretor); json.addProperty("ano", sel.ano);
            json.addProperty("sinopse", sel.sinopse);
            JsonArray gens = new JsonArray(); sel.generos.forEach(gens::add); json.add("genero", gens);

            abrirDialogFilme(json).ifPresent(res -> {
                ClienteSocket.getInstance().adminEditarFilme(res, (s, m) -> Platform.runLater(() -> {
                    if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            });
        });

        btnExcluir.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            if(AlertaUtil.mostrarConfirmacao("Excluir", "Tem certeza?")) {
                ClienteSocket.getInstance().adminExcluirFilme(sel.id, (s, m) -> Platform.runLater(() -> {
                    if(s) { carregarFilmes(); AlertaUtil.mostrarInformacao("Sucesso", m); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            }
        });

        btnReviews.setOnAction(e -> {
            FilmeItem selecionado = listaFilmesView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um filme para ver as reviews.");
                return;
            }
            abrirDialogGerenciarReviews(selecionado);
        });

        filtroGenero.setOnAction(e -> { paginaAtual = 0; atualizarListaExibida(); });
        btnAnterior.setOnAction(e -> { if (paginaAtual > 0) { paginaAtual--; atualizarListaExibida(); } });
        btnProximo.setOnAction(e -> { paginaAtual++; atualizarListaExibida(); });

        layout.getChildren().addAll(titleLabel, filtroGenero, listaFilmesView, painelPaginacao, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            btnVoltar.getStyleClass().add("secondary-button");
        }
        carregarFilmes();
        return scene;
    }

    private void carregarFilmes() {
        masterList.clear();
        listaFilmesView.getItems().clear();
        paginaAtual = 0;
        ClienteSocket.getInstance().enviarListarFilmes((sucesso, dados, mensagem) -> {
            Platform.runLater(() -> {
                if (sucesso && dados.isJsonArray()) {
                    for (JsonElement el : dados.getAsJsonArray()) {
                        JsonObject obj = el.getAsJsonObject();
                        List<String> gens = new ArrayList<>();
                        if (obj.has("genero")) obj.getAsJsonArray("genero").forEach(g -> gens.add(g.getAsString()));

                        masterList.add(new FilmeItem(
                                obj.get("id").getAsString(),
                                obj.get("titulo").getAsString(),
                                obj.has("nota") && !obj.get("nota").isJsonNull() ? obj.get("nota").getAsString() : "0.0",
                                obj.has("qtd_avaliacoes") ? obj.get("qtd_avaliacoes").getAsString() : "0",
                                gens,
                                obj.has("diretor") ? obj.get("diretor").getAsString() : "",
                                obj.has("ano") ? obj.get("ano").getAsString() : "",
                                obj.has("sinopse") ? obj.get("sinopse").getAsString() : ""
                        ));
                    }
                    atualizarListaExibida();
                } else {
                    listaFilmesView.setPlaceholder(new Label("Erro ou nenhum filme: " + mensagem));
                }
            });
        });
    }
}