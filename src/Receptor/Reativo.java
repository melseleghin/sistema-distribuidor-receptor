package src.Receptor;

import java.io.*;
import java.net.*;
import java.util.*; // adicionada para ExecutorService, Future, Callable, Executors
import java.util.concurrent.*; // adicionada para List e ArrayList

public class Reativo
{

    // private -> only this class can access (private, public)
    // static -> only one instance of the class (no instances)
    // final -> cannot be changed (constant)
    // ExecutorService -> pool of threads for CPU-bound tasks

    // ExecutorService is a class that manages a pool of threads

    // Method used at the beginning of the program to create the pool of threads
    // newFixedThreadPool(n) creates a pool with n threads -> In this case is the number of available processors
    private static final ExecutorService cpuPool =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // newCachedThreadPool() creates a pool with a number of threads created for each connection
    // Example: 
        // Conexão 1 → Thread 1 (criada)
        // Conexão 2 → Thread 2 (criada)
        // Conexão 3 → Thread 3 (criada)
        // ...
        // Conexão 10 → Thread 10 (criada)
        // Total: 10 threads criadas

        // Conexão 5 → ESPERA Thread 1 ficar livre
        // Conexão 6 → ESPERA Thread 2 ficar livre
        // ...
        // Total: 4 threads, 6 conexões na fila
    private static final ExecutorService connectionPool = 
        Executors.newCachedThreadPool();

    public static void main (String[] args)
    {
        try
        {
            ServerSocket pedido =
                new ServerSocket (12345); // Creates ServerSocket object on port 12345

            // For each client
            while (true)
            {
                // Creates a connection
                Socket conexao = 
                    pedido.accept(); // Pedido.accept returns a Socket object. Client connection

                // Assigns a thread for a connection ( limited threads but infinite connections ) 
                connectionPool.submit(() -> {
                    try {
                        // Receber / Input
                        ObjectInputStream receptor =
                            new ObjectInputStream( // Reads OBJECTS from client
                            conexao.getInputStream()); // Returns an InputStream object ( Data entering from client ) and converts it from Binary Data to OBJECTS

                        // Enviar / Output
                        ObjectOutputStream transmissor = // Writes OBJECTS to the client
                            new ObjectOutputStream(
                            conexao.getOutputStream()); // Gets the output stream from client connection ( Data leaving to client ) and converts OBJECTS to Binary Data

                        Object objeto;
                        do
                        {
                            objeto = receptor.readObject();

                            if (objeto instanceof Pedido)
                            {
                                Pedido pedidoRecebido = (Pedido) objeto;

                                // Parallel processing
                                int contagem;
                                contagem = processarPedidoParalelo(
                                        pedidoRecebido.getNumeros(),
                                        pedidoRecebido.getProcurado()
                                    );                     
                                Resposta resposta = new Resposta(contagem);

                                transmissor.writeObject(resposta);
                                transmissor.flush();

                                System.out.println("Processado pedido - contagem: " + contagem);
                            }
                            else if (objeto instanceof ComunicadoEncerramento)
                            {
                                System.out.println("Recebido ComunicadoEncerramento - encerrando conexão");
                                break;
                            }
                        }
                        while (true);

                        transmissor.close();
                        receptor.close();
                        conexao.close();
                    }
                    catch (Exception erro)
                    {
                        System.err.println(erro.getMessage());
                    }
                });
            }
        }
        catch (Exception erro)
        {
            System.err.println(erro.getMessage());
        }
    }

    private static int processarPedidoParalelo(int[] numeros, int procurado) throws InterruptedException, ExecutionException {
        int numProcessadores = Runtime.getRuntime().availableProcessors();

        int tamanhoArray = numeros.length;
        int tamanhoFatia = tamanhoArray / numProcessadores;

        // Each processed slice from working threads
        List<Future<Integer>> resultados = new ArrayList<>();

        for (int i = 0; i < numProcessadores; i++)
        {   
            // Ex: i=0 --> 1 * 333 = 0    -> Server will process 0 to 332
            // Ex: i=1 --> 1 * 333 = 333  -> Server will process 333 to 665
            int inicio = i * tamanhoFatia; // This determines the start of the slice, index of each processor multiplied by the size of the slice

            // Problem:
                // Example array of 1000 divided by 3 processors:
                // Processor 1: 0-333
                // Processor 2: 334-666
                // Processor 3: 667-999
                // To fix this, we use the following condition: If it's the last processor, take the rest of the array, otherwise, take the next slice
            int fim = (i == numProcessadores - 1) ? tamanhoArray : (i + 1) * tamanhoFatia; 
            // It works because we divided the array by the number of processors. Multiplying again works

            resultados.add(cpuPool.submit(new ContadorTask(numeros, inicio, fim, procurado)));
        }       

        // Reunite the results from each thread
        int total = 0;
        for (Future<Integer> futuro : resultados)
            total += futuro.get();

        return total;
    }

    // Task that will actually process in each Thread
    // Extends pedido to use method contar() overridden and Implements Callable<Integer> so that 
    // ExecutorService can make the thread call a task and return its result with its correct type
    private static class ContadorTask extends Pedido implements Callable<Integer>
    {
        private final int inicio;
        private final int fim;

        public ContadorTask(int[] numeros, int inicio, int fim, int procurado)
        {
            super(numeros, procurado); 
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public int contar()
        {
            int cont = 0;
            for (int i = inicio; i < fim; i++)
                // if (numeros[i] == procurado()) FUCK YOU  JAIME, WHY PRIVATE FIELDS YOU MF, FUCK YOU JAIME >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:(  >:( 
                if (getNumeros()[i] == getProcurado())
                    cont++;
            return cont;
        }

        // This will be the method caller that the ExecutorService will call while managing the threads
        @Override
        public Integer call()
        {
            return contar(); // Usa o método contar() sobrescrito
        }
    }

    // ao final ira juntar todas e envia rpar ao cliente
}
