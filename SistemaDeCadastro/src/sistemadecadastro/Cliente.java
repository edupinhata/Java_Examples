
package sistemadecadastro;

/**
 *
 * @author Pinhata
 */
public class Cliente {
    
    private String nome;
    private String cpf;
    
    
    public Cliente(String nome, String cpf){
        this.nome = nome;
        this.cpf = cpf;
    }
    
    
    public void setNome(String nome){
        this.nome = nome;
    }
    
    public String getNome(){
        return this.nome;
    }
    
    public void setCPF(String cpf){
        this.cpf = cpf;
    }
    
    public String getCPF(){
        return this.cpf;
    }
    
}
