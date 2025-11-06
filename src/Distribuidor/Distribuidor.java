package src.Distribuidor;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import src.Comunicacao.*;

public class Distribuidor {

    static class ConexaoR {
        String host;
        int porta;
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;

        public ConexaoR(String host, int porta) throws IOException {
            this.host = host;
            this.porta = porta;
            this.socket = new Socket(host, porta);
            this.socket.setTcpNoDelay(true);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Conectado a " + host + ":" + porta);
        }

        public void fechar() {
            try {
                socket.close();
                System.out.println("Conexão fechada com " + host + ":" + porta);
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexão com " + host + ":" + porta + " - " + e.getMessage());
            }
        }

        @Override
        public String toString() {
            return host + ":" + porta;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("=== INICIANDO DISTRIBUIDOR ===");

            // IPs e portas hard coded
            String[] servidores = {
                "192.168.246.255:12345",
                "192.168.246.255:12346"
            };

            // Criação das conexões persistentes
            List<ConexaoR> conexoes = new ArrayList<>();
            for (String s : servidores) {
                String[] partes = s.split(":");
                conexoes.add(new ConexaoR(partes[0], Integer.parseInt(partes[1])));
            }

            // Geração do vetor principal
            final int TAM = 5_000_000;
            SecureRandom rnd = new SecureRandom();
            int[] vetor = new int[TAM];
            for (int i = 0; i < TAM; i++)
                vetor[i] = -100 + rnd.nextInt(201);

            // Número aleatório escolhido do vetor
            int procurado = vetor[rnd.nextInt(TAM)];
            System.out.println("Número escolhido para contagem: " + procurado);

            // Contagem distribuída principal
            long inicio = System.currentTimeMillis();
            int total = contarDistribuido(conexoes, vetor, procurado);
            long fim = System.currentTimeMillis();
            System.out.println("Contagem distribuída total = " + total + " em " + (fim - inicio) + " ms");

            // Contagem local (sequencial para validação)
            int local = 0;
            for (int n : vetor)
                if (n == procurado) local++;
            System.out.println("Contagem local (checagem) = " + local);

            // Envio do ComunicadoEncerramento
            for (ConexaoR c : conexoes) {
                try {
                    System.out.println("Enviando ComunicadoEncerramento para " + c);
                    c.out.writeObject(new ComunicadoEncerramento());
                    c.out.flush();
                    c.fechar();
                } catch (IOException e) {
                    System.out.println("Erro ao enviar encerramento para " + c + " - " + e.getMessage());
                }
            }

            System.out.println("=== FIM DO DISTRIBUIDOR ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Realiza a contagem distribuída enviando pedidos em paralelo
    private static int contarDistribuido(List<ConexaoR> conexoes, int[] vetor, int procurado) throws Exception {
        List<Thread> threads = new ArrayList<>();
        List<Resposta> respostas = Collections.synchronizedList(new ArrayList<>());

        int tamanhoParte = vetor.length / conexoes.size();
        int resto = vetor.length % conexoes.size();

        for (int i = 0; i < conexoes.size(); i++) {
            final int inicio = i * tamanhoParte;
            final int fim = inicio + tamanhoParte + (i == conexoes.size() - 1 ? resto : 0);
            final int[] subVetor = Arrays.copyOfRange(vetor, inicio, fim);

            final int indice = i;
            Thread thread = new Thread(() -> {
                try {
                    ConexaoR c = conexoes.get(indice);
                    System.out.println("Enviando Pedido para " + c + " (tam=" + subVetor.length + ", alvo=" + procurado + ")");
                    c.out.writeObject(new Pedido(subVetor, procurado));
                    c.out.flush();

                    Object obj = c.in.readObject();
                    if (obj instanceof Resposta) {
                        Resposta r = (Resposta) obj;
                        respostas.add(r);
                        System.out.println("Resposta recebida de " + c + ": " + r.getContagem());
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao comunicar com servidor " + conexoes.get(indice) + " - " + e.getMessage());
                }
            });

            threads.add(thread);
            thread.start();
        }

        // Sincronização das threads
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrompida: " + e.getMessage());
            }
        }

        // Soma dos resultados
        return respostas.stream().mapToInt(r -> r.getContagem()).sum();
    }
}