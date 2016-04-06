/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sistemadecadastro;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 *
 * @author Eduardo
 */
public class MinhaConexao {
    
    public static MinhaConexao conexao = null;
    
    public static MinhaConexao getInstance(){
        try{
            if(conexao==null || conexao.sqlConnection.isClosed()){
                conexao = new MinhaConexao();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return conexao;
    }
    
    public Connection sqlConnection;
    
    private MinhaConexao()
    {
        try{
           Class.forName("com.mysql.jdbc.Driver").newInstance();
           String textoConexao = "jdbc:mysql://localhost/loja?user=root&password=321inQuesti";
           sqlConnection = DriverManager.getConnection(textoConexao);
        }catch(Exception e){
               e.printStackTrace();
        }
    }
}
    
    
    

