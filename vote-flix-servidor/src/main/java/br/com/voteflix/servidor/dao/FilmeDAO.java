// vote-flix-servidor/src/main/java/br/com/voteflix/servidor/dao/FilmeDAO.java
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

    // CORRIGIDO: RNF 7.10 - Aceita 'ano' e 'idExcluido' como String, converte para Int
    private boolean verificarDuplicidadeFilme(Connection conn, String titulo, String diretor, String ano, String idExcluido) throws SQLException {
        String sql = "SELECT id FROM filmes WHERE titulo = ? AND diretor = ? AND ano = ?";
        if (idExcluido != null) {
            sql += " AND id != ?";
        }
        sql += " LIMIT 1"; // Só precisamos saber se 1 existe

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, diretor);
            pstmt.setInt(3, Integer.parseInt(ano)); // CONVERTIDO
            if (idExcluido != null) {
                pstmt.setInt(4, Integer.parseInt(idExcluido)); // CONVERTIDO
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Retorna true (duplicado) se encontrou um registro
            }
        }
    }

    // CORRIGIDO: RNF 7.10 - Aceita 'filmeId' como String, converte para Int
    private List<String> obterGenerosPorFilmeId(Connection conn, String filmeId) throws SQLException {
        List<String> generos = new ArrayList<>();
        String sql = "SELECT g.nome FROM generos g JOIN filmes_generos fg ON g.id = fg.genero_id WHERE fg.filme_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(filmeId)); // CONVERTIDO
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    generos.add(rs.getString("nome"));
                }
            }
        }
        return generos;
    }


    // CORRIGIDO: RNF 7.10 - Aceita 'filmeId' como String, converte para Int
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

        // Usa try-with-resources para garantir fechamento da conexão
        try (Connection conn = ConexaoBancoDados.obterConexao()) {
            if (conn == null) return false;

            conn.setAutoCommit(false); // Inicia transação

            try {
                // 1. Excluir reviews associadas (ignora se não houver)
                try (PreparedStatement pstmt = conn.prepareStatement(deleteReviewsSql)) {
                    pstmt.setInt(1, filmeIdInt); // CONVERTIDO
                    pstmt.executeUpdate();
                }

                // 2. Excluir gêneros associados (ignora se não houver)
                try (PreparedStatement pstmt = conn.prepareStatement(deleteGenerosSql)) {
                    pstmt.setInt(1, filmeIdInt); // CONVERTIDO
                    pstmt.executeUpdate();
                }

                // 3. Excluir o filme
                try (PreparedStatement pstmt = conn.prepareStatement(deleteFilmeSql)) {
                    pstmt.setInt(1, filmeIdInt); // CONVERTIDO
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


    // CORRIGIDO: RNF 7.10 - Converte IDs e Ano (Strings) para Int antes de enviar ao DB
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
                // --- Verificação de duplicidade (já aceita Strings) ---
                if (verificarDuplicidadeFilme(conn, filme.getTitulo(), filme.getDiretor(), filme.getAno(), filme.getId())) {
                    conn.rollback();
                    return ERRO_DUPLICADO;
                }

                int filmeIdInt = Integer.parseInt(filme.getId());

                // 1. Atualiza os dados principais do filme
                try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFilmeSql)) {
                    pstmtUpdate.setString(1, filme.getTitulo());
                    pstmtUpdate.setString(2, filme.getDiretor());
                    pstmtUpdate.setInt(3, Integer.parseInt(filme.getAno())); // CONVERTIDO
                    pstmtUpdate.setString(4, filme.getSinopse());
                    pstmtUpdate.setInt(5, filmeIdInt); // CONVERTIDO
                    int affectedRows = pstmtUpdate.executeUpdate();

                    if (affectedRows == 0) {
                        conn.rollback(); // Filme não encontrado, desfaz a transação
                        return ERRO_NAO_ENCONTRADO; // Retorna erro específico
                    }
                } // pstmtUpdate fechado automaticamente

                // 2. Remove os gêneros antigos associados ao filme
                try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteGenerosSql)) {
                    pstmtDelete.setInt(1, filmeIdInt); // CONVERTIDO
                    pstmtDelete.executeUpdate(); // Executa a exclusão
                } // pstmtDelete fechado automaticamente

                // 3. Insere os novos gêneros (usando batch para eficiência)
                try (PreparedStatement pstmtInsert = conn.prepareStatement(insertGenerosSql)) {
                    for (JsonElement genero : generos) {
                        pstmtInsert.setInt(1, filmeIdInt); // CONVERTIDO
                        pstmtInsert.setString(2, genero.getAsString());
                        pstmtInsert.addBatch(); // Adiciona a operação ao lote
                    }
                    pstmtInsert.executeBatch(); // Executa todas as inserções do lote
                } // pstmtInsert fechado automaticamente

                conn.commit(); // Confirma a transação se tudo deu certo
                return SUCESSO;

            } catch (SQLException | NumberFormatException e) { // Adicionado NumberFormatException
                conn.rollback(); // Desfaz a transação em caso de erro
                System.err.println("Erro de SQL ou Formato Numérico ao editar filme: " + e.getMessage());
                return ERRO_SQL;
            }
        } catch (SQLException e) {
            // Erro ao obter conexão ou problemas no fechamento automático
            System.err.println("Erro de conexão ao editar filme: " + e.getMessage());
            return ERRO_CONEXAO; // Retorna erro de conexão
        }
    }

    // CORRIGIDO: RNF 7.10 - Lê dados do DB e seta nos campos String do Modelo
    public List<Filme> listarFilmes() {
        List<Filme> filmes = new ArrayList<>();
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Filme filme = new Filme();
                String filmeId = rs.getString("id"); // Lê como String
                filme.setId(filmeId);
                filme.setTitulo(rs.getString("titulo"));
                filme.setDiretor(rs.getString("diretor"));
                filme.setAno(rs.getString("ano")); // Lê como String
                filme.setSinopse(rs.getString("sinopse"));

                // Busca gêneros separadamente (agora aceita String)
                filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));

                // Converte dados numéricos para String
                filme.setNota(String.format("%.2f", rs.getDouble("nota_media_acumulada")));
                filme.setQtdAvaliacoes(String.valueOf(rs.getInt("total_avaliacoes")));
                filmes.add(filme);
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar filmes: " + e.getMessage());
            return null; // Retorna null em caso de erro
        }
        return filmes;
    }

    // CORRIGIDO: RNF 7.10 - Assinatura aceita String, converte para Int para query
    public Filme obterFilmePorId(String id) {
        Filme filme = null;
        String sql = "SELECT id, titulo, diretor, ano, sinopse, nota_media_acumulada, total_avaliacoes FROM filmes WHERE id = ?";

        try (Connection conn = ConexaoBancoDados.obterConexao();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(id)); // CONVERTIDO
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    filme = new Filme();
                    String filmeId = rs.getString("id"); // Lê como String
                    filme.setId(filmeId);
                    filme.setTitulo(rs.getString("titulo"));
                    filme.setDiretor(rs.getString("diretor"));
                    filme.setAno(rs.getString("ano")); // Lê como String
                    filme.setSinopse(rs.getString("sinopse"));

                    // Busca gêneros separadamente (agora aceita String)
                    filme.setGenero(obterGenerosPorFilmeId(conn, filmeId));

                    // Converte dados numéricos para String
                    filme.setNota(String.format("%.2f", rs.getDouble("nota_media_acumulada")));
                    filme.setQtdAvaliacoes(String.valueOf(rs.getInt("total_avaliacoes")));
                }
            }
        } catch (SQLException | NumberFormatException e) { // Adicionado NumberFormatException
            System.err.println("Erro de SQL ou Formato Numérico ao obter filme por ID: " + e.getMessage());
            return null;
        }
        return filme;
    }

    // CORRIGIDO: RNF 7.10 - Converte Ano (String) para Int para o DB
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

            // Verificação de duplicidade (já aceita Strings)
            if (verificarDuplicidadeFilme(conn, filme.getTitulo(), filme.getDiretor(), filme.getAno(), null)) {
                conn.rollback();
                return ERRO_DUPLICADO;
            }

            int filmeId = -1;

            try (PreparedStatement pstmtFilme = conn.prepareStatement(sqlFilme, Statement.RETURN_GENERATED_KEYS)) {
                pstmtFilme.setString(1, filme.getTitulo());
                pstmtFilme.setString(2, filme.getDiretor());
                pstmtFilme.setInt(3, Integer.parseInt(filme.getAno())); // CONVERTIDO
                pstmtFilme.setString(4, filme.getSinopse());

                if (pstmtFilme.executeUpdate() > 0) {
                    try (ResultSet generatedKeys = pstmtFilme.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            filmeId = generatedKeys.getInt(1); // ID gerado pelo DB é INT
                        } else {
                            throw new SQLException("Falha ao obter o ID do filme criado.");
                        }
                    }
                } else {
                    throw new SQLException("Nenhuma linha afetada ao inserir filme.");
                }
            }

            // O ID do filme (filmeId) é INT, como esperado pela tabela filmes_generos
            try (PreparedStatement pstmtGenero = conn.prepareStatement(sqlGenero)) {
                for (JsonElement genero : generos) {
                    pstmtGenero.setInt(1, filmeId); // OK
                    pstmtGenero.setString(2, genero.getAsString());
                    pstmtGenero.addBatch(); // Adiciona ao lote
                }
                pstmtGenero.executeBatch();
            }

            conn.commit();
            return SUCESSO;

        } catch (SQLException | NumberFormatException e) { // Adicionado NumberFormatException
            // Em caso de qualquer erro SQL, desfaz a transação
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