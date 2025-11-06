// vote-flix-cliente/src/main/java/br/com/voteflix/cliente/ui/TelaAdminFilmes.java
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
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.control.ListCell;

public class TelaAdminFilmes {

    private SceneManager sceneManager;
    private ListView<FilmeItem> listaFilmesView = new ListView<>();
    private List<FilmeItem> masterList = new ArrayList<>(); // <-- NOVA

    // --- NOVOS COMPONENTES DE UI E ESTADO ---
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
        txtTitulo.setPromptText("Título do filme (3-30 chars)"); // Dica mantida
        TextField txtDiretor = new TextField();
        txtDiretor.setPromptText("Nome do diretor (3-30 chars)"); // Dica mantida
        TextField txtAno = new TextField();
        txtAno.setPromptText("Ano de lançamento (3-4 dígitos)"); // Dica mantida
        TextArea txtSinopse = new TextArea();
        txtSinopse.setPromptText("Breve sinopse do filme (max 250 chars)"); // Dica mantida
        txtSinopse.setWrapText(true);

        grid.add(new Label("Título:"), 0, 0);
        grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Diretor:"), 0, 1);
        grid.add(txtDiretor, 1, 1);
        grid.add(new Label("Ano:"), 0, 2);
        grid.add(txtAno, 1, 2);
        grid.add(new Label("Sinopse:"), 0, 3);
        grid.add(txtSinopse, 1, 3);
        GridPane.setVgrow(txtSinopse, Priority.ALWAYS); // Permite crescer

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
                String titulo = txtTitulo.getText(); // Não usa trim()
                String diretor = txtDiretor.getText();
                String anoStr = txtAno.getText();
                String sinopse = txtSinopse.getText();
                List<String> generosSelecionadosList = checkBoxes.stream()
                        .filter(CheckBox::isSelected)
                        .map(CheckBox::getText)
                        .collect(Collectors.toList());

                // Validações de UI (não de negócio)
                if (generosSelecionadosList.isEmpty()) {
                    AlertaUtil.mostrarErro("Erro de Validação", "Selecione pelo menos um gênero.");
                    return null;
                }

                // Validações de negócio REMOVIDAS

                JsonObject filmeJson = new JsonObject();
                if (modoEdicao) {
                    filmeJson.addProperty("id", filmeExistente.get("id").getAsString());
                }
                filmeJson.addProperty("titulo", titulo);
                filmeJson.addProperty("diretor", diretor);
                filmeJson.addProperty("ano", anoStr); // Envia como string
                filmeJson.addProperty("sinopse", sinopse);

                JsonArray generosJsonArray = new JsonArray();
                for (String genero : generosSelecionadosList) {
                    generosJsonArray.add(genero);
                }
                filmeJson.add("genero", generosJsonArray);

                return filmeJson;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    // ... (Restante do arquivo TelaAdminFilmes.java permanece igual) ...
    // ... (atualizarListaExibida, criarCena, carregarFilmes, etc.)
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

        Label titleLabel = new Label("Gerenciar Filmes");

        filtroGenero.getItems().add("Todos os Gêneros");
        filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS);
        filtroGenero.setValue("Todos os Gêneros");
        filtroGenero.setPrefWidth(380); // Ajustado para 3 botões

        painelPaginacao = new HBox(10, btnAnterior, lblPagina, btnProximo);
        painelPaginacao.setAlignment(Pos.CENTER);

        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);
        listaFilmesView.setPlaceholder(new Label("Nenhum filme cadastrado no momento."));

        listaFilmesView.setCellFactory(param -> new ListCell<FilmeItem>() {
            private VBox content = new VBox(2);
            private Label lblTitulo = new Label();
            private Label lblInfo = new Label();

            {
                // *** MUDANÇA AQUI: Usando classes CSS ***
                lblTitulo.getStyleClass().add("list-cell-filme-titulo");
                lblInfo.getStyleClass().add("list-cell-filme-info");
                // *** FIM DA MUDANÇA ***

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

        Button btnCriar = new Button("Criar Filme");
        btnCriar.setPrefWidth(120);
        Button btnEditar = new Button("Editar Filme");
        btnEditar.setPrefWidth(120);
        Button btnExcluir = new Button("Excluir Filme");
        btnExcluir.setPrefWidth(120);

        HBox crudButtons = new HBox(10, btnCriar, btnEditar, btnExcluir);
        crudButtons.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setPrefWidth(380);

        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaAdminMenu());

        btnCriar.setOnAction(e -> {
            abrirDialogFilme(null).ifPresent(filmeJson -> {
                ClienteSocket.getInstance().adminCriarFilme(filmeJson, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                            carregarFilmes(); // Atualiza a lista
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Criar", mensagem);
                        }
                    });
                });
            });
        });

        btnEditar.setOnAction(e -> {
            FilmeItem selecionado = listaFilmesView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um filme para editar.");
                return;
            }
            ClienteSocket.getInstance().enviarBuscarFilmePorId(selecionado.id, (sucessoBusca, dadosCompletos, mensagemBusca) -> {
                Platform.runLater(() -> {
                    if (!sucessoBusca || dadosCompletos == null || !dadosCompletos.isJsonObject() || !dadosCompletos.getAsJsonObject().has("filme")) {
                        AlertaUtil.mostrarErro("Erro ao Carregar Dados", mensagemBusca);
                        return;
                    }
                    JsonObject filmeParaEditar = dadosCompletos.getAsJsonObject().getAsJsonObject("filme");
                    abrirDialogFilme(filmeParaEditar).ifPresent(filmeEditado -> {
                        ClienteSocket.getInstance().adminEditarFilme(filmeEditado, (sucessoEdicao, mensagemEdicao) -> {
                            Platform.runLater(() -> {
                                if (sucessoEdicao) {
                                    AlertaUtil.mostrarInformacao("Sucesso", mensagemEdicao);
                                    carregarFilmes(); // Atualiza a lista
                                } else {
                                    AlertaUtil.mostrarErro("Erro ao Editar", mensagemEdicao);
                                }
                            });
                        });
                    });
                });
            });
        });

        btnExcluir.setOnAction(e -> {
            FilmeItem selecionado = listaFilmesView.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                AlertaUtil.mostrarErro("Erro", "Selecione um filme para excluir.");
                return;
            }
            boolean confirmado = AlertaUtil.mostrarConfirmacao("Excluir Filme", "Tem certeza que deseja excluir o filme '" + selecionado.titulo + "'? Esta ação também excluirá todas as reviews associadas.");
            if (confirmado) {
                ClienteSocket.getInstance().adminExcluirFilme(selecionado.id, (sucesso, mensagem) -> {
                    Platform.runLater(() -> {
                        if (sucesso) {
                            AlertaUtil.mostrarInformacao("Sucesso", mensagem);
                            carregarFilmes(); // Atualiza a lista
                        } else {
                            AlertaUtil.mostrarErro("Erro ao Excluir", mensagem);
                        }
                    });
                });
            }
        });

        filtroGenero.setOnAction(e -> {
            paginaAtual = 0; // Reseta para a primeira página ao trocar o filtro
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

        layout.getChildren().addAll(titleLabel, filtroGenero, listaFilmesView, painelPaginacao, crudButtons, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);


        URL cssResource = getClass().getResource("/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            titleLabel.getStyleClass().add("title-label");
            btnVoltar.getStyleClass().add("secondary-button");
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
                    if (dados != null && dados.isJsonArray() && dados.getAsJsonArray().size() > 0) {
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
}