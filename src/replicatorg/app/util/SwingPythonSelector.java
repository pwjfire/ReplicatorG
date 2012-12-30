package replicatorg.app.util;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.util.PythonUtils.Selector;

public class SwingPythonSelector implements Selector {
	private Frame frame;
	
	public SwingPythonSelector(Frame frame) {
		this.frame = frame;
	}
	
	public String selectPythonPath(Vector<String> candidates) {
		if (candidates != null && candidates.size() >= 2) {
			return selectCandidatePath(candidates);
		} else {
			// Linux users should have zero problems setting up python on their system or putting it in
			// the path.  Even n00bs.  Thank you, Debian/Ubuntu. 
			if (candidates != null && Base.isLinux()) { return null; }
			String s = "<html>"+
				"<p>程序在您的电脑上无法找到Python解释器。</p>"+
				"<p>访问Python下载页面或手动指定您电脑上的Python安装位置？</p>"+
				"</html>";
			Object[] options = {
					"转到Python网址",
					"选择Python",
					"取消"
			};
			int r = JOptionPane.showOptionDialog(frame,
				    s,
				    "Python not found",
				    JOptionPane.YES_NO_CANCEL_OPTION,
				    JOptionPane.QUESTION_MESSAGE,
				    null,
				    options,
				    options[0]); 
			
			if (r == JOptionPane.YES_OPTION) {
				Base.openURL("http://python.org/download");
			} 
			if (r == JOptionPane.NO_OPTION) {
				return selectFreeformPath();
			}
			return null;
		}
	}
	
	private String selectedCandidate = null;
	
	private String selectCandidatePath(Vector<String> candidates) {
		final JDialog dialog = new JDialog(frame, "选择Python", true);
		Container content = dialog.getContentPane();
		content.setLayout(new MigLayout());
		String msg = "<html>在您的电脑上发现了多个Python安装包。<br>"+
			"从下面的列表中选择一个，或点击'其他...'来找到其他版本。</html>";
		content.add(new JLabel(msg),"growx,wrap");
		final JList list = new JList(candidates);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		content.add(list,"growx,wrap");
		selectedCandidate = null;
		JButton ok = new JButton("确认");
		JButton cancel = new JButton("取消");
		JButton other = new JButton("其他...");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectedCandidate = (String)list.getSelectedValue();
				dialog.setVisible(false);
			}
		});
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectedCandidate = null;
				dialog.setVisible(false);
			}
		});
		other.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dialog.setVisible(false);
				selectedCandidate = selectFreeformPath();
			}
		});
		dialog.add(other,"wrap,wmin button");
		dialog.add(ok,"tag ok,wmin button");
		dialog.add(cancel,"tag cancel,wmin button");
		dialog.pack();
		dialog.setVisible(true);
		return selectedCandidate;
	}
	
	public String selectFreeformPath() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("选择已安装的Python安装包");
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		if (chooser.showOpenDialog(this.frame) == JFileChooser.APPROVE_OPTION) {
			File chosen = chooser.getSelectedFile();
			if (chosen != null) {
				return chosen.getAbsolutePath();
			}
		}
		return null;
	}

}
