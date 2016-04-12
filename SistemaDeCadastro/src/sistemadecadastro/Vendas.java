
package sistemadecadastro;

/**
 *
 * @author Pinhata
 */
public class Vendas {
    
    int clienteID;
    int produtoID;
    int quantidade;
    String date;
    
    public Vendas(int clienteID, int produtoID, int quantidade, String date){
        this.clienteID = clienteID;
        this.produtoID = produtoID;
        this.quantidade = quantidade;
        this.date = date;
    }

    public int getClienteID() {
        return clienteID;
    }

    public void setClienteID(int clienteID) {
        this.clienteID = clienteID;
    }

    public int getProdutoID() {
        return produtoID;
    }

    public void setProdutoID(int produtoID) {
        this.produtoID = produtoID;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    
    
}
