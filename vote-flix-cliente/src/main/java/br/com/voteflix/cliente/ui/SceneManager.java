package br.com.voteflix.cliente.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage primaryStage;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void switchScene(Scene newScene, String title) {
        double currentX = primaryStage.getX();
        double currentY = primaryStage.getY();
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();
        boolean isMaximized = primaryStage.isMaximized();

        primaryStage.setScene(newScene);
        primaryStage.setTitle(title);

        if (isMaximized) {
            primaryStage.setMaximized(true);
        } else {

            if (!Double.isNaN(currentX) && !Double.isNaN(currentY)) {
                primaryStage.setX(currentX);
                primaryStage.setY(currentY);
            } else {
                primaryStage.centerOnScreen();
            }

            if (!Double.isNaN(currentWidth) && !Double.isNaN(currentHeight) && currentWidth > 0 && currentHeight > 0) {
                primaryStage.setWidth(currentWidth);
                primaryStage.setHeight(currentHeight);
            }
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