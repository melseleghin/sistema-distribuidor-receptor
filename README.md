# Sistema Distribuído de Contagem em Java

## 📋 Visão Geral

Este projeto implementa um sistema distribuído para contagem de ocorrências de números em grandes vetores, desenvolvido como trabalho prático da disciplina de Programação Paralela e Distribuída. O sistema utiliza arquitetura cliente-servidor com TCP/IP, serialização de objetos e processamento paralelo para otimizar a contagem de elementos em grandes conjuntos de dados.

## 🎯 Objetivo

Desenvolver um sistema distribuído onde um programa Distribuidor (D) gera um grande vetor de números inteiros aleatórios (tipo byte entre -100 e 100), escolhe um número aleatório do vetor e distribui partes do vetor para múltiplos programas Receptores (R) que executam a contagem em paralelo. A comunicação entre D e R ocorre via TCP/IP com serialização de objetos e conexões persistentes.

## 🏗️ Arquitetura do Sistema

O sistema é composto por dois programas principais:

### Distribuidor (D) - Cliente
- Gera um vetor de 5.000.000 de números inteiros aleatórios
- Escolhe aleatoriamente um número para ser contado
- Divide o vetor em partes e distribui para os Receptores
- Mantém conexões persistentes com múltiplos Receptores
- Agrega os resultados parciais
- Compara o tempo de execução com contagem sequencial

### Receptor (R) - Servidor
- Aguarda conexões de clientes na porta 12345
- Recebe pedidos de contagem via objetos serializados
- Processa pedidos em paralelo usando pool de threads
- Utiliza todos os processadores disponíveis da máquina
- Retorna resultados parciais ao Distribuidor
- Mantém conexão aberta até receber comunicado de encerramento

## 📦 Classes Principais

### Hierarquia de Comunicação

```
Comunicado (Serializable)
├── Pedido
├── Resposta
└── ComunicadoEncerramento
```

### Comunicado
- Classe base abstrata que implementa `Serializable`
- Não possui atributos nem métodos
- Serve como superclasse para todos os tipos de comunicação

### Pedido
- **Atributos:**
  - `int[] numeros` - Parte do vetor a ser processada
  - `int procurado` - Número a ser contado
- **Métodos:**
  - `contar()` - Percorre o vetor e retorna quantas vezes o número procurado aparece

### Resposta
- **Atributos:**
  - `Integer contagem` - Resultado da contagem parcial
- **Métodos:**
  - `getContagem()` - Retorna o valor da contagem

### ComunicadoEncerramento
- Sinal de término de comunicação
- Indica que o Distribuidor não enviará mais pedidos
- Faz com que o Receptor feche a conexão atual e aguarde novas conexões

## 🔧 Componentes Adicionais

### Distribuidor.java
Implementa o cliente distribuidor com:
- Conexões persistentes para múltiplos servidores
- Divisão automática do vetor em partes
- Threads para comunicação paralela com cada Receptor
- Agregação de resultados parciais
- Medição e comparação de tempos de execução
- Validação local dos resultados

### Reativo.java (Receptor)
Implementa o servidor receptor com:
- `ExecutorService` com thread pool fixo (baseado no número de processadores)
- Pool de threads para gerenciar múltiplas conexões simultâneas
- Processamento paralelo de pedidos
- Sistema de logging informativo
- Tratamento robusto de exceções

### ContagemSequencial.java
Programa auxiliar para comparação de desempenho:
- Executa a contagem sem paralelismo ou distribuição
- Gera vetores com mesmo tamanho do sistema distribuído
- Mede tempo de execução para análise comparativa

## 🚀 Configuração e Execução

### Pré-requisitos
- Java JDK 8 ou superior
- Múltiplos computadores na mesma rede local (recomendado 3-4)
- Conectividade TCP/IP entre as máquinas

### Descobrindo o Endereço IP

**Windows:**
```bash
ipconfig
```

**Linux/macOS:**
```bash
ifconfig
```

### Compilação

```bash
# Compilar Receptor
javac src/Receptor/*.java

# Compilar Distribuidor
javac src/Distribuidor/*.java

# Compilar ContagemSequencial
javac src/ContagemSequencial.java
```

### Execução

#### 1. Iniciar os Receptores (em cada máquina servidora)

```bash
java src.Receptor.Reativo
```

O Receptor iniciará e aguardará conexões na porta 12345.

#### 2. Configurar o Distribuidor

Edite o arquivo `Distribuidor.java` e configure os IPs dos servidores:

```java
String[] servidores = {
    "192.168.1.10:12345",
    "192.168.1.11:12345",
    "192.168.1.12:12345"
};
```

#### 3. Executar o Distribuidor

```bash
java src.Distribuidor.Distribuidor
```

#### 4. Comparar com Execução Sequencial

```bash
java src.ContagemSequencial
```

## 🧪 Testes

### Teste Local
Para testar localmente sem múltiplas máquinas:

1. Inicie múltiplas instâncias do Receptor em portas diferentes:
```bash
# Terminal 1
java src.Receptor.Reativo

# Terminal 2 (modifique a porta no código para 12346)
java src.Receptor.Reativo

# Terminal 3 (modifique a porta no código para 12347)
java src.Receptor.Reativo
```

2. Configure o Distribuidor com `localhost`:
```java
String[] servidores = {
    "localhost:12345",
    "localhost:12346",
    "localhost:12347"
};
```

### Teste de Número Inexistente
O sistema permite testar a contagem de números que não existem no vetor:
- Configure o número procurado como `111` no Distribuidor
- A contagem deve retornar `0`

### Logs Esperados

**Receptor:**
```
[R] Servidor iniciado na porta 12345
[R] Pedido recebido do cliente 172.16.21.50
[R] Processando pedido com 1250000 números
[R] Contagem concluída: 6234 ocorrências
```

**Distribuidor:**
```
=== INICIANDO DISTRIBUIDOR ===
Conectado a 172.16.21.22:12345
Número escolhido para contagem: 42
Enviando pedido para 172.16.21.22...
Resposta recebida: 6234
Contagem distribuída total = 24936 em 1523 ms
Contagem local (validação) = 24936
```

## ⚙️ Descobrindo Número de Processadores

O sistema usa automaticamente todos os processadores disponíveis:

```java
int quantidade = Runtime.getRuntime().availableProcessors();
```

## 📊 Análise de Performance

O sistema realiza comparações automáticas entre:
- **Contagem Distribuída**: Utiliza múltiplos computadores e processamento paralelo
- **Contagem Sequencial**: Execução tradicional em uma única thread

### Métricas Coletadas
- Tempo de geração do vetor
- Tempo de contagem distribuída
- Tempo de contagem sequencial
- Speedup obtido com paralelização

## 🔍 Detalhes Técnicos

### Comunicação TCP/IP
- Porta padrão: 12345
- Protocolo: TCP/IP com serialização de objetos Java
- Conexões persistentes mantidas até comunicado de encerramento
- `setTcpNoDelay(true)` para reduzir latência

### Paralelismo
- **Receptor**: Pool fixo de threads = número de processadores
- **Distribuidor**: Uma thread por Receptor conectado
- Processamento paralelo dentro de cada Receptor

### Tratamento de Exceções
- Captura e tratamento adequado de `IOException`
- Logs informativos para debugging
- Fechamento correto de recursos (streams, sockets)

### Sincronização
- Uso de `Thread.join()` para sincronizar threads no Distribuidor
- `ExecutorService` para gerenciar pool de threads nos Receptores
- `Future` e `Callable` para processamento paralelo com retorno

## 📁 Estrutura do Projeto

```
sistema-distribuidor-receptor/
├── src/
│   ├── Distribuidor/
│   │   ├── Comunicado.java
│   │   ├── Pedido.java
│   │   ├── Resposta.java
│   │   ├── ComunicadoEncerramento.java
│   │   └── Distribuidor.java
│   ├── Receptor/
│   │   ├── Comunicado.java
│   │   ├── Pedido.java
│   │   ├── Resposta.java
│   │   ├── ComunicadoEncerramento.java
│   │   └── Reativo.java
│   ├── ContagemSequencial.java
│   └── Main.java
├── .gitignore
└── README.md
```

## 🔒 Boas Práticas Implementadas

- ✅ Tratamento robusto de exceções
- ✅ Logging informativo em ambos os programas
- ✅ Uso de `Thread.join()` para sincronização
- ✅ Teste com números inexistentes no vetor
- ✅ Programa de contagem sequencial para comparação
- ✅ Medição de tempos de execução
- ✅ Fechamento apropriado de recursos (sockets, streams)
- ✅ Validação dos resultados distribuídos

## 🐛 Troubleshooting

### Problema: Conexão recusada
**Solução:** Verifique se o Receptor está executando e o firewall permite conexões na porta 12345

### Problema: OutOfMemoryError
**Solução:** Execute com mais memória:
```bash
java -Xmx8G src.Distribuidor.Distribuidor
```

### Problema: Resultados inconsistentes
**Solução:** Verifique se todos os Receptores estão respondendo corretamente e se a divisão do vetor está correta

## 📚 Referências

- Java Network Programming
- Java Concurrency in Practice
- Documentação oficial do Java sobre Serialization
- Documentação do ExecutorService

## 👥 Desenvolvimento

Este projeto foi desenvolvido como trabalho da disciplina de Programação Paralela e Distribuída, demonstrando conceitos de:
- Sistemas distribuídos
- Comunicação via sockets TCP/IP
- Serialização de objetos Java
- Programação concorrente e paralela
- Thread pools e ExecutorService
- Análise de desempenho

## 📄 Licença

Este projeto foi desenvolvido para fins educacionais.

---

**Data de Desenvolvimento:** Outubro 2025  
**Disciplina:** Programação Paralela e Distribuída em Java
