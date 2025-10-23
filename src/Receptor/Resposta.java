package src.Receptor;

public class Resposta extends Comunicado {

    private int contagem;

    public Resposta(int contagem) {
        this.contagem = contagem;
    }

    public int getContagem() {
        return contagem;
    }
}
