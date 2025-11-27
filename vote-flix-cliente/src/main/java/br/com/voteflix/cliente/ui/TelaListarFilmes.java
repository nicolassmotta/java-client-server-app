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
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TelaListarFilmes {

    private SceneManager sceneManager;
    private ListView<FilmeItem> listaFilmesView = new ListView<>();
    private List<FilmeItem> masterList = new ArrayList<>();
    private ComboBox<String> filtroGenero = new ComboBox<>();

    private Button btnAnterior = new Button("❮");
    private Button btnProximo = new Button("❯");
    private Label lblPagina = new Label("1 / 1");
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
            this.id = id; this.titulo = titulo; this.nota = nota; this.qtdAvaliacoes = qtdAvaliacoes;
            this.generos = generos; this.diretor = diretor; this.ano = ano; this.sinopse = sinopse;
        }
    }

    public TelaListarFilmes(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public Scene criarCena() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 30, 15, 30));
        topBar.setStyle("-fx-background-color: #000000; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 5);");

        Label logo = new Label("VOTEFLIX");
        logo.setStyle("-fx-font-size: 24px; -fx-padding: 0; -fx-text-fill: #E50914; -fx-font-weight: 900;");

        filtroGenero.getItems().clear();
        filtroGenero.getItems().add("Todos os Gêneros");
        filtroGenero.getItems().addAll(GENEROS_PRE_CADASTRADOS);
        filtroGenero.setValue("Todos os Gêneros");
        filtroGenero.setPrefWidth(200);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoltar = new Button("Voltar ao Menu");
        btnVoltar.getStyleClass().add("secondary-button");
        btnVoltar.setOnAction(e -> sceneManager.mostrarTelaMenu());

        topBar.getChildren().addAll(logo, filtroGenero, spacer, btnVoltar);
        root.setTop(topBar);

        VBox centerContent = new VBox(15);
        centerContent.setPadding(new Insets(20, 40, 20, 40));
        centerContent.setAlignment(Pos.TOP_CENTER);

        Label pageTitle = new Label("Catálogo de Filmes");
        pageTitle.getStyleClass().add("subtitle-label");

        listaFilmesView.setPlaceholder(new Label("Nenhum filme encontrado."));
        VBox.setVgrow(listaFilmesView, Priority.ALWAYS);

        listaFilmesView.setCellFactory(p -> new ListCell<FilmeItem>() {
            private final HBox cardLayout = new HBox(15);
            private final VBox textContainer = new VBox(5);
            private final Label titulo = new Label();
            private final Label info = new Label();
            private final Label generos = new Label();
            private final Label nota = new Label();
            private final StackPane posterPlaceholder = new StackPane();

            {
                Rectangle bg = new Rectangle(60, 90);
                bg.setArcWidth(5); bg.setArcHeight(5);
                bg.setFill(Color.web("#333"));
                Label icon = new Label("🎬");
                icon.setStyle("-fx-font-size: 24px;");
                posterPlaceholder.getChildren().addAll(bg, icon);

                titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
                info.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
                generos.setStyle("-fx-text-fill: #666; -fx-font-style: italic; -fx-font-size: 11px;");

                nota.setMinWidth(40);
                nota.setAlignment(Pos.CENTER);

                textContainer.getChildren().addAll(titulo, info, generos);
                textContainer.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(textContainer, Priority.ALWAYS);

                cardLayout.getChildren().addAll(posterPlaceholder, textContainer, nota);
                cardLayout.setAlignment(Pos.CENTER_LEFT);
            }

            @Override protected void updateItem(FilmeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    titulo.setText(item.titulo);
                    info.setText(item.ano + " • " + item.diretor);
                    generos.setText(String.join(", ", item.generos));

                    double n = 0.0;
                    try { n = Double.parseDouble(item.nota.replace(",", ".")); } catch (Exception e) {}

                    if (n > 0) {
                        nota.setText(String.format("%.1f", n));
                        if(n >= 4.0) nota.setStyle("-fx-text-fill: #46d369; -fx-border-color: #46d369; -fx-border-radius: 3px; -fx-padding: 2 5;");
                        else nota.setStyle("-fx-text-fill: #e5b109; -fx-border-color: #e5b109; -fx-border-radius: 3px; -fx-padding: 2 5;");
                    } else {
                        nota.setText("N/A");
                        nota.setStyle("-fx-text-fill: #666; -fx-border-color: #666; -fx-padding: 2 5; -fx-border-radius: 3px;");
                    }
                    setGraphic(cardLayout);
                }
            }
        });

        HBox bottomBar = new HBox(15);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(10));

        btnAnterior.getStyleClass().add("secondary-button");
        btnProximo.getStyleClass().add("secondary-button");
        lblPagina.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

        Button btnVer = new Button("Ver Detalhes");
        btnVer.getStyleClass().add("secondary-button");

        Button btnAvaliar = new Button("Avaliar Filme");
        btnAvaliar.getStyleClass().add("button");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        HBox paginacaoBox = new HBox(10, btnAnterior, lblPagina, btnProximo);
        paginacaoBox.setAlignment(Pos.CENTER_LEFT);

        HBox actionsBox = new HBox(10, btnVer, btnAvaliar);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        bottomBar.getChildren().addAll(paginacaoBox, spacerBottom, actionsBox);
        centerContent.getChildren().addAll(pageTitle, listaFilmesView, bottomBar);
        root.setCenter(centerContent);

        filtroGenero.setOnAction(e -> { paginaAtual=0; atualizarListaExibida(); });
        btnAnterior.setOnAction(e -> { if(paginaAtual>0){ paginaAtual--; atualizarListaExibida(); }});
        btnProximo.setOnAction(e -> { paginaAtual++; atualizarListaExibida(); });

        btnAvaliar.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Atenção", "Selecione um filme para avaliar."); return; }
            abrirDialogCriarReview(sel.id).ifPresent(json -> {
                ClienteSocket.getInstance().enviarCriarReview(json.get("id_filme").getAsString(), json.get("titulo").getAsString(), json.get("descricao").getAsString(), json.get("nota").getAsString(), (s, m) -> Platform.runLater(() -> {
                    if(s) { AlertaUtil.mostrarInformacao("Sucesso", m); carregarFilmes(); } else AlertaUtil.mostrarErro("Erro", m);
                }));
            });
        });

        btnVer.setOnAction(e -> {
            FilmeItem sel = listaFilmesView.getSelectionModel().getSelectedItem();
            if (sel == null) { AlertaUtil.mostrarErro("Atenção", "Selecione um filme para ver detalhes."); return; }
            ClienteSocket.getInstance().enviarBuscarFilmePorId(sel.id, (s, d, m) -> Platform.runLater(() -> {
                if(s && d!=null && d.isJsonObject()) {
                    JsonObject obj = d.getAsJsonObject();
                    if(obj.has("filme") && obj.has("reviews")) mostrarDialogDetalhes(obj.getAsJsonObject("filme"), obj.getAsJsonArray("reviews"));
                    else AlertaUtil.mostrarErro("Erro", "Dados incompletos.");
                } else AlertaUtil.mostrarErro("Erro", m);
            }));
        });

        Scene scene = new Scene(root, 900, 650);
        if(getClass().getResource("/styles.css") != null) scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
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
                            o.get("id").getAsString(),
                            o.get("titulo").getAsString(),
                            o.has("nota") && !o.get("nota").isJsonNull() ? o.get("nota").getAsString() : "0.0",
                            o.has("qtd_avaliacoes") ? o.get("qtd_avaliacoes").getAsString() : "0",
                            gl,
                            o.has("diretor") ? o.get("diretor").getAsString() : "Desconhecido",
                            o.has("ano") ? o.get("ano").getAsString() : "----",
                            o.has("sinopse") ? o.get("sinopse").getAsString() : ""
                    ));
                }
                atualizarListaExibida();
            }
        }));
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

        lblPagina.setText((paginaAtual + 1) + " / " + totalPaginas);
        btnAnterior.setDisable(paginaAtual == 0);
        btnProximo.setDisable(paginaAtual >= totalPaginas - 1);

        int inicio = paginaAtual * ITENS_POR_PAGINA;
        int fim = Math.min(inicio + ITENS_POR_PAGINA, listaFiltrada.size());
        listaFilmesView.getItems().clear();
        if (inicio < fim) listaFilmesView.getItems().addAll(listaFiltrada.subList(inicio, fim));
    }

    private Optional<JsonObject> abrirDialogCriarReview(String filmeId) {
        Dialog<JsonObject> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        dialog.setTitle("Avaliar Filme");
        dialog.setHeaderText("Escreva sua avaliação");
        ButtonType btnEnviar = new ButtonType("Enviar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnviar, btnCancelar);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.add(new Label("Título:"), 0, 0);
        TextField txtTitulo = new TextField();
        grid.add(txtTitulo, 1, 0);
        grid.add(new Label("Nota:"), 0, 1);
        ComboBox<Integer> cbNota = new ComboBox<>(); cbNota.getItems().addAll(1, 2, 3, 4, 5); cbNota.setValue(5);
        grid.add(cbNota, 1, 1);
        grid.add(new Label("Opinião:"), 0, 2);
        TextArea txtDesc = new TextArea(); txtDesc.setWrapText(true);
        grid.add(txtDesc, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefSize(500, 400);

        dialog.setResultConverter(btn -> {
            if (btn == btnEnviar) {
                JsonObject j = new JsonObject();
                j.addProperty("id_filme", filmeId); j.addProperty("titulo", txtTitulo.getText());
                j.addProperty("nota", cbNota.getValue().toString()); j.addProperty("descricao", txtDesc.getText());
                return j;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void mostrarDialogDetalhes(JsonObject filme, JsonArray reviews) {
        Dialog<Void> dialog = new Dialog<>();
        if (getClass().getResource("/styles.css") != null) dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.setTitle(filme.get("titulo").getAsString());
        dialog.setHeaderText("Detalhes & Avaliações");

        VBox content = new VBox(15);
        content.getChildren().add(new Label("Sinopse:"));
        TextArea sinopse = new TextArea(filme.get("sinopse").getAsString());
        sinopse.setEditable(false); sinopse.setWrapText(true); sinopse.setPrefRowCount(3);
        content.getChildren().add(sinopse);

        content.getChildren().add(new Separator());
        Label lblRev = new Label("Avaliações da Comunidade (" + reviews.size() + "):");
        lblRev.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(lblRev);

        ListView<VBox> listaR = new ListView<>();
        if(reviews.size() == 0) {
            listaR.setPlaceholder(new Label("Seja o primeiro a avaliar este filme!"));
        }

        for(JsonElement e : reviews) {
            JsonObject r = e.getAsJsonObject();
            VBox card = new VBox(5);

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label user = new Label(r.get("nome_usuario").getAsString());
            user.setStyle("-fx-font-weight: bold; -fx-text-fill: #E50914;");

            Label nota = new Label("★ " + r.get("nota").getAsString());
            nota.setStyle("-fx-text-fill: #e5b109; -fx-font-weight: bold;");

            String dt = r.has("data") ? r.get("data").getAsString() : "";
            Label data = new Label(dt);
            data.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(user, nota, spacer, data);

            Label tituloRev = new Label(r.get("titulo").getAsString());
            tituloRev.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

            Label desc = new Label(r.get("descricao").getAsString());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: #ccc;");

            card.getChildren().addAll(header, tituloRev, desc);
            listaR.getItems().add(card);
        }
        VBox.setVgrow(listaR, Priority.ALWAYS);
        content.getChildren().add(listaR);

        dialog.getDialogPane().setContent(content);
        ButtonType btnFechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(btnFechar);

        dialog.getDialogPane().setPrefSize(600, 700);
        dialog.show();
    }
}