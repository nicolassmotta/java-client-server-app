package br.com.voteflix.cliente.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private final Stage primaryStage;

    public SceneManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private void switchScene(Scene newScene, String title) {
        // 1. Captura o estado ATUAL da janela antes de trocar
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();
        boolean isMaximized = primaryStage.isMaximized();

        // Se for a primeira vez abrindo (valores inválidos), define o padrão
        if (Double.isNaN(currentWidth) || currentWidth < 100) currentWidth = 800;
        if (Double.isNaN(currentHeight) || currentHeight < 100) currentHeight = 600;

        // 2. Troca a cena
        primaryStage.setScene(newScene);
        primaryStage.setTitle(title);

        // 3. Reaplica o tamanho/estado anterior
        if (isMaximized) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setWidth(currentWidth);
            primaryStage.setHeight(currentHeight);
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