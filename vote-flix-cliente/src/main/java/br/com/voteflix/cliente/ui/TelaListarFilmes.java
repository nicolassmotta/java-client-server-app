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

        Button btnVer = new Button("Ver Detalhes");
        btnVer.getStyleClass().add("secondary-button");

        Button btnAvaliar = new Button("Avaliar Filme");
        btnAvaliar.getStyleClass().add("button");

        bottomBar.getChildren().addAll(btnVer, btnAvaliar);
        centerContent.getChildren().addAll(pageTitle, listaFilmesView, bottomBar);
        root.setCenter(centerContent);

        filtroGenero.setOnAction(e -> atualizarListaExibida());

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
        masterList.clear(); listaFilmesView.getItems().clear();
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
        listaFilmesView.getItems().setAll(listaFiltrada);
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

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(100);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        Label lblT = new Label("Título:");
        lblT.getStyleClass().add("label");
        lblT.setStyle("-fx-font-weight: bold;");

        Label lblN = new Label("Nota:");
        lblN.getStyleClass().add("label");
        lblN.setStyle("-fx-font-weight: bold;");

        Label lblO = new Label("Opinião:");
        lblO.getStyleClass().add("label");
        lblO.setStyle("-fx-font-weight: bold;");

        grid.add(lblT, 0, 0);
        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Resumo em poucas palavras");
        grid.add(txtTitulo, 1, 0);

        grid.add(lblN, 0, 1);
        ComboBox<Integer> cbNota = new ComboBox<>();
        cbNota.getItems().addAll(1, 2, 3, 4, 5);
        cbNota.setValue(5);
        grid.add(cbNota, 1, 1);

        grid.add(lblO, 0, 2);
        TextArea txtDesc = new TextArea();
        txtDesc.setWrapText(true);
        txtDesc.setPromptText("O que você achou do filme?");
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
        Label lblSinopse = new Label("Sinopse:");
        lblSinopse.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        content.getChildren().add(lblSinopse);

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

            boolean isEdited = false;
            if(r.has("editado")) {
                String val = r.get("editado").getAsString();
                isEdited = "true".equalsIgnoreCase(val) || "1".equals(val);
            }

            Label data = new Label(dt);
            data.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-font-style: italic;");

            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(user, nota, spacer, data);

            if(isEdited) {
                Label editTag = new Label("(Editado)");
                editTag.setStyle("-fx-text-fill: #e5b109; -fx-font-size: 10px; -fx-font-weight: bold;");
                header.getChildren().add(editTag);
            }

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