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
        String id, titulo, nota, qtdAvaliacoes;
        List<String> generos;
        public FilmeItem(String id, String titulo, String nota, String qtdAvaliacoes, List<String> generos) {
            this.id = id; this.titulo = titulo; this.nota = nota; this.qtdAvaliacoes = qtdAvaliacoes; this.generos = generos;
        }
    }

    public TelaListarFilmes(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    private Optional<JsonObject> abrirDialogCriarReview(String filmeId) {
        Dialog<JsonObject> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        dialog.setTitle("Avaliar Filme");
        dialog.setHeaderText("Escreva sua avaliação");
        ButtonType btnEnviar = new ButtonType("Enviar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnviar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(70);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField txtTitulo = new TextField(); txtTitulo.setPromptText("Título da avaliação");
        TextArea txtDescricao = new TextArea(); txtDescricao.setPromptText("Sua opinião..."); txtDescricao.setWrapText(true);
        ComboBox<Integer> cbNota = new ComboBox<>(); cbNota.getItems().addAll(1, 2, 3, 4, 5); cbNota.setPromptText("Nota");

        grid.add(new Label("Título:"), 0, 0); grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Nota:"), 0, 1); grid.add(cbNota, 1, 1);
        grid.add(new Label("Descrição:"), 0, 2); grid.add(txtDescricao, 1, 2);
        GridPane.setVgrow(txtDescricao, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(400, 300);
        Platform.runLater(txtTitulo::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == btnEnviar) {
                String t = txtTitulo.getText().trim(), d = txtDescricao.getText().trim();
                Integer n = cbNota.getValue();
                if(t.isEmpty() || n == null || d.isEmpty()) { AlertaUtil.mostrarErro("Erro", "Preencha tudo."); return null; }
                if(t.length() > 50) { AlertaUtil.mostrarErro("Erro", "Título > 50 chars."); return null; }
                if(d.length() > 250) { AlertaUtil.mostrarErro("Erro", "Descrição > 250 chars."); return null; }
                JsonObject j = new JsonObject();
                j.addProperty("id_filme", filmeId); j.addProperty("titulo", t); j.addProperty("nota", n.toString()); j.addProperty("descricao", d);
                return j;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void mostrarDialogDetalhes(JsonObject filme, JsonArray reviews) {
        Dialog<Void> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setTitle("Detalhes do Filme");
        dialog.setHeaderText(filme.get("titulo").getAsString().toUpperCase());

        VBox layoutDialog = new VBox(10); layoutDialog.setPadding(new Insets(20));
        GridPane gridInfo = new GridPane(); gridInfo.setHgap(10); gridInfo.setVgap(5);

        gridInfo.add(new Label("Diretor:"), 0, 0); gridInfo.add(new Label(filme.get("diretor").getAsString()), 1, 0);
        gridInfo.add(new Label("Ano:"), 0, 1); gridInfo.add(new Label(filme.get("ano").getAsString()), 1, 1);

        if (filme.has("genero") && filme.get("genero").isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            JsonArray ga = filme.getAsJsonArray("genero");
            for(int i=0; i<ga.size(); i++) { sb.append(ga.get(i).getAsString()); if(i<ga.size()-1) sb.append(", "); }
            gridInfo.add(new Label("Gêneros:"), 0, 2);
            Label lg = new Label(sb.toString()); lg.setWrapText(true);
            gridInfo.add(lg, 1, 2);
        }
        layoutDialog.getChildren().add(gridInfo);
        layoutDialog.getChildren().add(new Separator());
        layoutDialog.getChildren().add(new Label("Sinopse:"));
        TextArea txtS = new TextArea(filme.get("sinopse").getAsString());
        txtS.setEditable(false); txtS.setWrapText(true); txtS.setPrefRowCount(4);
        layoutDialog.getChildren().add(txtS);
        layoutDialog.getChildren().add(new Separator());
        layoutDialog.getChildren().add(new Label("Avaliações (" + reviews.size() + "):"));

        VBox boxReviews = new VBox(10); boxReviews.getStyleClass().add("reviews-box");
        if(reviews.isEmpty()) boxReviews.getChildren().add(new Label("Sem avaliações."));
        else {
            for (JsonElement el : reviews) {
                JsonObject r = el.getAsJsonObject();
                VBox re = new VBox(2);
                Label lt = new Label(r.get("titulo").getAsString() + " (Nota: " + r.get("nota").getAsString() + ")");
                lt.getStyleClass().add("list-cell-review-titulo");
                Label lu = new Label("Por: " + r.get("nome_usuario").getAsString());
                lu.getStyleClass().add("list-cell-filme-info");
                Label ld = new Label("\"" + r.get("descricao").getAsString() + "\"");
                ld.setWrapText(true);
                re.getChildren().addAll(lt, lu, ld);
                boxReviews.getChildren().add(re);
                boxReviews.getChildren().add(new Separator());
            }
        }
        ScrollPane sp = new ScrollPane(boxReviews); sp.setFitToWidth(true); sp.setPrefHeight(200);
        layoutDialog.getChildren().add(sp);
        dialog.getDialogPane().setContent(layoutDialog);

        ButtonType btnFechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnFechar);

        dialog.getDialogPane().setPrefSize(500, 600);
        dialog.showAndWait();
    }

    private void atualizarListaExibida() {
        List<FilmeItem> listaFiltrada = new ArrayList<>();
        String generoSelecionado = filtroGenero.getValue();
        if (generoSelecionado == null || "Todos os Gêneros".equals(generoSelecionado)) listaFiltrada.addAll(masterList);
        else for (FilmeItem item : masterList) if (item.generos.contains(generoSelecionado)) listaFiltrada.add(item);

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
        if (inicio < fim) listaFilmesView.getItems().addAll(listaFiltrada.subList(inicio, fim));
    }

    public Scene criarCena() {
        VBox layout = new VBox(15); layout.setPadding(new Insets(30)); layout.setAlignment(Pos.CENTER);
        Label lbl = new Label("Filmes Disponíveis"); lbl.getStyleClass().add("title-label");
        filtroGenero.getItems().add("Todos os Gêneros"); filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS); filtroGenero.setValue("Todos os Gêneros"); filtroGenero.setPrefWidth(390);
        painelPaginacao = new HBox(10, btnAnterior, lblPagina, btnProximo); painelPaginacao.setAlignment(Pos.CENTER);
        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);

        listaFilmesView.setCellFactory(p -> new ListCell<FilmeItem>() {
            private VBox c = new VBox(2); private Label t = new Label(), i = new Label();
            { t.getStyleClass().add("list-cell-filme-titulo"); i.getStyleClass().add("list-cell-filme-info"); c.getChildren().addAll(t, i); c.setPadding(new Insets(5)); }
            @Override protected void updateItem(FilmeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    t.setText(item.titulo);
                    double n = 0.0; try { n = Double.parseDouble(item.nota.replace(",", ".")); } catch (Exception e) {}
                    if (n > 0) i.setText(String.format("Nota: %s | %s avaliações", item.nota, item.qtdAvaliacoes));
                    else i.setText("Nenhuma avaliação");
                    setGraphic(c);
                }
            }
        });

        Button btnVer = new Button("Ver Detalhes / Reviews"), btnAvaliar = new Button("Avaliar Filme");
        btnVer.setPrefWidth(190); btnAvaliar.setPrefWidth(190);
        HBox act = new HBox(10, btnVer, btnAvaliar); act.setAlignment(Pos.CENTER);
        Button btnVoltar = new Button("Voltar"); btnVoltar.setPrefWidth(390);
        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        btnAvaliar.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            abrirDialogCriarReview(sel.id).ifPresent(json -> {
                ClienteSocket.getInstance().enviarCriarReview(json.get("id_filme").getAsString(), json.get("titulo").getAsString(), json.get("descricao").getAsString(), json.get("nota").getAsString(), (s, m) -> Platform.runLater(() -> {
                    if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarFilmes(); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            });
        });

        btnVer.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Erro", "Selecione um filme."); return; }
            ClienteSocket.getInstance().enviarBuscarFilmePorId(sel.id, (s, d, m) -> Platform.runLater(() -> {
                if(s && d!=null && d.isJsonObject()) {
                    JsonObject obj = d.getAsJsonObject();
                    if(obj.has("filme") && obj.has("reviews")) mostrarDialogDetalhes(obj.getAsJsonObject("filme"), obj.getAsJsonArray("reviews"));
                    else AlertaUtil.mostrarErro("Erro", "Dados incompletos.");
                } else AlertaUtil.mostrarErro("Erro", m);
            }));
        });

        layout.getChildren().addAll(lbl, filtroGenero, listaFilmesView, painelPaginacao, act, btnVoltar);
        Scene scene = new Scene(layout, 800, 600);
        if(getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            btnVoltar.getStyleClass().add("secondary-button");
            btnVer.getStyleClass().add("secondary-button");
        }
        carregarFilmes();
        return scene;
    }

    private void carregarFilmes() {
        masterList.clear(); listaFilmesView.getItems().clear(); paginaAtual = 0;
        ClienteSocket.getInstance().enviarListarFilmes((s, d, m) -> Platform.runLater(() -> {
            if (s && d.isJsonArray()) {
                for (JsonElement el : d.getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    List<String> gl = new ArrayList<>();
                    if(o.has("genero")) o.getAsJsonArray("genero").forEach(x -> gl.add(x.getAsString()));
                    masterList.add(new FilmeItem(
                            o.get("id").getAsString(), o.get("titulo").getAsString(),
                            o.has("nota") && !o.get("nota").isJsonNull() ? o.get("nota").getAsString() : "0.0",
                            o.has("qtd_avaliacoes") ? o.get("qtd_avaliacoes").getAsString() : "0", gl
                    ));
                }
                atualizarListaExibida();
            } else AlertaUtil.mostrarErro("Erro", m);
        }));
    }
}