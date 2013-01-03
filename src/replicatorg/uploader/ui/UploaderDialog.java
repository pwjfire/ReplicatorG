package replicatorg.uploader.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.uploader.AbstractFirmwareUploader;
import replicatorg.uploader.AvrdudeUploader;
import replicatorg.uploader.FirmwareUploader;
import replicatorg.uploader.FirmwareVersion;

public class UploaderDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -401692780113162450L;
	JButton nextButton;
	JPanel centralPanel;
	FirmwareUploader uploader;
	
	enum State {
		SELECTING_BOARD,
		SELECTING_FIRMWARE,
		SELECTING_PORT,
		UPLOADING
	};
	
	State state;

	public UploaderDialog(Frame parent, FirmwareUploader uploader) {
		super(parent,"上传固件",true);
		this.uploader = uploader;
		centralPanel = new JPanel(new BorderLayout());
		Container c = getContentPane();
		c.setLayout(new MigLayout());
		Dimension panelSize = new Dimension(500,180);
		centralPanel.setMinimumSize(panelSize);
		centralPanel.setMaximumSize(panelSize);
		c.add(centralPanel,"wrap,spanx");
		JButton cancelButton = new JButton("取消");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doCancel();
			}
		});
		c.add(cancelButton,"tag cancel");
		nextButton = new JButton("下一步 >");
		nextButton.setEnabled(false);
		nextButton.addActionListener(this);
		c.add(nextButton,"tag ok");
		doBoardSelection();
		pack();
		setLocationRelativeTo(parent);
	}
	public void showPanel(JComponent panel) {
		centralPanel.removeAll();
		centralPanel.setLayout(new BorderLayout());
		centralPanel.add(panel,BorderLayout.CENTER);
		panel.setVisible(true);
		pack();
	}


	public void actionPerformed(ActionEvent arg0) {
		if (state == State.SELECTING_BOARD) {
			doFirmwareSelection();
		} else if (state == State.SELECTING_FIRMWARE) {
			doPortSelection();
		} else if (state == State.SELECTING_PORT) {
			doUpload();
		}
	}

	public void doCancel() {
		dispose();
	}

	Node selectedBoard = null;

	void doBoardSelection() {
		state = State.SELECTING_BOARD;
		nextButton.setEnabled(false);
		BoardSelectionPanel boardPanel = new BoardSelectionPanel(FirmwareUploader.getFirmwareDoc(),
				new BoardSelectionPanel.BoardSelectionListener() {
					public void boardSelected(Node board) {
						selectedBoard = board;
						nextButton.setEnabled(true);
					}

					@Override
					public void boardConfirmed() {
						// equivalent to clicking "next"
						actionPerformed(null);
					}
		});
		showPanel(boardPanel);
	}

	FirmwareVersion selectedVersion = null;

	void doFirmwareSelection() {
		state = State.SELECTING_FIRMWARE;
		nextButton.setEnabled(false);
		FirmwareSelectionPanel firmwarePanel = new FirmwareSelectionPanel(selectedBoard,
				new FirmwareSelectionPanel.FirmwareSelectionListener() {
					public void firmwareSelected(FirmwareVersion firmware) {
						selectedVersion = firmware;
						nextButton.setEnabled(true);
					}

					public void firmwareConfirmed() {
						// equivalent to clicking "next"
						actionPerformed(null);
					}
		});
		showPanel(firmwarePanel);
	}

	String portName = null;

	void doPortSelection() {
		state = State.SELECTING_PORT;
		nextButton.setEnabled(false);
		PortSelectionPanel portPanel = new PortSelectionPanel(
				new PortSelectionPanel.PortSelectionListener() {
					public void portSelected(String port) {
						portName = port;
						nextButton.setEnabled(true);
					}

					@Override
					public void portConfirmed() {
						// equivalent to clicking "next"
						actionPerformed(null);
					}
		});
		showPanel(portPanel);
	}

	AbstractFirmwareUploader createUploader() {
		MachineInterface machine = Base.getMachineLoader().getMachineInterface();
		
		if (machine != null) { machine.dispose(); }
		NodeList nl = selectedBoard.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if ("programmer".equalsIgnoreCase(n.getNodeName())) {
				return AbstractFirmwareUploader.makeUploader(n);
			}
		}
		return null;
	}
	
	void doUpload() {
		final AbstractFirmwareUploader uploader = createUploader();
		state = State.UPLOADING;
		nextButton.setEnabled(true);
		nextButton.setText("上传");
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.add(new JLabel("<html>"+uploader.getUploadInstructions()+"</html>"),"wrap");

		final String eepromPath = selectedVersion.getEepromPath();
		if (eepromPath != null) {				
			final JCheckBox cbox = new JCheckBox("overwrite current EEPROM settings", true);
			cbox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cbox.isSelected()) {
						selectedVersion.setEepromPath(eepromPath);
					} else {
						selectedVersion.setEepromPath(null);
					}
				}
			});
			panel.add(cbox,"wrap");
			final String text = "Overwriting your eeprom will erase any changes you have made to your board settings.  If "+
			"you've made changes you want to keep, either uncheck this box or note your parameters before overwriting.";
			panel.add(new JLabel("<html><i>"+text+"</i></html>"),"wrap");
		}
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				performUpload(uploader);
			}
		});
		showPanel(panel);
	}

	void performUpload(AbstractFirmwareUploader uploader) {
		if (uploader != null) {
			uploader.setPortName(portName);
			// This should be abstracted out; the uploader should just get the firmware node and
			// decide what to do with it instead of making dialog pull out the data.
			String path = Base.getUserFile(selectedVersion.getRelPath()).getAbsolutePath();
			uploader.setSource(path);
			if (uploader instanceof AvrdudeUploader) {
				if (selectedVersion.getEepromPath() != null) {
					String eepromPath = Base.getUserFile(selectedVersion.getEepromPath()).getAbsolutePath();
					((AvrdudeUploader)uploader).setEeprom(eepromPath);
				}
			}
			boolean success = uploader.upload();
			if (success) {
				JOptionPane.showMessageDialog(this, 
						"固件上传成功！", 
						"固件已上传",
						JOptionPane.INFORMATION_MESSAGE);
				doCancel();
			} else {
				JOptionPane.showMessageDialog(this, 
						"<html>固件上传失败。详细信息请参见面板。<br/>"+
						"您可以按前述步骤再试几次。</html>", 
						"上传失败",
						JOptionPane.ERROR_MESSAGE);
				
			}
		}
	}
}
