package br.com.voteflix.cliente.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage primaryStage;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void switchScene(Scene newScene, String title) {
        primaryStage.setScene(newScene);
        primaryStage.setTitle(title);

        if (!primaryStage.isShowing()) {
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } else {

            double widthAtual = primaryStage.getWidth();
            double heightAtual = primaryStage.getHeight();


            primaryStage.setWidth(widthAtual + 0.001);
            primaryStage.setHeight(heightAtual + 0.001);

            primaryStage.setWidth(widthAtual);
            primaryStage.setHeight(heightAtual);
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
        switchScene(tela.criarCena(), "VoteFlix - Menu");
    }

    public void mostrarTelaAdminMenu() {
        TelaAdminMenu tela = new TelaAdminMenu(this);
        switchScene(tela.criarCena(), "VoteFlix - Painel Admin");
    }

    public void mostrarTelaListarFilmes() {
        TelaListarFilmes tela = new TelaListarFilmes(this);
        switchScene(tela.criarCena(), "VoteFlix - Filmes");
    }

    public void mostrarTelaAdminFilmes() {
        TelaAdminFilmes tela = new TelaAdminFilmes(this);
        switchScene(tela.criarCena(), "VoteFlix - Gerenciar Filmes");
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