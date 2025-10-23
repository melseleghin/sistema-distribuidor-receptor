package src;

import java.util.Random;

/**
 * Programa que faz a contagem sem paralelismo/distribuição,
 * para comparar tempos com o sistema distribuído.
 */

public class ContagemSequencial {

    public static int[] gerarVetorAleatorio(int tamanho, int minIncl, int maxIncl) {
        Random rnd = new Random();
        int[] v = new int[tamanho];
        for (int i = 0; i < tamanho; i++) {
            v[i] = rnd.nextInt(maxIncl - minIncl + 1) + minIncl;
        }
        return v;
    }

    public static int contarSequencial(int[] v, int procurado) {
        int cont = 0;
        for (int n : v) if (n == procurado) cont++;
        return cont;
    }

    public static void main(String[] args) {
        int tamanho = 5_000_000; // usar mesmo tamanho que no Distribuidor para comparação
        System.out.println("[SEQ] Gerando vetor...");
        int[] v = gerarVetorAleatorio(tamanho, -100, 100);

        int procurado = v[new Random().nextInt(v.length)];
        // int procurado = 111; // para testar inexistente

        System.out.printf("[SEQ] Procurando por %d%n", procurado);

        long t0 = System.currentTimeMillis();
        int cont = contarSequencial(v, procurado);
        long t1 = System.currentTimeMillis();

        System.out.printf("[SEQ] Resultado: %d (tempo=%d ms)%n", cont, (t1 - t0));
    }
}

