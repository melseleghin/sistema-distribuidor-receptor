package src.Comunicacao;

public class Pedido extends Comunicado {
    private final byte[] numeros;
    private final byte procurado;

    public Pedido(byte[] numeros, byte procurado) {
        this.numeros = numeros;
        this.procurado = procurado;
    }

    public byte[] getNumeros() {
        return numeros;
    }

    public byte getProcurado() {
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


