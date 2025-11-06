package br.com.voteflix.servidor.dao;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.model.Filme;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FilmeDAO {

    public static final int SUCESSO = 1;
    public static final int ERRO_DUPLICADO = 2;
    public static final int ERRO_SQL = 3;
    public static final int ERRO_CONEXAO = 4;
    public static final int ERRO_NAO_ENCONTRADO = 5;

    private boolean verificarDuplicidadeFilme(Connection conn, String titulo, String diretor, String ano, String idExcluido) throws SQLException {
        String sql = "SELECT id FROM filmes WHERE titulo = ? AND diretor = ? AND ano = ?";
        if (idExcluido != null) {
            sql += " AND id != ?";
        }
        sql += " LIMIT 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, diretor);
            pstmt.setInt(3, Integer.parseInt(ano));
            if (idExcluido != null) {
                pstmt.setInt(4, Integer.parseInt(idExcluido));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<String> obterGenerosPorFilmeId(Connection conn, String filmeId) throws SQLException {
        List<String> generos = new ArrayList<>();
        String sql = "SELECT g.nome FROM generos g JOIN filmes_generos fg ON g.id = fg.genero_id WHERE fg.filme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(filmeId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generos.add(rs.getString("nome"));
                }
            }
        }
        return generos;
    }

    public boolean excluirFilme(String filmeId) {
        String deleteReviewsSql = "DELETE FROM reviews WHERE filme_id = ?";
        String deleteGenerosSql = "DELETE FROM filmes_generos WHERE filme_id = ?";
        String deleteFilmeSql = "DELETE FROM filmes WHERE id = ?";
        int filmeIdInt;

        try {
            filmeIdInt = Integer.parseInt(filmeId);
        } catch (NumberFormatException e) {
            System.err.println("ID de filme inválido para excluir: " + filmeId);
            return false;
        }

        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) return false;

            conn.setAutoCommit(false);

            try {
                try (PreparedStatement pstmt = conn.prepareStatement(deleteReviewsSql)) {
                    pstmt.setInt(1, filmeIdInt);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(deleteGenerosSql)) {
                    pstmt.setInt(1, filmeIdInt);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(deleteFilmeSql)) {
                    pstmt.setInt(1, filmeIdInt);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Erro de SQL ao excluir filme: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro de conexão ao excluir filme: " + e.getMessage());
            return false;
        }
    }

    public int editarFilme(Filme filme, JsonArray generos) {

        String updateFilmeSql = "UPDATE filmes SET titulo = ?, diretor = ?, ano = ?, sinopse = ? WHERE id = ?";
        String deleteGenerosSql = "DELETE FROM filmes_generos WHERE filme_id = ?";
        String insertGenerosSql = "INSERT INTO filmes_generos (filme_id, genero_id) VALUES (?, (SELECT id FROM generos WHERE nome = ?))";

        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) return ERRO_CONEXAO;

            conn.setAutoCommit(false);

            try {
                if (verificarDuplicidadeFilme(conn, filme.getTitulo(), filme.getDiretor(), filme.getAno(), filme.getId())) {
                    conn.rollback();
                    return ERRO_DUPLICADO;
                }

                int filmeIdInt = Integer.parseInt(filme.getId());

                try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFilmeSql)) {
                    pstmtUpdate.setString(1, filme.getTitulo());
                    pstmtUpdate.setString(2, filme.getDiretor());
                    pstmtUpdate.setInt(3, Integer.parseInt(filme.getAno()));
                    pstmtUpdate.setString(4, filme.getSinopse());
                    pstmtUpdate.setInt(5, filmeIdInt);
                    int affectedRows = pstmtUpdate.executeUpdate();

                    if (affectedRows == 0) {
                        conn.rollback();
                        return ERRO_NAO_ENCONTRADO;
                    }
                }

                try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteGenerosSql)) {
                    pstmtDelete.setInt(1, filmeIdInt);
                    pstmtDelete.executeUpdate();
                }

                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertGenerosSql)) {
                    for (JsonElement genero : generos) {
                        pstmtInsert.setInt(1, filmeIdInt);
                        pstmtInsert.setString(2, genero.getAsString());
                        pstmtInsert.addBatch();
                    }
                    pstmtInsert.executeBatch();
                }

                conn.commit();
                return SUCESSO;

            } catch (SQLException | NumberFormatException e) {
                conn.rollback();
                System.err.println("Erro de SQL ou Formato Numérico ao editar filme: " + e.getMessage());
                return ERRO_SQL;
            }
        } catch (SQLException e) {

            System.err.println("Erro de conexão ao editar filme: " + e.getMessage());
            return ERRO_CONEXAO;
        }
    }

    public List<Filme> listarFilmes() {
        List<Filme> filmes = new ArrayList<>();
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Filme filme = new Filme();
                String filmeId = rs.getString("id");
                filme.setId(filmeId);
                filme.setTitulo(rs.getString("titulo"));
                filme.setDiretor(rs.getString("diretor"));
                filme.setAno(rs.getString("ano"));
                filme.setSinopse(rs.getString("sinopse"));
                filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));
                filme.setNota(String.format("%.2f", rs.getDouble("nota_media_acumulada")));
                filme.setQtdAvaliacoes(String.valueOf(rs.getInt("total_avaliacoes")));
                filmes.add(filme);
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar filmes: " + e.getMessage());
            return null;
        }
        return filmes;
    }

    public Filme obterFilmePorId(String id) {
        Filme filme = null;
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes WHERE id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    filme = new Filme();
                    String filmeId = rs.getString("id");
                    filme.setId(filmeId);
                    filme.setTitulo(rs.getString("titulo"));
                    filme.setDiretor(rs.getString("diretor"));
                    filme.setAno(rs.getString("ano"));
                    filme.setSinopse(rs.getString("sinopse"));
                    filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));
                    filme.setNota(String.format("%.2f", rs.getDouble("nota_media_acumulada")));
                    filme.setQtdAvaliacoes(String.valueOf(rs.getInt("total_avaliacoes")));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao obter filme por ID: " + e.getMessage());
            return null;
        }
        return filme;
    }

    public int criarFilme(Filme filme, JsonArray generos) {
        String sqlFilme = "INSERT INTO filmes (titulo, diretor, ano, sinopse) VALUES (?, ?, ?, ?)";
        String sqlGenero = "INSERT INTO filmes_generos (filme_id, genero_id) VALUES (?, (SELECT id FROM generos WHERE nome = ?))";

        Connection conn = null;
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) {
                return ERRO_CONEXAO;
            }
            conn.setAutoCommit(false);

            if (verificarDuplicidadeFilme(conn, filme.getTitulo(), filme.getDiretor(), filme.getAno(), null)) {
                conn.rollback();
                return ERRO_DUPLICADO;
            }

            int filmeId = -1;

            try (PreparedStatement pstmtFilme = conn.prepareStatement(sqlFilme, Statement.RETURN_GENERATED_KEYS)) {
                pstmtFilme.setString(1, filme.getTitulo());
                pstmtFilme.setString(2, filme.getDiretor());
                pstmtFilme.setInt(3, Integer.parseInt(filme.getAno()));
                pstmtFilme.setString(4, filme.getSinopse());

                if (pstmtFilme.executeUpdate() > 0) {
                    try (ResultSet generatedKeys = pstmtFilme.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            filmeId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Falha ao obter o ID do filme criado.");
                        }
                    }
                } else {
                    throw new SQLException("Nenhuma linha afetada ao inserir filme.");
                }
            }

            try (PreparedStatement pstmtGenero = conn.prepareStatement(sqlGenero)) {
                for (JsonElement genero : generos) {
                    pstmtGenero.setInt(1, filmeId);
                    pstmtGenero.setString(2, genero.getAsString());
                    pstmtGenero.addBatch();
                }
                pstmtGenero.executeBatch();
            }

            conn.commit();
            return SUCESSO;

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao criar filme: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Erro ao reverter transação: " + ex.getMessage());
            }
            return ERRO_SQL;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
}