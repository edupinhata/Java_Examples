/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sistemadecadastro;

import interfaceGrafica.MeuMenu;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 *
 * @author Eduardo
 */
public class SistemaDeCadastro {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        /* SwingUtilities.invokeLater(new Runnable(){
        @Override
        public void run(){
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(new Menu());
            frame.pack();
            frame.setVisible(true);
        }
    });*/
        
        MeuMenu mm = new MeuMenu();
        mm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mm.setVisible(true);
    }
    
}
