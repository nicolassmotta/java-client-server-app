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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.control.Separator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ListCell;

public class TelaListarFilmes {

    private SceneManager sceneManager;
    private ListView<FilmeItem> listaFilmesView = new ListView<>();
    private List<FilmeItem> masterList = new ArrayList<>();

    private ComboBox<String> filtroGenero = new ComboBox<>();
    private Button btnAnterior = new Button("<< Anterior");
    private Button btnProximo = new Button("Próximo >>");
    private Label lblPagina = new Label("Página 1 de 1");
    private HBox painelPaginacao;

    private static final int ITENS_POR_PAGINA = 10;
    private int paginaAtual = 0;


    private final List<String> GENEROS_PRE_CADASTRADOS = Arrays.asList(
            "Ação", "Aventura", "Comédia", "Drama", "Fantasia", "Ficção Científica",
            "Terror", "Romance", "Documentário", "Musical", "Animação"
    );

    private static class FilmeItem {
        String id;
        String titulo;
        String nota;
        String qtdAvaliacoes;
        List<String> generos;

        public FilmeItem(String id, String titulo, String nota, String qtdAvaliacoes, List<String> generos) {
            this.id = id;
            this.titulo = titulo;
            this.nota = nota;
            this.qtdAvaliacoes = qtdAvaliacoes;
            this.generos = generos;
        }
    }

    public TelaListarFilmes(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private Optional<JsonObject> abrirDialogCriarReview(String filmeId) {
        Dialog<JsonObject> dialog = new Dialog<>();
        dialog.setTitle("Avaliar Filme");
        dialog.setHeaderText("Escreva sua avaliação para este filme");

        ButtonType criarButtonType = new ButtonType("Enviar Avaliação", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(criarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Título da sua avaliação (até 50 chars)");
        TextArea txtDescricao = new TextArea();
        txtDescricao.setPromptText("Sua opinião sobre o filme... (até 250 chars)");
        txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>();
        cbNota.getItems().addAll(1, 2, 3, 4, 5);
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
            if (dialogButton == criarButtonType) {
                String titulo = txtTitulo.getText().trim();
                String descricao = txtDescricao.getText().trim();
                Integer nota = cbNota.getValue();

                if (titulo.isEmpty() || nota == null || descricao.isEmpty()) {
                    AlertaUtil.mostrarErro("Erro de Validação", "Todos os campos são obrigatórios.");
                    return null;
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
                reviewJson.addProperty("id_filme", filmeId);
                reviewJson.addProperty("titulo", titulo);
                reviewJson.addProperty("nota", nota.toString());
                reviewJson.addProperty("descricao", descricao);
                return reviewJson;
            }
            return null;
        });

        return dialog.showAndWait();
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
        if (totalPaginas == 0) {
            totalPaginas = 1;
        }

        if (paginaAtual >= totalPaginas) {
            paginaAtual = totalPaginas - 1;
        }
        if (paginaAtual < 0) {
            paginaAtual = 0;
        }

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

        Label titleLabel = new Label("Filmes Disponíveis");

        filtroGenero.getItems().add("Todos os Gêneros");
        filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS);
        filtroGenero.setValue("Todos os Gêneros");
        filtroGenero.setPrefWidth(390);

        painelPaginacao = new HBox(10, btnAnterior, lblPagina, btnProximo);
        painelPaginacao.setAlignment(Pos.CENTER);

        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);
        listaFilmesView.setPlaceholder(new Label("Nenhum filme cadastrado no momento."));

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

                    double notaDouble = 0.0;
                    try {
                        notaDouble = Double.parseDouble(item.nota.replace(",", "."));
                    } catch (Exception e) { /* ignora */ }

                    if (notaDouble > 0) {
                        lblInfo.setText(String.format("Nota: %s | %s avaliações", item.nota, item.qtdAvaliacoes));
                    } else {
                        lblInfo.setText("Nenhuma avaliação");
                    }
                    setGraphic(content);
                }
            }
        });

        Button btnVerDetalhes = new Button("Ver Detalhes / Reviews");
        btnVerDetalhes.setPrefWidth(190);
        Button btnCriarReview = new Button("Avaliar Filme");
        btnCriarReview.setPrefWidth(190);

        HBox actionButtons = new HBox(10, btnVerDetalhes, btnCriarReview);
        actionButtons.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setPrefWidth(390);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        btnCriarReview.setOnAction(e -> {
            FilmeItem selecionado = listaFilmesView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um filme para avaliar.");
                return;
            }

            abrirDialogCriarReview(selecionado.id).ifPresent(reviewJson -> {
                String idFilme = reviewJson.get("id_filme").getAsString();
                String titulo = reviewJson.get("titulo").getAsString();
                String descricao = reviewJson.get("descricao").getAsString();
                String nota = reviewJson.get("nota").getAsString();

                ClienteSocket.getInstance().enviarCriarReview(idFilme, titulo, descricao, nota, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                            carregarFilmes();
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Criar Review", mensagem);
                        }
                    });
                });
            });
        });

        btnVerDetalhes.setOnAction(e -> {
            FilmeItem selecionado = listaFilmesView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um filme para ver os detalhes.");
                return;
            }

            ClienteSocket.getInstance().enviarBuscarFilmePorId(selecionado.id, (sucesso, dadosCompletos, mensagem) -> {
                Platform.runLater(() -> {
                    if (sucesso && dadosCompletos != null && dadosCompletos.isJsonObject()) {
                        JsonObject respostaCompleta = dadosCompletos.getAsJsonObject();
                        if (respostaCompleta.has("filme") && respostaCompleta.has("reviews") &&
                                respostaCompleta.get("filme").isJsonObject() && respostaCompleta.get("reviews").isJsonArray())
                        {
                            JsonObject filme = respostaCompleta.getAsJsonObject("filme");
                            JsonArray reviews = respostaCompleta.getAsJsonArray("reviews");
                            mostrarDialogDetalhes(filme, reviews);
                        } else {
                            AlertaUtil.mostrarErro("Erro", "Resposta do servidor incompleta ao buscar detalhes.");
                        }
                    } else {
                        AlertaUtil.mostrarErro("Erro ao Buscar Detalhes", mensagem);
                    }
                });
            });
        });

        filtroGenero.setOnAction(e -> {
            paginaAtual = 0;
            atualizarListaExibida();
        });

        btnAnterior.setOnAction(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                atualizarListaExibida();
            }
        });

        btnProximo.setOnAction(e -> {
            paginaAtual++;
            atualizarListaExibida();
        });

        layout.getChildren().addAll(titleLabel, filtroGenero, listaFilmesView, painelPaginacao, actionButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);

        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            btnVoltar.getStyleClass().add("secondary-button");
            btnVerDetalhes.getStyleClass().add("secondary-button");
        } else {
            System.err.println("Recurso 'styles.css' não encontrado.");
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
                if (sucesso) {
                    if (dados != null && dados.isJsonArray() && !dados.getAsJsonArray().isEmpty()) {
                        for (JsonElement filmeElement : dados.getAsJsonArray()) {
                            JsonObject obj = filmeElement.getAsJsonObject();
                            String nota = "0.0";
                            if (obj.has("nota") && !obj.get("nota").isJsonNull()) {
                                nota = obj.get("nota").getAsString();
                            }
                            String qtd = "0";
                            if (obj.has("qtd_avaliacoes") && !obj.get("qtd_avaliacoes").isJsonNull()) {
                                qtd = obj.get("qtd_avaliacoes").getAsString();
                            }

                            List<String> generosDoFilme = new ArrayList<>();
                            if (obj.has("genero") && obj.get("genero").isJsonArray()) {
                                JsonArray generosJson = obj.getAsJsonArray("genero");
                                for (JsonElement genEl : generosJson) {
                                    generosDoFilme.add(genEl.getAsString());
                                }
                            }
                            masterList.add(
                                    new FilmeItem(
                                            obj.get("id").getAsString(),
                                            obj.get("titulo").getAsString(),
                                            nota,
                                            qtd,
                                            generosDoFilme
                                    )
                            );
                        }
                    }
                } else {
                    AlertaUtil.mostrarErro("Erro ao Carregar Filmes", mensagem);
                }
                atualizarListaExibida();
            });
        });
    }

    private void mostrarDialogDetalhes(JsonObject filme, JsonArray reviews) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalhes do Filme");
        dialog.setHeaderText(filme.get("titulo").getAsString().toUpperCase());

        VBox layoutDialog = new VBox(10);
        layoutDialog.setPadding(new Insets(20));

        GridPane gridInfo = new GridPane();
        gridInfo.setHgap(10);
        gridInfo.setVgap(5);

        gridInfo.add(new Label("Diretor:"), 0, 0);
        gridInfo.add(new Label(filme.get("diretor").getAsString()), 1, 0);
        gridInfo.add(new Label("Ano:"), 0, 1);
        gridInfo.add(new Label(filme.get("ano").getAsString()), 1, 1);

        if (filme.has("genero") && filme.get("genero").isJsonArray()) {
            JsonArray generosArray = filme.getAsJsonArray("genero");
            if (!generosArray.isEmpty()) {
                StringBuilder generosStr = new StringBuilder();
                for (int i = 0; i < generosArray.size(); i++) {
                    generosStr.append(generosArray.get(i).getAsString());
                    if (i < generosArray.size() - 1) generosStr.append(", ");
                }
                gridInfo.add(new Label("Gêneros:"), 0, 2);
                Label lblGeneros = new Label(generosStr.toString());
                lblGeneros.setWrapText(true);
                gridInfo.add(lblGeneros, 1, 2);
            }
        }

        layoutDialog.getChildren().add(gridInfo);

        layoutDialog.getChildren().add(new Separator());
        layoutDialog.getChildren().add(new Label("Sinopse:"));
        TextArea txtSinopse = new TextArea(filme.get("sinopse").getAsString());
        txtSinopse.setEditable(false);
        txtSinopse.setWrapText(true);
        txtSinopse.setPrefRowCount(4);
        layoutDialog.getChildren().add(txtSinopse);

        layoutDialog.getChildren().add(new Separator());
        layoutDialog.getChildren().add(new Label("Avaliações (" + reviews.size() + "):"));

        VBox boxReviews = new VBox(10);
        boxReviews.getStyleClass().add("reviews-box");

        if (reviews.isEmpty()) {
            boxReviews.getChildren().add(new Label("Este filme ainda não possui avaliações."));
        } else {
            for (int i = 0; i < reviews.size(); i++) {
                JsonElement reviewEl = reviews.get(i);
                JsonObject r = reviewEl.getAsJsonObject();
                VBox reviewEntry = new VBox(2);
                Label lblReviewTitulo = new Label(r.get("titulo").getAsString() + " (Nota: " + r.get("nota").getAsString() + ")");
                lblReviewTitulo.getStyleClass().add("list-cell-review-titulo");

                Label lblReviewUser = new Label("Por: " + r.get("nome_usuario").getAsString());
                lblReviewUser.getStyleClass().add("list-cell-filme-info");

                Label lblReviewDesc = new Label("\"" + r.get("descricao").getAsString() + "\"");
                lblReviewDesc.setWrapText(true);

                reviewEntry.getChildren().addAll(lblReviewTitulo, lblReviewUser, lblReviewDesc);
                boxReviews.getChildren().add(reviewEntry);

                if (i < reviews.size() - 1) {
                    boxReviews.getChildren().add(new Separator());
                }
            }
        }

        ScrollPane scrollPaneReviews = new ScrollPane(boxReviews);
        scrollPaneReviews.setFitToWidth(true);
        scrollPaneReviews.setPrefHeight(200);
        layoutDialog.getChildren().add(scrollPaneReviews);
        dialog.getDialogPane().setContent(layoutDialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(500, 600);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        dialog.showAndWait();
    }
}