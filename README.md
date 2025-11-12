
-----

# VoteFlix: Um App Java Cliente-Servidor de Avaliação de Filmes

## 📖 Visão Geral

O VoteFlix é uma aplicação desktop completa que simula uma plataforma de avaliação de filmes, como o "IMDb" ou "Letterboxd". O projeto é construído inteiramente em Java e demonstra uma arquitetura **Cliente-Servidor**.

A comunicação entre o cliente (interface gráfica) e o servidor (lógica de negócios) é realizada via **Sockets TCP**, e todas as informações são persistidas em um banco de dados MySQL. A autenticação de sessão é gerenciada usando **JSON Web Tokens (JWT)**.

Este projeto foi desenvolvido como parte dos estudos em Sistemas Distribuídos.

## ✨ Funcionalidades

A aplicação é dividida em dois painéis principais:

### 👤 Painel do Usuário

* **Autenticação:** Criar conta e fazer login (com sessão gerenciada por JWT).
* **Listar Filmes:** Visualizar todos os filmes cadastrados, suas notas médias e sinopses.
* **Criar Review:** Escrever e publicar uma nova review para um filme, atribuindo uma nota.
* **Minhas Reviews:** Visualizar, editar ou excluir apenas as reviews feitas pelo próprio usuário.

### 🛠️ Painel Administrativo

* **Gerenciamento de Filmes:** Administradores podem adicionar, editar ou excluir filmes do catálogo.
* **Gerenciamento de Usuários:** Administradores podem visualizar e excluir contas de usuários (excluindo outros administradores).

## 🚀 Tecnologias Utilizadas

* **Linguagem:** Java 17+
* **Interface Gráfica:** JavaFX
* **Gerenciador de Dependências:** Maven
* **Banco de Dados:** MySQL
* **Comunicação:** Java Sockets (Arquitetura Cliente-Servidor TCP)
* **Autenticação:** JSON Web Token (JWT) para gerenciamento de sessão

## 🔧 Como Executar

Siga os passos abaixo para configurar e executar o projeto localmente.

### Pré-requisitos

* Java (JDK 17 ou superior)
* Apache Maven
* MySQL Server (rodando na porta padrão 3306)

### 1\. Clonar o Repositório

```bash
git clone https://github.com/nicolassmotta/java-client-server-app.git
cd java-client-server-app
```

### 2\. Configurar o Banco de Dados

**A. Crie o banco de dados:**
Entre no seu cliente MySQL e execute:

```sql
CREATE DATABASE voteflix_db;
```

**B. Execute o Script de Criação:**
Use o arquivo `schema.sql` (localizado dentro da pasta `ProjetoSistemasDistribuidos/`) para criar todas as tabelas e inserir os gêneros padrão.

```bash
# Estando na raiz do repositório (java-client-server-app)
mysql -u root -p voteflix_db < ProjetoSistemasDistribuidos/schema.sql
```

### 3\. Configurar as Credenciais do Banco

Para manter as senhas seguras, o projeto usa um arquivo `db.properties` que é ignorado pelo Git.

1.  Navegue até a pasta `resources` do servidor:
    ```bash
    cd ProjetoSistemasDistribuidos/vote-flix-servidor/src/main/resources/
    ```
2.  Copie o arquivo de exemplo:
    ```bash
    cp db.properties.example db.properties
    ```
3.  Abra o `db.properties` (com `nano db.properties`) e edite os campos `db.user` e `db.password` com suas credenciais do MySQL.

### 4\. Executar o Servidor

O servidor precisa ser iniciado primeiro.

1.  Abra um terminal na pasta do servidor:
    ```bash
    # (A partir da raiz do repositório)
    cd ProjetoSistemasDistribuidos/vote-flix-servidor
    ```
2.  Compile e execute o projeto com Maven:
    ```bash
    mvn clean install
    mvn javafx:run
    ```
3.  A interface do servidor irá abrir. Clique no botão **"Iniciar Servidor"**.

### 5\. Executar o Cliente

1.  Abra um **novo terminal** (mantenha o servidor rodando).
2.  Navegue até a pasta do cliente:
    ```bash
    # (A partir da raiz do repositório)
    cd ProjetoSistemasDistribuidos/vote-flix-cliente
    ```
3.  Compile e execute o projeto com Maven:
    ```bash
    mvn clean install
    mvn javafx:run
    ```
4.  A tela de conexão aparecerá. Conecte-se (o IP é `localhost` e a porta é `22222`), faça login (admin/admin) ou crie sua conta.

-----
