package GUI;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;

public class ControlGUI {

	private JFrame frmRhotMasterNode;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ControlGUI window = new ControlGUI();
					window.frmRhotMasterNode.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ControlGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmRhotMasterNode = new JFrame();
		frmRhotMasterNode.setTitle("R.H.O.T Master Node Control Panel");
		frmRhotMasterNode.setBounds(100, 100, 450, 300);
		frmRhotMasterNode.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel lblSlaveComands = new JLabel("Slave Nodes");
		
		JButton btnSendStartComand = new JButton("Send Start Comand");
		
		JLabel lblMasterNod = new JLabel("Master Node");
		
		JButton btnC = new JButton("Calibrate");
		
		textField = new JTextField();
		textField.setColumns(10);
		
		textField_1 = new JTextField();
		textField_1.setColumns(10);
		
		textField_2 = new JTextField();
		textField_2.setColumns(10);
		
		JLabel lblOrigin = new JLabel("Origin");
		
		JLabel lblX = new JLabel("X");
		
		JLabel lblY = new JLabel("Y");
		
		JLabel lblZ = new JLabel("Z");
		GroupLayout groupLayout = new GroupLayout(frmRhotMasterNode.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnC)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblOrigin)
							.addGap(18)
							.addComponent(lblX)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textField_2, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblY)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textField_1, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblZ)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textField, GroupLayout.PREFERRED_SIZE, 59, GroupLayout.PREFERRED_SIZE)
							.addGap(11))
						.addComponent(lblSlaveComands, GroupLayout.PREFERRED_SIZE, 118, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnSendStartComand)
						.addComponent(lblMasterNod))
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(lblSlaveComands)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(btnSendStartComand)
					.addGap(41)
					.addComponent(lblMasterNod)
					.addGap(18)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(btnC)
						.addComponent(lblOrigin)
						.addComponent(textField_2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblX)
						.addComponent(lblY)
						.addComponent(lblZ))
					.addContainerGap(134, Short.MAX_VALUE))
		);
		frmRhotMasterNode.getContentPane().setLayout(groupLayout);
	}
}
