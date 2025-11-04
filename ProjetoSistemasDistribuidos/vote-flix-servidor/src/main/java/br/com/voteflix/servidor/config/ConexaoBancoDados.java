package br.com.voteflix.servidor.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexaoBancoDados {

    private static final String URL;
    private static final String USUARIO;
    private static final String SENHA;

    // Bloco estático para carregar as propriedades UMA VEZ
    static {
        Properties prop = new Properties();
        String urlTemp = null;
        String userTemp = null;
        String passTemp = null;

        // Tenta carregar o arquivo 'db.properties' do 'resources'
        try (InputStream input = ConexaoBancoDados.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (input == null) {
                System.out.println("ERRO: Desculpe, não foi possível encontrar o 'db.properties'.");
                // Lança uma exceção para parar a execução se o arquivo não for encontrado
                throw new RuntimeException("Arquivo 'db.properties' não encontrado no classpath");
            }

            // Carrega as propriedades
            prop.load(input);

            // Atribui os valores às variáveis finais
            urlTemp = prop.getProperty("db.url");
            userTemp = prop.getProperty("db.user");
            passTemp = prop.getProperty("db.password");

        } catch (Exception e) {
            e.printStackTrace();
            // Lança uma exceção se houver erro ao ler o arquivo
            throw new RuntimeException("Erro ao carregar 'db.properties'", e);
        }

        // Atribui os valores carregados às constantes
        URL = urlTemp;
        USUARIO = userTemp;
        SENHA = passTemp;
    }

    public static Connection obterConexao() {
        try {
            // Usa as constantes carregadas do arquivo
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter conexão com o banco de dados.", e);
        }
    }
}