-- Garante que você está usando o banco de dados correto.
-- ATENÇÃO: ISTO APAGARÁ TODOS OS DADOS EXISTENTES!
DROP DATABASE IF EXISTS voteflix_db;
CREATE DATABASE voteflix_db;
USE voteflix_db;

-- Tabela de Usuários
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(25) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL
);

-- Tabela de Filmes
CREATE TABLE filmes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    diretor VARCHAR(100) NOT NULL,
    ano INT NOT NULL,
    sinopse TEXT,
    nota_media_acumulada DECIMAL(3, 2) DEFAULT 0.00,
    total_avaliacoes INT DEFAULT 0,

    -- Constraint para evitar filmes duplicados
    UNIQUE KEY uq_filme_unico (titulo, diretor, ano)
);

-- Tabela de Gêneros
CREATE TABLE generos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

-- Tabela de Reviews (Avaliações)
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filme_id INT NOT NULL,
    usuario_id INT NOT NULL,
    titulo VARCHAR(50) NOT NULL,
    comentario VARCHAR(255) NOT NULL,
    nota INT NOT NULL,
    data TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    editado VARCHAR(5) DEFAULT 'false', -- Coluna nova para controle de edição

    -- Constraint para garantir que um usuário só avalie um filme uma vez
    UNIQUE KEY uq_usuario_filme (usuario_id, filme_id),

    -- Chaves estrangeiras
    FOREIGN KEY (filme_id) REFERENCES filmes(id)
        ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        ON DELETE CASCADE
);

-- Tabela de Junção Filmes <-> Gêneros
CREATE TABLE filmes_generos (
    filme_id INT NOT NULL,
    genero_id INT NOT NULL,

    PRIMARY KEY (filme_id, genero_id),

    FOREIGN KEY (filme_id) REFERENCES filmes(id)
        ON DELETE CASCADE,
    FOREIGN KEY (genero_id) REFERENCES generos(id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------
-- DADOS INICIAIS
-- -----------------------------------------------------

-- 1. Popula a tabela de Gêneros
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

-- 2. Popula a tabela de Filmes (Dados solicitados)
INSERT INTO filmes (titulo, ano, diretor, sinopse) VALUES
('The Godfather', 1972, 'Francis Ford Coppola', 'A saga da família Corleone mostra a ascensão e o poder de uma máfia italiana nos EUA.'),
('The Shawshank Redemption', 1994, 'Frank Darabont', 'Um homem condenado injustamente forma uma amizade duradoura e busca esperança dentro da prisão de Shawshank.'),
('Pulp Fiction', 1994, 'Quentin Tarantino', 'Histórias interligadas de criminosos em Los Angeles repletas de humor negro e diálogos marcantes.'),
('The Dark Knight', 2008, 'Christopher Nolan', 'Batman enfrenta o Coringa em uma luta intensa entre justiça e caos em Gotham City.'),
('Forrest Gump', 1994, 'Robert Zemeckis', 'A vida extraordinária de um homem simples que testemunha grandes eventos da história americana.'),
('Fight Club', 1999, 'David Fincher', 'Um homem insatisfeito cria um clube de luta clandestino que se transforma em uma revolução contra o sistema.'),
('Inception', 2010, 'Christopher Nolan', 'Um ladrão que invade sonhos tenta realizar o golpe perfeito: plantar uma ideia na mente de alguém.'),
('The Matrix', 1999, 'Lana Wachowski', 'Um hacker descobre que o mundo é uma simulação e se une à resistência para libertar a humanidade.'),
('Goodfellas', 1990, 'Martin Scorsese', 'A ascensão e queda de um mafioso ao longo de décadas no submundo do crime organizado.'),
('The Lord of the Rings: The Return of the King', 2003, 'Peter Jackson', 'A batalha final pela Terra-média decide o destino do Um Anel e de todos os seus povos.'),
('The Empire Strikes Back', 1980, 'Irvin Kershner', 'O Império persegue os rebeldes enquanto Luke Skywalker treina para se tornar um verdadeiro Jedi.'),
('The Silence of the Lambs', 1991, 'Jonathan Demme', 'Uma jovem agente do FBI busca a ajuda de um assassino canibal para capturar outro serial killer.'),
('Saving Private Ryan', 1998, 'Steven Spielberg', 'Durante a Segunda Guerra Mundial, soldados arriscam tudo para resgatar um único homem atrás das linhas inimigas.'),
('Schindler’s List', 1993, 'Steven Spielberg', 'A história real de um empresário que salvou centenas de judeus durante o Holocausto.'),
('The Green Mile', 1999, 'Frank Darabont', 'Um guarda de prisão descobre que um prisioneiro condenado à morte possui um dom sobrenatural.'),
('Se7en', 1995, 'David Fincher', 'Dois detetives investigam uma série de assassinatos inspirados nos sete pecados capitais.'),
('The Departed', 2006, 'Martin Scorsese', 'Um policial infiltrado e um espião da máfia tentam descobrir a identidade um do outro em Boston.'),
('Gladiator', 2000, 'Ridley Scott', 'Um general romano busca vingança contra o imperador que destruiu sua vida e sua família.'),
('Interstellar', 2014, 'Christopher Nolan', 'Exploradores viajam através de um buraco de minhoca em busca de um novo lar para a humanidade.'),
('Braveheart', 1995, 'Mel Gibson', 'William Wallace lidera os escoceses em uma luta épica pela liberdade contra a opressão inglesa.');

-- 3. Associa os Filmes aos Gêneros (Mapeamento manual baseado nos nomes)
-- IDs assumidos sequencialmente a partir de 1 para filmes e gêneros
-- Gêneros IDs: 1=Ação, 2=Aventura, 3=Comédia, 4=Drama, 5=Fantasia, 6=Ficção, 7=Terror, 8=Romance...

INSERT INTO filmes_generos (filme_id, genero_id) VALUES
(1, 4), -- The Godfather -> Drama
(2, 4), -- Shawshank -> Drama
(3, 1), -- Pulp Fiction -> Ação (ou Crime, mas vou usar Ação conforme lista)
(4, 1), -- Dark Knight -> Ação
(5, 4), -- Forrest Gump -> Drama
(6, 4), -- Fight Club -> Drama
(7, 1), -- Inception -> Ação
(8, 1), -- Matrix -> Ação
(9, 4), -- Goodfellas -> Drama
(10, 1), -- LOTR -> Ação (ou Fantasia, pus Ação conforme lista original)
(11, 1), -- Empire Strikes Back -> Ação
(12, 7), -- Silence of the Lambs -> Terror
(13, 1), -- Saving Private Ryan -> Ação
(14, 4), -- Schindler's List -> Drama
(15, 4), -- Green Mile -> Drama
(16, 7), -- Se7en -> Terror
(17, 1), -- The Departed -> Ação
(18, 1), -- Gladiator -> Ação
(19, 6), -- Interstellar -> Ficção Científica
(20, 1); -- Braveheart -> Ação

-- O usuário 'admin' será criado automaticamente pelo servidor Java ao iniciar,
-- mas se quiser garantir, descomente a linha abaixo:
-- INSERT INTO usuarios (login, senha) VALUES ('admin', 'admin');