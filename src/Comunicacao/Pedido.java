package src.Comunicacao;

public class Pedido extends Comunicado {
    private final int[] numeros;
    private final int procurado;

    public Pedido(int[] numeros, int procurado) {
        this.numeros = numeros;
        this.procurado = procurado;
    }

    public int[] getNumeros() {
        return numeros;
    }

    public int getProcurado() {
        return procurado;
    }

    public int contar() {
        int cont = 0;
        for (int n : numeros) {
            if (n == procurado) cont++;
        }
        return cont;
    }
}


