# Projeto VoteFlix (Sistemas Distribuídos)

## Descrição do Projeto

Este projeto é uma aplicação de sistema distribuído modelo cliente-servidor para avaliação de filmes, similar ao Netflix, mas focado em votação (VoteFlix).

A arquitetura é composta por:

  * **vote-flix-servidor:** Um servidor Java que gerencia a lógica de negócios, autenticação e persistência de dados com um banco MySQL. Ele utiliza uma interface gráfica JavaFX para logs de atividade e visualização de usuários conectados.
  * **vote-flix-cliente:** Um cliente JavaFX que permite aos usuários se conectarem, cadastrarem-se, fazerem login, ver filmes, e criar/editar/excluir suas próprias avaliações.

A comunicação entre cliente e servidor é feita via Sockets TCP, com mensagens serializadas em formato JSON.

## Tecnologias Utilizadas

  * **Linguagem:** Java 11
  * **Interface Gráfica:** JavaFX 17
  * **Gerenciador de Dependências:** Maven
  * **Banco de Dados:** MySQL (via `mysql-connector-j`)
  * **Comunicação:** Sockets TCP (java.net.Socket)
  * **Serialização:** Gson
  * **Autenticação:** JSON Web Tokens (JJWT)

## Pré-requisitos

Para executar este projeto, é necessário ter o seguinte software instalado:

1.  **Java Development Kit (JDK):** Versão 11 ou superior.
2.  **Apache Maven:** Para gerenciamento de dependências e execução.
3.  **Servidor MySQL:** Um servidor MySQL (como o MySQL Community Server ou XAMPP) em execução.

-----

## 1\. Configuração (Setup)

Antes de executar o projeto, o ambiente do servidor precisa ser configurado.

### 1.1. Banco de Dados MySQL

É necessário criar o banco de dados que o servidor utilizará.

1.  Acesse seu cliente MySQL (HeidiSQL, DBeaver, ou linha de comando) e execute:
    ```sql
    CREATE DATABASE voteflix_db;
    ```
2.  **IMPORTANTE:** O projeto espera que as tabelas (`usuarios`, `filmes`, `reviews`, `generos`, `filmes_generos`) já existam. O arquivo `.sql` com a estrutura não foi incluído nos arquivos, então ele deve ser executado manualmente para criar a estrutura de tabelas necessária.
3.  Verifique a conexão em `vote-flix-servidor/src/main/java/br/com/voteflix/servidor/config/ConexaoBancoDados.java`. Por padrão, ele está configurado para:
      * **URL:** `jdbc:mysql://localhost:3306/voteflix_db`
      * **Usuário:** `root`
      * **Senha:** `1234`
      * **Ajuste** o usuário e senha neste arquivo para que correspondam à sua instalação local do MySQL.

### 1.2. Chave de Segurança (JWT)

O servidor usa uma chave secreta para assinar os tokens de autenticação. A chave padrão está definida em: `vote-flix-servidor/src/main/java/br/com/voteflix/servidor/security/UtilitarioJwt.java`.

Para testes, a chave padrão (`bWV1LXNlZ3JlZG8tbXVpdG8tZm9ydGUtcGFyYS12b3RlZmxpeC0yMDI1`) pode ser mantida.

-----

## 2\. Orientações de Uso (Execução)

O projeto é gerenciado pelo Maven, que cuidará de baixar todas as bibliotecas necessárias e executar as aplicações.

### 2.1. Executando o Servidor

1.  Abra um terminal ou prompt de comando.
2.  Navegue até a pasta do servidor:
    ```bash
    cd /caminho/para/o/projeto/ProjetoSistemasDistribuidos/vote-flix-servidor
    ```
3.  Execute o Maven para baixar as dependências (bibliotecas) e compilar:
    ```bash
    mvn clean install
    ```
4.  Inicie a aplicação do servidor (que possui uma GUI de controle):
    ```bash
    mvn javafx:run
    ```
5.  Uma janela ("Painel de Controle - Servidor VoteFlix") irá aparecer. O servidor **não** inicia automaticamente.
6.  Clique no botão **"Iniciar Servidor"**. O log na interface gráfica deve confirmar que o servidor está ouvindo na porta (padrão `22222`).

### 2.2. Executando o Cliente

1.  Abra um **novo** terminal (mantenha o terminal do servidor em execução).
2.  Navegue até a pasta do cliente:
    ```bash
    cd /caminho/para/o/projeto/ProjetoSistemasDistribuidos/vote-flix-cliente
    ```
3.  Execute o Maven para baixar as dependências e compilar:
    ```bash
    mvn clean install
    ```
4.  Inicie a aplicação do cliente:
    ```bash
    mvn javafx:run
    ```
5.  A tela de conexão do VoteFlix irá aparecer.

### 2.3. Utilizando a Aplicação

1.  **Conexão:** Na tela de conexão do cliente, mantenha os valores padrão (`localhost` e porta `22222`) e clique em **"Conectar"**.
2.  **Tela Inicial:** Clique em "Login" ou "Cadastrar".
3.  **Login:** Você pode se cadastrar com um novo usuário ou usar o usuário **admin** padrão (criado automaticamente pelo servidor na primeira inicialização):
      * **Login:** `admin`
      * **Senha:** `admin`
