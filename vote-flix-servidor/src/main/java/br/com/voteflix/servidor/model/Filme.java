// vote-flix-servidor/src/main/java/br/com/voteflix/servidor/model/Filme.java
package br.com.voteflix.servidor.model;

import java.util.List;

public class Filme {

    // CORRIGIDO: RNF 7.10 - Todos os campos são String
    private String id;
    private String titulo;
    private String diretor;
    private String ano;
    private String sinopse;
    private List<String> genero;
    private String nota;
    private String qtdAvaliacoes;

    // Getters e Setters

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
        return qtdAvaliacoes;
    }

    public void setQtdAvaliacoes(String qtdAvaliacoes) {
        this.qtdAvaliacoes = qtdAvaliacoes;
    }
}