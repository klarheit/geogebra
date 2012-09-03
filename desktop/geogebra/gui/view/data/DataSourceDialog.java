package geogebra.gui.view.data;

import geogebra.common.main.App;
import geogebra.gui.GuiManagerD;
import geogebra.gui.util.LayoutUtil;
import geogebra.main.AppD;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Dialog to manage data soures for the DataAnalysisView
 * 
 * @author G. Sturr
 * 
 */
public class DataSourceDialog extends JDialog implements ActionListener,
		WindowFocusListener {

	private static final long serialVersionUID = 1L;

	private AppD app;
	private int mode;

	private DataSourcePanel dataSourcePanel;
	private JButton btnCancel, btnOK;
	private JLabel lblTitle;

	/*******************************************
	 * Constructs the dialog
	 * 
	 * @param app
	 * @param mode
	 */
	public DataSourceDialog(AppD app, int mode) {

		// non-modal dialog
		super(app.getFrame(), app.getMenu(""), false);

		this.app = app;
		this.mode = mode;
		addWindowFocusListener(this);
		createGUI();

		this.setResizable(true);
		pack();
		setLocation();

	}

	private void createGUI() {

		dataSourcePanel = new DataSourcePanel(app, this, mode);

		lblTitle = new JLabel();
		lblTitle.setIconTextGap(10);

		btnOK = new JButton();
		btnOK.addActionListener(this);

		btnCancel = new JButton();
		btnCancel.addActionListener(this);

		JPanel titlePanel = LayoutUtil.flowPanel(lblTitle);
		// titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(titlePanel, BorderLayout.NORTH);
		mainPanel.add(dataSourcePanel, BorderLayout.CENTER);
		mainPanel.add(LayoutUtil.flowPanelRight(5, 0, 0, btnCancel, btnOK),
				BorderLayout.SOUTH);

		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainPanel, BorderLayout.CENTER);
		setLabels();

	}

	@Override
	public void setVisible(boolean isVisible) {
		super.setVisible(isVisible);
	}

	public void windowGainedFocus(WindowEvent e) {
		// ignored
	}

	public void windowLostFocus(WindowEvent e) {
		// this.setVisible(false);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == btnOK) {
			dataSourcePanel.applySettings();
			app.getGuiManager().setShowView(true, App.VIEW_DATA_ANALYSIS);
			app.setMoveMode();
			setVisible(false);

		} else if (source == btnCancel) {
			app.setMoveMode();
			setVisible(false);
		}

	}

	public void updateFonts(Font font) {
		setFont(font);
		dataSourcePanel.updateFonts(font);
		GuiManagerD.setFontRecursive(this, font);
	}

	public void setLabels() {

		setTitle(app.getMenu(app.getMenu("DataSource")));

		lblTitle.setText(app.getToolName(mode));
		lblTitle.setIcon(app.getModeIcon(mode));

		btnCancel.setText(app.getMenu("Cancel"));
		btnOK.setText(app.getMenu("Analyze"));
		dataSourcePanel.setLabels();
	}

	public void updateDialog(int mode, boolean doAutoLoadSelectedGeos) {
		this.mode = mode;
		dataSourcePanel.updatePanel(mode, doAutoLoadSelectedGeos);
		setLabels();
		// setLocation();
		pack();

	}

	private void setLocation() {
		if (app.getGuiManager().showView(App.VIEW_DATA_ANALYSIS)) {
			setLocationRelativeTo(((DataAnalysisViewD) app.getGuiManager()
					.getDataAnalysisView()).getDataAnalysisViewComponent());
		} else {
			setLocationRelativeTo(app.getMainComponent());
		}

	}
}
