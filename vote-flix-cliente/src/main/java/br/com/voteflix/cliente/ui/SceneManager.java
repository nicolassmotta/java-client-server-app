package br.com.voteflix.cliente.ui;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class SceneManager {

    private final Stage primaryStage;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setMinWidth(900);
        this.primaryStage.setMinHeight(650);
    }

    private void switchScene(Scene newScene, String title) {
        primaryStage.setTitle(title);

        if (primaryStage.getWidth() <= 1 || primaryStage.getHeight() <= 1) {
            primaryStage.setWidth(900);
            primaryStage.setHeight(650);
            primaryStage.centerOnScreen();
        }

        if (newScene.getRoot() != null) {
            newScene.getRoot().setOpacity(0);
        }

        primaryStage.setScene(newScene);

        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }

        if (newScene.getRoot() != null) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newScene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }

    public void mostrarTelaConexao() {
        TelaConexao tela = new TelaConexao(this);
        switchScene(tela.criarCena(), "VoteFlix - Conexão");
    }

    public void mostrarTelaInicial() {
        TelaInicial tela = new TelaInicial(this);
        switchScene(tela.criarCena(), "VoteFlix - Bem-vindo");
    }

    public void mostrarTelaLogin() {
        TelaLogin tela = new TelaLogin(this);
        switchScene(tela.criarCena(), "VoteFlix - Login");
    }

    public void mostrarTelaCadastro() {
        TelaCadastro tela = new TelaCadastro(this);
        switchScene(tela.criarCena(), "VoteFlix - Cadastro");
    }

    public void mostrarTelaMenu() {
        TelaMenu tela = new TelaMenu(this);
        switchScene(tela.criarCena(), "VoteFlix - Menu Principal");
    }

    public void mostrarTelaAdminMenu() {
        TelaAdminMenu tela = new TelaAdminMenu(this);
        switchScene(tela.criarCena(), "VoteFlix - Painel Admin");
    }

    public void mostrarTelaListarFilmes() {
        TelaListarFilmes tela = new TelaListarFilmes(this);
        switchScene(tela.criarCena(), "VoteFlix - Catálogo");
    }

    public void mostrarTelaAdminFilmes() {
        TelaAdminFilmes tela = new TelaAdminFilmes(this);
        switchScene(tela.criarCena(), "VoteFlix - Gerenciar Catálogo");
    }

    public void mostrarTelaAdminUsuarios() {
        TelaAdminUsuarios tela = new TelaAdminUsuarios(this);
        switchScene(tela.criarCena(), "VoteFlix - Gerenciar Usuários");
    }

    public void mostrarTelaMinhasReviews() {
        TelaMinhasReviews tela = new TelaMinhasReviews(this);
        switchScene(tela.criarCena(), "VoteFlix - Minhas Avaliações");
    }
}