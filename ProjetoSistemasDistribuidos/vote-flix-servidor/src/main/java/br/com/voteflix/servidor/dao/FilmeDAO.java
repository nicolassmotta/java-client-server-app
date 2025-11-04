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

    private boolean verificarDuplicidadeFilme(Connection conn, String titulo, String diretor, int ano, Integer idExcluido) throws SQLException {
        String sql = "SELECT id FROM filmes WHERE titulo = ? AND diretor = ? AND ano = ?";
        if (idExcluido != null) {
            sql += " AND id != ?";
        }
        sql += " LIMIT 1"; // Só precisamos saber se 1 existe

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, diretor);
            pstmt.setInt(3, ano);
            if (idExcluido != null) {
                pstmt.setInt(4, idExcluido);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Retorna true (duplicado) se encontrou um registro
            }
        }
    }

    private List<String> obterGenerosPorFilmeId(Connection conn, int filmeId) throws SQLException {
        List<String> generos = new ArrayList<>();
        String sql = "SELECT g.nome FROM generos g JOIN filmes_generos fg ON g.id = fg.genero_id WHERE fg.filme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, filmeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generos.add(rs.getString("nome"));
                }
            }
        }
        return generos;
    }


    // --- Métodos excluirFilme e editarFilme permanecem praticamente iguais ---
    // (Eles já usavam transações)
    public boolean excluirFilme(int filmeId) {
        String deleteReviewsSql = "DELETE FROM reviews WHERE filme_id = ?";
        String deleteGenerosSql = "DELETE FROM filmes_generos WHERE filme_id = ?";
        String deleteFilmeSql = "DELETE FROM filmes WHERE id = ?";

        // Usa try-with-resources para garantir fechamento da conexão
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) return false;

            conn.setAutoCommit(false); // Inicia transação

            try {
                // 1. Excluir reviews associadas (ignora se não houver)
                try (PreparedStatement pstmt = conn.prepareStatement(deleteReviewsSql)) {
                    pstmt.setInt(1, filmeId);
                    pstmt.executeUpdate();
                }

                // 2. Excluir gêneros associados (ignora se não houver)
                try (PreparedStatement pstmt = conn.prepareStatement(deleteGenerosSql)) {
                    pstmt.setInt(1, filmeId);
                    pstmt.executeUpdate();
                }

                // 3. Excluir o filme
                try (PreparedStatement pstmt = conn.prepareStatement(deleteFilmeSql)) {
                    pstmt.setInt(1, filmeId);
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0) {
                        conn.rollback(); // Desfaz a transação se o filme não foi encontrado
                        return false;
                    }
                }

                conn.commit(); // Confirma a transação se tudo ocorreu bem
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Desfaz em caso de erro em qualquer etapa
                System.err.println("Erro de SQL ao excluir filme: " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            // Erro ao obter conexão ou fechar recursos (try-with-resources trata o fechamento)
            System.err.println("Erro de conexão ao excluir filme: " + e.getMessage());
            return false;
        }
    }


    public int editarFilme(Filme filme, JsonArray generos) {
        // SQL para atualizar dados básicos do filme
        String updateFilmeSql = "UPDATE filmes SET titulo = ?, diretor = ?, ano = ?, sinopse = ? WHERE id = ?";
        // SQL para remover associações de gênero existentes
        String deleteGenerosSql = "DELETE FROM filmes_generos WHERE filme_id = ?";
        // SQL para inserir novas associações de gênero (busca ID do gênero pelo nome)
        String insertGenerosSql = "INSERT INTO filmes_generos (filme_id, genero_id) VALUES (?, (SELECT id FROM generos WHERE nome = ?))";

        // Usa try-with-resources para a conexão
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) return ERRO_CONEXAO; // Falha ao conectar

            conn.setAutoCommit(false); // Inicia a transação

            try {
                // --- NOVA VERIFICAÇÃO DE DUPLICIDADE ---
                if (verificarDuplicidadeFilme(conn, filme.getTitulo(), filme.getDiretor(), filme.getAno(), filme.getId())) {
                    conn.rollback();
                    return ERRO_DUPLICADO;
                }
                // --- FIM DA VERIFICAÇÃO ---

                // 1. Atualiza os dados principais do filme
                try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFilmeSql)) {
                    pstmtUpdate.setString(1, filme.getTitulo());
                    pstmtUpdate.setString(2, filme.getDiretor());
                    pstmtUpdate.setInt(3, filme.getAno());
                    pstmtUpdate.setString(4, filme.getSinopse());
                    pstmtUpdate.setInt(5, filme.getId());
                    int affectedRows = pstmtUpdate.executeUpdate();

                    if (affectedRows == 0) {
                        conn.rollback(); // Filme não encontrado, desfaz a transação
                        return ERRO_NAO_ENCONTRADO; // Retorna erro específico
                    }
                } // pstmtUpdate fechado automaticamente

                // 2. Remove os gêneros antigos associados ao filme
                try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteGenerosSql)) {
                    pstmtDelete.setInt(1, filme.getId());
                    pstmtDelete.executeUpdate(); // Executa a exclusão
                } // pstmtDelete fechado automaticamente

                // 3. Insere os novos gêneros (usando batch para eficiência)
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertGenerosSql)) {
                    for (JsonElement genero : generos) {
                        pstmtInsert.setInt(1, filme.getId());
                        pstmtInsert.setString(2, genero.getAsString());
                        pstmtInsert.addBatch(); // Adiciona a operação ao lote
                    }
                    pstmtInsert.executeBatch(); // Executa todas as inserções do lote
                } // pstmtInsert fechado automaticamente

                conn.commit(); // Confirma a transação se tudo deu certo
                return SUCESSO;

            } catch (SQLException e) {
                conn.rollback(); // Desfaz a transação em caso de erro
                System.err.println("Erro de SQL ao editar filme: " + e.getMessage());
                return ERRO_SQL;
            }
        } catch (SQLException e) {
            // Erro ao obter conexão ou problemas no fechamento automático
            System.err.println("Erro de conexão ao editar filme: " + e.getMessage());
            return ERRO_CONEXAO; // Retorna erro de conexão
        }
    }

    public List<Filme> listarFilmes() {
        List<Filme> filmes = new ArrayList<>();
        // Query principal sem JOINs desnecessários para média/contagem
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Filme filme = new Filme();
                int filmeId = rs.getInt("id");
                filme.setId(filmeId);
                filme.setTitulo(rs.getString("titulo"));
                filme.setDiretor(rs.getString("diretor"));
                filme.setAno(rs.getInt("ano"));
                filme.setSinopse(rs.getString("sinopse"));

                // Busca gêneros separadamente
                filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));

                // Usa as novas colunas para nota e contagem
                filme.setNota(rs.getDouble("nota_media_acumulada"));
                filme.setQtdAvaliacoes(rs.getInt("total_avaliacoes"));
                filmes.add(filme);
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar filmes: " + e.getMessage());
            return null; // Retorna null em caso de erro
        }
        return filmes;
    }

    public Filme obterFilmePorId(int id) {
        Filme filme = null;
        // Query principal sem JOINs desnecessários para média/contagem
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes WHERE id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    filme = new Filme();
                    int filmeId = rs.getInt("id");
                    filme.setId(filmeId);
                    filme.setTitulo(rs.getString("titulo"));
                    filme.setDiretor(rs.getString("diretor"));
                    filme.setAno(rs.getInt("ano"));
                    filme.setSinopse(rs.getString("sinopse"));

                    // Busca gêneros separadamente
                    filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));

                    // Usa as novas colunas para nota e contagem
                    filme.setNota(rs.getDouble("nota_media_acumulada"));
                    filme.setQtdAvaliacoes(rs.getInt("total_avaliacoes"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao obter filme por ID: " + e.getMessage());
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
                pstmtFilme.setInt(3, filme.getAno());
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
                    pstmtGenero.addBatch(); // Adiciona ao lote
                }
                pstmtGenero.executeBatch();
            }

            conn.commit();
            return SUCESSO;

        } catch (SQLException e) {
            // Em caso de qualquer erro SQL, desfaz a transação
            System.err.println("Erro de SQL ao criar filme: " + e.getMessage());
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