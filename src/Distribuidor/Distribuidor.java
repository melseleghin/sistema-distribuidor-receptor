package src.Distribuidor;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import src.Comunicacao.*;

public class Distribuidor {

    static class ConexaoR {
        String host;
        int porta;
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;
        private final Lock mutex = new ReentrantLock();

        public ConexaoR(String host, int porta) throws IOException {
            this.host = host;
            this.porta = porta;
            this.socket = new Socket(host, porta);
            this.socket.setTcpNoDelay(true);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[LOG] Conectado a " + host + ":" + porta);
        }

        public Resposta enviarPedido(Pedido pedido) throws IOException, ClassNotFoundException {
            mutex.lock();
            try {
                System.out.println("[LOG] Adquiriu mutex para " + this);
                out.writeObject(pedido);
                out.flush();
                System.out.println("[LOG] Pedido enviado para " + this);

                Object obj = in.readObject();
                if (obj instanceof Resposta) {
                    System.out.println("[LOG] Resposta recebida de " + this);
                    return (Resposta) obj;
                }
                throw new IOException("Resposta inválida recebida de " + this);
            } finally {
                mutex.unlock();
                System.out.println("[LOG] Mutex liberado para " + this);
            }
        }

        public void enviarEncerramento() throws IOException {
            mutex.lock();
            try {
                System.out.println("[LOG] Enviando encerramento para " + this);
                out.writeObject(new ComunicadoEncerramento());
                out.flush();
            } finally {
                mutex.unlock();
            }
        }

        public void fechar() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("[LOG] Conexão fechada com " + this);
                }
            } catch (IOException e) {
                System.err.println("[ERRO] Ao fechar conexão com " + this + ": " + e.getMessage());
            }
        }

        @Override
        public String toString() {
            return host + ":" + porta;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<ConexaoR> conexoes = new ArrayList<>();

        try {
            System.out.println("=== INICIANDO DISTRIBUIDOR ===");

            // IPs e portas hard coded
            String[] servidores = {
                    "localhost:12345",
                    "localhost:12346"
            };

            // Criação das conexões persistentes
            System.out.println("\n[LOG] Estabelecendo conexões com os receptores...");
            for (String s : servidores) {
                try {
                    String[] partes = s.split(":");
                    ConexaoR conexao = new ConexaoR(partes[0], Integer.parseInt(partes[1]));
                    conexoes.add(conexao);
                } catch (IOException e) {
                    System.err.println("[ERRO] Não foi possível conectar a " + s + ": " + e.getMessage());
                    System.err.println("[AVISO] Verifique se o servidor está rodando nesta porta!");
                }
            }

            if (conexoes.isEmpty()) {
                System.err.println("[ERRO FATAL] Nenhuma conexão estabelecida. Encerrando.");
                return;
            }

            System.out.println("[LOG] Total de conexões estabelecidas: " + conexoes.size() + "/" + servidores.length);

            // Solicitar tamanho do vetor
            System.out.print("\nDigite o tamanho do vetor (ex: 10, 1000, 5000000): ");
            int TAM = scanner.nextInt();

            if (TAM <= 0) {
                System.err.println("[ERRO] Tamanho inválido. Encerrando.");
                return;
            }

            System.out.println("[LOG] Gerando vetor de " + TAM + " elementos...");

            // Geração do vetor principal
            SecureRandom rnd = new SecureRandom();
            byte[] vetor = new byte[TAM];
            for (int i = 0; i < TAM; i++) {
                vetor[i] = (byte)(-100 + rnd.nextInt(201));
            }
            System.out.println("[LOG] Vetor gerado com sucesso!");

            // Perguntar se deseja exibir o vetor
            System.out.print("\nDeseja exibir o vetor na tela? (s/n): ");
            String resposta = scanner.next();
            if (resposta.equalsIgnoreCase("s")) {
                System.out.println("\n[VETOR]");
                for (int i = 0; i < vetor.length; i++) {
                    System.out.print(vetor[i]);
                    if (i < vetor.length - 1) System.out.print(", ");
                    if ((i + 1) % 20 == 0) System.out.println(); // quebra de linha a cada 20 elementos
                }
                System.out.println("\n");
            }

            // Escolha do número a procurar
            System.out.print("\nDeseja procurar um número que NÃO existe no vetor (111)? (s/n): ");
            resposta = scanner.next();

            byte procurado;
            if (resposta.equalsIgnoreCase("s")) {
                procurado = 111; // Número fora do intervalo [-100, 100]
                System.out.println("[LOG] Número escolhido para contagem: " + procurado + " (não existe no vetor)");
            } else {
                procurado = vetor[rnd.nextInt(TAM)];
                System.out.println("[LOG] Número escolhido para contagem: " + procurado + " (existe no vetor)");
            }

            // Contagem distribuída principal
            System.out.println("\n[LOG] Iniciando contagem distribuída...");
            long inicio = System.currentTimeMillis();
            int total = contarDistribuido(conexoes, vetor, procurado);
            long fim = System.currentTimeMillis();
            System.out.println("\n[RESULTADO] Contagem distribuída total = " + total + " em " + (fim - inicio) + " ms");

            // Contagem local (sequencial para validação)
            System.out.println("[LOG] Realizando contagem local para validação...");
            long inicioLocal = System.currentTimeMillis();
            int local = 0;
            for (byte n : vetor) {
                if (n == procurado) local++;
            }
            long fimLocal = System.currentTimeMillis();
            System.out.println("[RESULTADO] Contagem local (checagem) = " + local + " em " + (fimLocal - inicioLocal) + " ms");
            System.out.println("[RESULTADO] Contagem local (checagem) = " + local);

            if (total == local) {
                System.out.println("[SUCESSO] Contagens coincidem! Sistema funcionando corretamente.");
            } else {
                System.err.println("[ERRO] Contagens diferentes! Distribuída=" + total + ", Local=" + local);
            }

        } catch (InputMismatchException e) {
            System.err.println("[ERRO] Entrada inválida. Por favor, digite um número inteiro.");
        } catch (Exception e) {
            System.err.println("[ERRO] Exceção capturada no main: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Envio do ComunicadoEncerramento e fechamento de conexões
            System.out.println("\n[LOG] Encerrando conexões...");
            for (ConexaoR c : conexoes) {
                try {
                    c.enviarEncerramento();
                    c.fechar();
                } catch (IOException e) {
                    System.err.println("[ERRO] Ao enviar encerramento para " + c + ": " + e.getMessage());
                }
            }

            scanner.close();
            System.out.println("\n=== FIM DO DISTRIBUIDOR ===");
        }
    }

    // Realiza a contagem distribuída enviando pedidos em paralelo
    private static int contarDistribuido(List<ConexaoR> conexoes, byte[] vetor, byte procurado) throws Exception {
        List<Thread> threads = new ArrayList<>();
        List<Resposta> respostas = Collections.synchronizedList(new ArrayList<>());
        List<Exception> excecoes = Collections.synchronizedList(new ArrayList<>());

        int tamanhoParte = vetor.length / conexoes.size();
        int resto = vetor.length % conexoes.size();

        System.out.println("[LOG] Dividindo vetor em " + conexoes.size() + " partes...");

        for (int i = 0; i < conexoes.size(); i++) {
            final int inicio = i * tamanhoParte;
            final int fim = inicio + tamanhoParte + (i == conexoes.size() - 1 ? resto : 0);
            final byte[] subVetor = Arrays.copyOfRange(vetor, inicio, fim);

            final int indice = i;
            Thread thread = new Thread(() -> {
                try {
                    ConexaoR c = conexoes.get(indice);
                    System.out.println("[LOG] Thread-" + indice + " enviando para " + c +
                            " (tam=" + subVetor.length + ", alvo=" + procurado + ")");

                    Pedido pedido = new Pedido(subVetor, procurado);
                    Resposta r = c.enviarPedido(pedido);

                    respostas.add(r);
                    System.out.println("[LOG] Thread-" + indice + " recebeu resposta de " + c +
                            ": " + r.getContagem() + " ocorrências");
                } catch (Exception e) {
                    System.err.println("[ERRO] Thread-" + indice + " falhou ao comunicar com " +
                            conexoes.get(indice) + ": " + e.getMessage());
                    excecoes.add(e);
                }
            }, "Thread-Receptor-" + i);

            threads.add(thread);
            thread.start();
            System.out.println("[LOG] Thread-" + indice + " iniciada");
        }

        // Sincronização das threads usando join()
        System.out.println("[LOG] Aguardando conclusão de todas as threads...");
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
                System.out.println("[LOG] Thread-" + i + " finalizada");
            } catch (InterruptedException e) {
                System.err.println("[ERRO] Thread-" + i + " interrompida: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // Verificar se houve exceções
        if (!excecoes.isEmpty()) {
            System.err.println("[AVISO] " + excecoes.size() + " thread(s) falharam durante a execução");
            throw new Exception("Falhas na comunicação com receptores: " + excecoes.size() + " erros");
        }

        // Soma dos resultados
        System.out.println("[LOG] Somando resultados parciais...");
        return respostas.stream().mapToInt(r -> r.getContagem()).sum();
    }
}