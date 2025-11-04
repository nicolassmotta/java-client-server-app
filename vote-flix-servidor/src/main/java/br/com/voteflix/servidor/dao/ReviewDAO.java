package br.com.voteflix.servidor.dao;

import br.com.voteflix.servidor.config.ConexaoBancoDados;
import br.com.voteflix.servidor.model.Review;

import java.sql.*; // Import Statement
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    // Método auxiliar para buscar a nota atual e o filmeId de uma review
    private int[] obterNotaEFilmeIdReview(Connection conn, int reviewId) throws SQLException {
        String sql = "SELECT nota, filme_id FROM reviews WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("nota"), rs.getInt("filme_id")};
                }
            }
        }
        return null; // Retorna null se a review não for encontrada
    }

    // Método centralizado para atualizar a média do filme
    private boolean atualizarMediaFilme(Connection conn, int filmeId, int notaNova, int notaAntiga) throws SQLException {
        // Se notaNova == 0 e notaAntiga > 0 -> Exclusão
        // Se notaNova > 0 e notaAntiga == 0 -> Criação
        // Se notaNova > 0 e notaAntiga > 0 -> Edição

        // 1. Obter dados atuais do filme
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
                    return false; // Filme não existe
                }
            }
        }

        // 2. Calcular novos valores
        double newAvg;
        int newCount;

        if (notaNova > 0 && notaAntiga == 0) { // Criação
            newCount = oldCount + 1;
            newAvg = ((oldAvg * oldCount) + notaNova) / newCount;
        } else if (notaNova == 0 && notaAntiga > 0) { // Exclusão
            newCount = oldCount - 1;
            if (newCount <= 0) {
                newAvg = 0.0;
                newCount = 0; // Garante que não fique negativo
            } else {
                newAvg = ((oldAvg * oldCount) - notaAntiga) / newCount;
            }
        } else if (notaNova > 0 && notaAntiga > 0) { // Edição
            newCount = oldCount; // Contagem não muda na edição
            if (newCount <= 0) { // Caso raro, mas seguro
                newAvg = notaNova; // Apenas a nova nota conta
                newCount = 1;
            } else {
                newAvg = ((oldAvg * oldCount) - notaAntiga + notaNova) / newCount;
            }
        } else {
            // Caso inválido (notaNova e notaAntiga são 0), não faz nada
            return true; // Não houve erro, mas nada a fazer
        }

        // Arredondamento seguro para evitar problemas de precisão
        newAvg = Math.round(newAvg * 100.0) / 100.0;


        // 3. Atualizar filme
        String updateFilmeSql = "UPDATE filmes SET nota_media_acumulada = ?, total_avaliacoes = ? WHERE id = ?";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFilmeSql)) {
            pstmtUpdate.setDouble(1, newAvg);
            pstmtUpdate.setInt(2, newCount);
            pstmtUpdate.setInt(3, filmeId);
            return pstmtUpdate.executeUpdate() > 0;
        }
    }


    public boolean excluirReview(int reviewId, int usuarioId, String funcao) {
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

            conn.setAutoCommit(false); // Inicia transação

            // 1. Obter nota antiga e filmeId ANTES de excluir
            int[] dadosAntigos = obterNotaEFilmeIdReview(conn, reviewId);
            if (dadosAntigos == null) {
                conn.rollback(); // Review não existe
                return false;
            }
            int notaAntiga = dadosAntigos[0];
            int filmeId = dadosAntigos[1];

            // 2. Excluir a review
            int affectedRows;
            try (PreparedStatement pstmt = conn.prepareStatement(reviewSql)) {
                pstmt.setInt(1, reviewId);
                if (!isAdmin) {
                    pstmt.setInt(2, usuarioId);
                }
                affectedRows = pstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                // 3. Atualizar a média do filme
                boolean mediaAtualizada = atualizarMediaFilme(conn, filmeId, 0, notaAntiga);
                if (mediaAtualizada) {
                    conn.commit(); // Confirma se tudo deu certo
                    return true;
                } else {
                    conn.rollback(); // Desfaz se a atualização da média falhar
                    System.err.println("Falha ao atualizar média após excluir review " + reviewId);
                    return false;
                }
            } else {
                conn.rollback(); // Review não encontrada ou não pertence ao usuário
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao excluir review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }

    public boolean editarReview(Review review, int usuarioId) {
        String sql = "UPDATE reviews SET titulo = ?, comentario = ?, nota = ? WHERE id = ? AND usuario_id = ?";
        Connection conn = null;
        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) return false;

            conn.setAutoCommit(false); // Inicia transação

            // 1. Obter nota antiga e filmeId ANTES de editar
            int[] dadosAntigos = obterNotaEFilmeIdReview(conn, review.getId());
            if (dadosAntigos == null) {
                conn.rollback(); // Review não existe
                System.err.println("Review com ID " + review.getId() + " não encontrada para edição.");
                return false;
            }
            int notaAntiga = dadosAntigos[0];
            int filmeId = dadosAntigos[1]; // Usado para atualizar a média

            // 2. Editar a review
            int affectedRows;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, review.getTitulo());
                pstmt.setString(2, review.getDescricao());
                pstmt.setInt(3, review.getNota());
                pstmt.setInt(4, review.getId());
                pstmt.setInt(5, usuarioId);
                affectedRows = pstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                // 3. Atualizar a média do filme
                boolean mediaAtualizada = atualizarMediaFilme(conn, filmeId, review.getNota(), notaAntiga);
                if (mediaAtualizada) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Falha ao atualizar média após editar review " + review.getId());
                    return false;
                }
            } else {
                conn.rollback(); // Review não encontrada ou não pertence ao usuário
                System.err.println("Nenhuma linha afetada ao editar review ID " + review.getId() + " para usuário ID " + usuarioId);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao editar review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }

    // --- Métodos listarReviewsPorFilme e listarReviewsPorUsuario permanecem iguais ---
    // (Não precisam de transação e não modificam dados)
    public List<Review> listarReviewsPorFilme(int filmeId) {
        List<Review> reviews = new ArrayList<>();
        // O nome da coluna no banco é 'comentario', mas o model usa 'descricao'
        String sql = "SELECT r.id, r.filme_id, u.login AS nome_usuario, r.nota, r.titulo, r.comentario AS descricao, r.data " +
                "FROM reviews r " +
                "JOIN usuarios u ON r.usuario_id = u.id " +
                "WHERE r.filme_id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, filmeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setId(rs.getInt("id"));
                    review.setIdFilme(rs.getInt("filme_id"));
                    review.setNomeUsuario(rs.getString("nome_usuario"));
                    review.setNota(rs.getInt("nota"));
                    review.setTitulo(rs.getString("titulo"));
                    review.setDescricao(rs.getString("descricao")); // Mapeando 'comentario' para 'descricao'
                    review.setData(rs.getTimestamp("data"));
                    reviews.add(review);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar reviews por filme: " + e.getMessage());
            return null;
        }
        return reviews;
    }

    public List<Review> listarReviewsPorUsuario(int usuarioId) {
        List<Review> reviews = new ArrayList<>();
        // O nome da coluna no banco é 'comentario', mas o model usa 'descricao'
        String sql = "SELECT r.id, r.filme_id, u.login AS nome_usuario, r.nota, r.titulo, r.comentario AS descricao, r.data " +
                "FROM reviews r " +
                "JOIN usuarios u ON r.usuario_id = u.id " +
                "WHERE r.usuario_id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, usuarioId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Review review = new Review();
                    review.setId(rs.getInt("id"));
                    review.setIdFilme(rs.getInt("filme_id"));
                    review.setNomeUsuario(rs.getString("nome_usuario"));
                    review.setNota(rs.getInt("nota"));
                    review.setTitulo(rs.getString("titulo"));
                    review.setDescricao(rs.getString("descricao")); // Mapeando 'comentario' para 'descricao'
                    review.setData(rs.getTimestamp("data"));
                    reviews.add(review);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar reviews por usuário: " + e.getMessage());
            return null; // Retorna null em caso de erro
        }
        return reviews;
    }

    public boolean criarReview(Review review) {
        String checkSql = "SELECT id FROM reviews WHERE usuario_id = ? AND filme_id = ?";
        // Corrigido nome da coluna 'comentario'
        String insertSql = "INSERT INTO reviews (filme_id, usuario_id, titulo, comentario, nota, data) VALUES (?, ?, ?, ?, ?, NOW())";
        Connection conn = null;

        try {
            conn = ConexaoBancoDados.obterConexao();
            if (conn == null) return false;

            conn.setAutoCommit(false); // Inicia transação

            // 1: Verificar duplicidade
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, review.getId());
                checkStmt.setInt(2, review.getIdFilme());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.err.println("Usuário já avaliou este filme.");
                        conn.rollback(); // Desfaz, pois não vai inserir
                        return false; // Conflito
                    }
                }
            }

            // 2: Inserir a nova review
            int affectedRows;
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, review.getIdFilme());
                insertStmt.setInt(2, review.getId());
                insertStmt.setString(3, review.getTitulo());
                insertStmt.setString(4, review.getDescricao()); // Usando getDescricao() para a coluna 'comentario'
                insertStmt.setInt(5, review.getNota());
                affectedRows = insertStmt.executeUpdate();
            }

            if (affectedRows > 0) {
                // 3: Atualizar a média do filme (notaAntiga é 0 para criação)
                boolean mediaAtualizada = atualizarMediaFilme(conn, review.getIdFilme(), review.getNota(), 0);
                if(mediaAtualizada) {
                    conn.commit(); // Confirma tudo
                    return true;
                } else {
                    conn.rollback();
                    System.err.println("Falha ao atualizar média após criar review para filme " + review.getIdFilme());
                    return false;
                }
            } else {
                conn.rollback(); // Falha na inserção por algum motivo
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Erro de SQL ao criar review: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* Ignora */}
            return false;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { /* Ignora */}
        }
    }
}