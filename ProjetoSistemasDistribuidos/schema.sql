-- Garante que você está usando o banco de dados correto.
-- Lembre-se de criá-lo primeiro com: CREATE DATABASE voteflix_db;
USE voteflix_db;

-- Remove tabelas existentes para permitir uma recriação limpa (ordem de dependência)
DROP TABLE IF EXISTS filmes_generos;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS generos;
DROP TABLE IF EXISTS filmes;
DROP TABLE IF EXISTS usuarios;

-- Tabela de Usuários
-- Baseado em: UsuarioDAO.java e Usuario.java
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(25) NOT NULL UNIQUE,  -- O cliente valida 3-20 caracteres
    senha VARCHAR(255) NOT NULL         -- DAO armazena a senha (recomendado usar hash)
);

-- Tabela de Filmes
-- Baseado em: FilmeDAO.java e Filme.java
CREATE TABLE filmes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,        -- O cliente valida max 100
    diretor VARCHAR(100) NOT NULL,       -- O cliente valida max 100
    ano INT NOT NULL,
    sinopse TEXT,                        -- O cliente valida max 500
    nota_media_acumulada DECIMAL(3, 2) DEFAULT 0.00, -- Calculado pelo ReviewDAO
    total_avaliacoes INT DEFAULT 0,                 -- Calculado pelo ReviewDAO

    -- Constraint para evitar filmes duplicados, como verificado no FilmeDAO
    UNIQUE KEY uq_filme_unico (titulo(100), diretor(100), ano)
);

-- Tabela de Gêneros
-- Baseado em: FilmeDAO.java (obterGenerosPorFilmeId)
CREATE TABLE generos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

-- Tabela de Reviews (Avaliações)
-- Baseado em: ReviewDAO.java e Review.java
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filme_id INT NOT NULL,
    usuario_id INT NOT NULL,
    titulo VARCHAR(50) NOT NULL,        -- O cliente valida max 50
    comentario VARCHAR(255) NOT NULL,   -- O cliente valida max 250 (nome 'descricao' no modelo)
    nota INT NOT NULL,
    data TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- DAO usa NOW()

    -- Constraint para garantir que um usuário só avalie um filme uma vez
    UNIQUE KEY uq_usuario_filme (usuario_id, filme_id),

    -- Chaves estrangeiras
    FOREIGN KEY (filme_id) REFERENCES filmes(id)
        ON DELETE CASCADE, -- Confirmado por FilmeDAO.excluirFilme
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE  -- Confirmado por UsuarioDAO.apagarUsuario
);

-- Tabela de Junção Filmes <-> Gêneros
-- Baseado em: FilmeDAO.java (obterGenerosPorFilmeId, editarFilme)
CREATE TABLE filmes_generos (
    filme_id INT NOT NULL,
    genero_id INT NOT NULL,

    PRIMARY KEY (filme_id, genero_id),

    FOREIGN KEY (filme_id) REFERENCES filmes(id)
        ON DELETE CASCADE, -- Confirmado por FilmeDAO.excluirFilme
    FOREIGN KEY (genero_id) REFERENCES generos(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- DADOS INICIAIS
-- -----------------------------------------------------

-- Popula a tabela de Gêneros
-- Baseado em: TelaAdminFilmes.java (GENEROS_PRE_CADASTRADOS)
INSERT INTO generos (nome) VALUES
('Ação'),
('Aventura'),
('Comédia'),
('Drama'),
('Fantasia'),
('Ficção Científica'),
('Terror'),
('Romance'),
('Documentário'),
('Musical'),
('Animação');

-- NOTA: O usuário 'admin' (senha 'admin') será criado automaticamente
-- na primeira vez que você iniciar o Servidor, conforme o código
-- em AplicacaoServidor.java e UsuarioDAO.java.