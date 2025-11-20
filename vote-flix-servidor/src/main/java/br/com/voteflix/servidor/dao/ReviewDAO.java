package br.com.voteflix.servidor.dao;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.model.Review;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    private int[] obterNotaEFilmeIdReview(Connection conn, String reviewId) throws SQLException {
        String sql = "SELECT nota, filme_id FROM reviews WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(reviewId));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("nota"), rs.getInt("filme_id")};
                }
            }
        }
        return null;
    }

    private boolean atualizarMediaFilme(Connection conn, int filmeId, int notaNova, int notaAntiga) throws SQLException {

        String selectFilmeSql = "SELECT nota_media_acumulada, total_avaliacoes FROM filmes WHERE id = ?";
        double oldAvg = 0.0;
        int oldCount = 0;
        try (PreparedStatement pstmtSelect = conn.prepareStatement(selectFilmeSql)) {
            pstmtSelect.setInt(1, filmeId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    oldAvg = rs.getDouble("nota_media_acumulada");
                    oldCount = rs.getInt("total_avaliacoes");
                } else {
                    System.err.println("Filme com ID " + filmeId + " não encontrado para atualizar média.");
                    return false;
                }
            }
        }

        double newAvg;
        int newCount;

        if (notaNova > 0 && notaAntiga == 0) {
            newCount = oldCount + 1;
            newAvg = ((oldAvg * oldCount) + notaNova) / newCount;
        } else if (notaNova == 0 && notaAntiga > 0) {
            newCount = oldCount - 1;
            if (newCount <= 0) {
                newAvg = 0.0;
                newCount = 0;
            } else {
                newAvg = ((oldAvg * oldCount) - notaAntiga) / newCount;
            }
        } else if (notaNova > 0 && notaAntiga > 0) {
            newCount = oldCount;
            if (newCount <= 0) {
                newAvg = notaNova;
                newCount = 1;
            } else {
                newAvg = ((oldAvg * oldCount) - notaAntiga + notaNova) / newCount;
            }
        } else {
            return true;
        }

        newAvg = Math.round(newAvg * 100.0) / 100.0;

        String updateFilmeSql = "UPDATE filmes SET nota_media_acumulada = ?, total_avaliacoes = ? WHERE id = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFilmeSql)) {
            pstmtUpdate.setDouble(1, newAvg);
            pstmtUpdate.setInt(2, newCount);
            pstmtUpdate.setInt(3, filmeId);
            return pstmtUpdate.executeUpdate() > 0;
        }
    }

    boolean recalcularMediaFilme(Connection conn, int filmeId, int notaNova, int notaAntiga) throws SQLException {
        return atualizarMediaFilme(conn, filmeId, notaNova, notaAntiga);
    }

    public boolean excluirReview(String reviewId, int usuarioId, String funcao) {
        String reviewSql;
        boolean isAdmin = "admin".equals(funcao);
        if (isAdmin) {
            reviewSql = "DELETE FROM reviews WHERE id = ?";
        } else {
            reviewSql = "DELETE FROM reviews WHERE id = ? AND usuario_id = ?";
        }

        Connection conn = null;
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) return false;
            conn.setAutoCommit(false);
            int reviewIdInt = Integer.parseInt(reviewId);
            int[] dadosAntigos = obterNotaEFilmeIdReview(conn, reviewId);

            if (dadosAntigos == null) {
                conn.rollback();
                return false;
            }

            int notaAntiga = dadosAntigos[0];
            int filmeId = dadosAntigos[1];
            int affectedRows;

            try (PreparedStatement pstmt = conn.prepareStatement(reviewSql)) {
                pstmt.setInt(1, reviewIdInt);
                if (!isAdmin) {
                    pstmt.setInt(2, usuarioId);
                }
                affectedRows = pstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                boolean mediaAtualizada = atualizarMediaFilme(conn, filmeId, 0, notaAntiga);
                if (mediaAtualizada) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Falha ao atualizar média após excluir review " + reviewId);
                    return false;
                }
            } else {
                conn.rollback();
                return false;
            }

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao excluir review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }

    public boolean editarReview(Review review, int usuarioId) {
        String sql = "UPDATE reviews SET titulo = ?, comentario = ?, nota = ?, editado = ? WHERE id = ? AND usuario_id = ?";

        Connection conn = null;
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) return false;

            conn.setAutoCommit(false);

            int[] dadosAntigos = obterNotaEFilmeIdReview(conn, review.getId());
            if (dadosAntigos == null) {
                conn.rollback();
                System.err.println("Review com ID " + review.getId() + " não encontrada para edição.");
                return false;
            }

            int notaAntiga = dadosAntigos[0];
            int filmeId = dadosAntigos[1];
            int notaNova = Integer.parseInt(review.getNota());
            int affectedRows;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, review.getTitulo());
                pstmt.setString(2, review.getDescricao());
                pstmt.setInt(3, notaNova);
                pstmt.setString(4, "true");
                pstmt.setInt(5, Integer.parseInt(review.getId()));
                pstmt.setInt(6, usuarioId);
                affectedRows = pstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                boolean mediaAtualizada = atualizarMediaFilme(conn, filmeId, notaNova, notaAntiga);
                if (mediaAtualizada) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Falha ao atualizar média após editar review " + review.getId());
                    return false;
                }
            } else {
                conn.rollback();
                System.err.println("Nenhuma linha afetada ao editar review ID " + review.getId());
                return false;
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao editar review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }

    public List<Review> listarReviewsPorFilme(String filmeId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.id, r.filme_id, u.login AS nome_usuario, r.nota, r.titulo, r.comentario AS descricao, r.data, r.editado " +
                "FROM reviews r " +
                "JOIN usuarios u ON r.usuario_id = u.id " +
                "WHERE r.filme_id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(filmeId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapearReview(rs));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao listar reviews por filme: " + e.getMessage());
            return null;
        }
        return reviews;
    }

    public List<Review> listarReviewsPorUsuario(int usuarioId) {
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT r.id, r.filme_id, u.login AS nome_usuario, r.nota, r.titulo, r.comentario AS descricao, r.data, r.editado " +
                "FROM reviews r " +
                "JOIN usuarios u ON r.usuario_id = u.id " +
                "WHERE r.usuario_id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapearReview(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar reviews por usuário: " + e.getMessage());
            return null;
        }
        return reviews;
    }

    public boolean criarReview(Review review) {
        String checkSql = "SELECT id FROM reviews WHERE usuario_id = ? AND filme_id = ?";
        String insertSql = "INSERT INTO reviews (filme_id, usuario_id, titulo, comentario, nota, data, editado) VALUES (?, ?, ?, ?, ?, NOW(), ?)";

        Connection conn = null;

        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) return false;

            conn.setAutoCommit(false);

            int usuarioIdInt = Integer.parseInt(review.getId());
            int filmeIdInt = Integer.parseInt(review.getIdFilme());
            int notaInt = Integer.parseInt(review.getNota());

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, usuarioIdInt);
                checkStmt.setInt(2, filmeIdInt);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.err.println("Usuário já avaliou este filme.");
                        conn.rollback();
                        return false;
                    }
                }
            }

            int affectedRows;

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, filmeIdInt);
                insertStmt.setInt(2, usuarioIdInt);
                insertStmt.setString(3, review.getTitulo());
                insertStmt.setString(4, review.getDescricao());
                insertStmt.setInt(5, notaInt);
                insertStmt.setString(6, "false");

                affectedRows = insertStmt.executeUpdate();
            }

            if (affectedRows > 0) {
                boolean mediaAtualizada = atualizarMediaFilme(conn, filmeIdInt, notaInt, 0);
                if(mediaAtualizada) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Falha ao atualizar média após criar review para filme " + review.getIdFilme());
                    return false;
                }
            } else {
                conn.rollback();
                return false;
            }

        } catch (SQLException | NumberFormatException e) {
            System.err.println("Erro de SQL ou Formato Numérico ao criar review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }

    private Review mapearReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setId(rs.getString("id"));
        review.setIdFilme(rs.getString("filme_id"));
        review.setNomeUsuario(rs.getString("nome_usuario"));
        review.setNota(rs.getString("nota"));
        review.setTitulo(rs.getString("titulo"));
        review.setDescricao(rs.getString("descricao"));
        review.setData(rs.getTimestamp("data"));
        String editadoBanco = rs.getString("editado");
        review.setEditado(editadoBanco != null ? editadoBanco : "false");

        return review;
    }
}