package src.Receptor;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import src.Comunicacao.*;

public class Receptor {

    private static final ExecutorService cpuPool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final ExecutorService connectionPool =
            Executors.newCachedThreadPool();

    public static void main(String[] args) {
        ServerSocket pedido = null;
        int porta = 0;

        try {
            // Permitir escolher a porta via argumento de linha de comando
            if (args.length > 0) {
                porta = Integer.parseInt(args[0]);
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Digite a porta para o receptor (ex: 12345, 12346): ");
                porta = scanner.nextInt();
                scanner.close();
            }

            pedido = new ServerSocket(porta);
            System.out.println("=== RECEPTOR INICIADO ===");
            System.out.println("[LOG] Servidor rodando na porta: " + pedido.getLocalPort());
            System.out.println("[LOG] Processadores disponíveis: " + Runtime.getRuntime().availableProcessors());
            System.out.println("[LOG] Aguardando conexões...\n");

            int numeroConexao = 0;

            while (true) {
                Socket conexao = pedido.accept();
                final int idConexao = ++numeroConexao;

                System.out.println("[LOG] Conexão #" + idConexao + " aceita de: " +
                        conexao.getInetAddress().getHostAddress() + ":" + conexao.getPort());

                connectionPool.submit(() -> {
                    ObjectInputStream receptor = null;
                    ObjectOutputStream transmissor = null;

                    try {
                        System.out.println("[LOG] Conexão #" + idConexao + " - Inicializando streams...");

                        receptor = new ObjectInputStream(conexao.getInputStream());
                        transmissor = new ObjectOutputStream(conexao.getOutputStream());
                        transmissor.flush();

                        System.out.println("[LOG] Conexão #" + idConexao + " - Pronto para receber pedidos");

                        Object objeto;
                        int numeroPedido = 0;

                        while (true) {
                            objeto = receptor.readObject();

                            if (objeto instanceof Pedido) {
                                numeroPedido++;
                                Pedido pedidoRecebido = (Pedido) objeto;

                                System.out.println("[LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                                        " recebido (tamanho vetor: " + pedidoRecebido.getNumeros().length +
                                        ", procurado: " + pedidoRecebido.getProcurado() + ")");

                                long inicio = System.currentTimeMillis();
                                int contagem = processarPedidoParalelo(
                                        pedidoRecebido.getNumeros(),
                                        pedidoRecebido.getProcurado()
                                );
                                long fim = System.currentTimeMillis();

                                Resposta resposta = new Resposta(contagem);
                                transmissor.writeObject(resposta);
                                transmissor.flush();

                                System.out.println("[LOG] Conexão #" + idConexao + " - Pedido #" + numeroPedido +
                                        " processado: " + contagem + " ocorrências em " +
                                        (fim - inicio) + " ms");
                            }
                            else if (objeto instanceof ComunicadoEncerramento) {
                                System.out.println("[LOG] Conexão #" + idConexao +
                                        " - ComunicadoEncerramento recebido");
                                break;
                            }
                            else {
                                System.err.println("[ERRO] Conexão #" + idConexao +
                                        " - Objeto desconhecido recebido: " + objeto.getClass().getName());
                            }
                        }

                        System.out.println("[LOG] Conexão #" + idConexao + " - Encerrando...");

                    } catch (EOFException e) {
                        System.err.println("[ERRO] Conexão #" + idConexao +
                                " - Fim inesperado do stream (cliente desconectou?)");
                    } catch (SocketException e) {
                        System.err.println("[ERRO] Conexão #" + idConexao +
                                " - Erro de socket: " + e.getMessage());
                    } catch (ClassNotFoundException e) {
                        System.err.println("[ERRO] Conexão #" + idConexao +
                                " - Classe não encontrada: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("[ERRO] Conexão #" + idConexao +
                                " - Exceção não esperada: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        // Fechamento seguro dos recursos
                        try {
                            if (transmissor != null) {
                                transmissor.close();
                                System.out.println("[LOG] Conexão #" + idConexao + " - Transmissor fechado");
                            }
                        } catch (IOException e) {
                            System.err.println("[ERRO] Conexão #" + idConexao +
                                    " - Erro ao fechar transmissor: " + e.getMessage());
                        }

                        try {
                            if (receptor != null) {
                                receptor.close();
                                System.out.println("[LOG] Conexão #" + idConexao + " - Receptor fechado");
                            }
                        } catch (IOException e) {
                            System.err.println("[ERRO] Conexão #" + idConexao +
                                    " - Erro ao fechar receptor: " + e.getMessage());
                        }

                        try {
                            if (conexao != null && !conexao.isClosed()) {
                                conexao.close();
                                System.out.println("[LOG] Conexão #" + idConexao + " - Socket fechado\n");
                            }
                        } catch (IOException e) {
                            System.err.println("[ERRO] Conexão #" + idConexao +
                                    " - Erro ao fechar socket: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (NumberFormatException e) {
            System.err.println("[ERRO FATAL] Porta inválida fornecida");
        } catch (IOException e) {
            System.err.println("[ERRO FATAL] Erro ao criar ServerSocket na porta " + porta + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERRO FATAL] Exceção no main: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Encerramento dos pools de threads
            System.out.println("\n[LOG] Encerrando pools de threads...");

            connectionPool.shutdown();
            cpuPool.shutdown();

            try {
                if (!connectionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("[AVISO] Forçando encerramento do connectionPool...");
                    connectionPool.shutdownNow();
                }
                if (!cpuPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("[AVISO] Forçando encerramento do cpuPool...");
                    cpuPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                connectionPool.shutdownNow();
                cpuPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            if (pedido != null && !pedido.isClosed()) {
                try {
                    pedido.close();
                    System.out.println("[LOG] ServerSocket fechado");
                } catch (IOException e) {
                    System.err.println("[ERRO] Ao fechar ServerSocket: " + e.getMessage());
                }
            }

            System.out.println("=== RECEPTOR ENCERRADO ===");
        }
    }

    private static int processarPedidoParalelo(byte[] numeros, byte procurado)
            throws InterruptedException, ExecutionException {

        int numProcessadores = Runtime.getRuntime().availableProcessors();
        int tamanhoArray = numeros.length;
        int tamanhoFatia = tamanhoArray / numProcessadores;

        List<Future<Integer>> resultados = new ArrayList<>();

        System.out.println("    [LOG] Processamento paralelo iniciado: " +
                numProcessadores + " threads, " + tamanhoArray + " elementos");

        for (int i = 0; i < numProcessadores; i++) {
            int inicio = i * tamanhoFatia;
            int fim = (i == numProcessadores - 1) ? tamanhoArray : (i + 1) * tamanhoFatia;

            System.out.println("    [LOG] Thread " + i + " processará índices [" + inicio + ", " + fim + ")");

            resultados.add(cpuPool.submit(new ContadorTask(numeros, inicio, fim, procurado)));
        }

        int total = 0;
        for (int i = 0; i < resultados.size(); i++) {
            int parcial = resultados.get(i).get();
            total += parcial;
            System.out.println("    [LOG] Thread " + i + " retornou: " + parcial + " ocorrências");
        }

        System.out.println("    [LOG] Total combinado: " + total + " ocorrências");
        return total;
    }

    private static class ContadorTask extends Pedido implements Callable<Integer> {
        private final int inicio;
        private final int fim;

        public ContadorTask(byte[] numeros, int inicio, int fim, byte procurado) {
            super(numeros, procurado);
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public int contar() {
            int cont = 0;
            byte[] nums = getNumeros();
            byte proc = getProcurado();

            for (int i = inicio; i < fim; i++) {
                if (nums[i] == proc) {
                    cont++;
                }
            }
            return cont;
        }

        @Override
        public Integer call() {
            return contar();
        }
    }
}