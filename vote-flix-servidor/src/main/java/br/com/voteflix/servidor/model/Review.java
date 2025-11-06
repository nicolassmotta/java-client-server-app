// vote-flix-servidor/src/main/java/br/com/voteflix/servidor/model/Review.java
package br.com.voteflix.servidor.model;

import java.util.Date;

public class Review {

    // CORRIGIDO: RNF 7.10 - IDs e Nota são String
    private String id;
    private String idFilme;
    private String nomeUsuario;
    private String nota;
    private String titulo;
    private String descricao;
    private Date data;

    // Getters e Setters

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdFilme() {
        return idFilme;
    }

    public void setIdFilme(String idFilme) {
        this.idFilme = idFilme;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }
}