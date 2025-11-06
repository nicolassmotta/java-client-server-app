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

    static {
        Properties prop = new Properties();
        String urlTemp = null;
        String userTemp = null;
        String passTemp = null;

        try (InputStream input = ConexaoBancoDados.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (input == null) {
                System.out.println("ERRO: Desculpe, não foi possível encontrar o 'db.properties'.");
                throw new RuntimeException("Arquivo 'db.properties' não encontrado no classpath");
            }
            prop.load(input);
            urlTemp = prop.getProperty("db.url");
            userTemp = prop.getProperty("db.user");
            passTemp = prop.getProperty("db.password");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao carregar 'db.properties'", e);
        }

        URL = urlTemp;
        USUARIO = userTemp;
        SENHA = passTemp;
    }

    public static Connection obterConexao() {
        try {
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter conexão com o banco de dados.", e);
        }
    }
}