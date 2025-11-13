package br.com.voteflix.servidor.model;

import java.util.List;

public class Filme {

    private String id;
    private String titulo;
    private String diretor;
    private String ano;
    private String sinopse;
    private List<String> genero;
    private String nota;
    private String qtd_Avaliacoes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDiretor() {
        return diretor;
    }

    public void setDiretor(String diretor) {
        this.diretor = diretor;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        this.ano = ano;
    }

    public String getSinopse() {
        return sinopse;
    }

    public void setSinopse(String sinopse) {
        this.sinopse = sinopse;
    }

    public List<String> getGenero() {
        return genero;
    }

    public void setGenero(List<String> genero) {
        this.genero = genero;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getQtdAvaliacoes() {
        return qtd_Avaliacoes;
    }

    public void setQtdAvaliacoes(String qtdAvaliacoes) {
        this.qtd_Avaliacoes = qtdAvaliacoes;
    }
}