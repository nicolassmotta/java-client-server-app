package br.com.voteflix.servidor.model;

import java.util.Date;

public class Review {

    private String id;
    private String id_filme;
    private String nome_usuario;
    private String nota;
    private String titulo;
    private String descricao;
    private Date data;
    private String editado;

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdFilme() {
        return id_filme;
    }

    public void setIdFilme(String idFilme) {
        this.id_filme = idFilme;
    }

    public String getNomeUsuario() {
        return nome_usuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nome_usuario = nomeUsuario;
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