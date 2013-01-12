package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.Profile;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.PrintOMatic5D;

class ConfigurationDialog extends JDialog {
	final boolean postProcessToolheadIndex = true;
	final String profilePref = "replicatorg.skeinforge.profilePref";
	
	JButton generateButton = new JButton("生成G代码");
	JButton cancelButton = new JButton("取消");
	
	/* these must be explicitly nulled at close because of a java bug:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6497929
	 * 
	 * because JDialogs may never be garbage collected, anything they keep reference to 
	 * may never be gc'd. By explicitly nulling these in the setVisible() function
	 * we allow them to be removed.
	 */
	private SkeinforgeGenerator parentGenerator = null;
	private List<Profile> profiles = null;
	
	JPanel profilePanel = new JPanel();
	
	/**
	 * Fills a combo box with a list of skeinforge profiles
	 * @param comboBox to fill with list of skeinforge profiles
	 */
	private void loadList(JComboBox comboBox) {
		
		comboBox.removeAllItems();
		profiles = parentGenerator.getProfiles();
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		int i=0;
		int selectedProfile = -1;
		for (Profile p : profiles) {
			///we display all profiles for all machines.
			// at MBI customer support's request.
			model.addElement(p.toString());
			
			if(p.toString().equals(Base.preferences.get("lastGeneratorProfileSelected","---")))
			{
				Base.logger.fine("Selecting last used element: " + p);
				/// default select the last profile that matches 
				// the currently selected machine type
				if(ProfileUtils.shouldDisplay(p)) {
					selectedProfile = i;
				}
			}
			i++;
		}
		comboBox.setModel(model);
		if(selectedProfile != -1) {
			comboBox.setSelectedIndex(selectedProfile);
		}
	}

	/**
	 * Help reduce effects of miserable memory leak.
	 * see declarations above.
	 */
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(!b)
		{
			parentGenerator = null;
			profiles = null;
		}
	}

	final JComboBox prefPulldown = new JComboBox();

	public ConfigurationDialog(final Frame parent, final SkeinforgeGenerator parentGeneratorIn) {
		super(parent, true);

		parentGenerator = parentGeneratorIn;
		setTitle("GCode Generator");
		setLayout(new MigLayout("aligny, top, ins 5, fill"));
		
		add(new JLabel("切片配置:"), "split 2");
		
		// This is intended to fix a bug where the "Generate Gcode"
		// button doesn't get enabled 
		prefPulldown.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				generateButton.setEnabled(true);
				generateButton.requestFocusInWindow();
				generateButton.setFocusPainted(true);
			}
		});
		
		/// Fills UI with the list of Skeinforge settings/options
		loadList(prefPulldown); 
		add(prefPulldown, "wrap, growx, gapbottom 10");


		for (SkeinforgePreference preference: parentGenerator.getPreferences()) {
			add(preference.getUI(), "growx, wrap");
		}
		
		generateButton.setToolTipText("为您的机器生成G代码.");
		
		add(generateButton, "tag ok, split 2");
		add(cancelButton, "tag cancel");

		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0)
			{
				checkIfAccelSpeeds(parent);
				parentGenerator.configSuccess = configureGenerator();
				setVisible(!parentGenerator.configSuccess);
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parentGenerator.configSuccess = false;
				setVisible(false);
			}
		});

	}

	private void checkIfAccelSpeeds(Frame parent)
	//Checks if speeds are acceleration level and if so displays a very nice message,
	//so that people will (hopefully) not shake their machines off their desks
	{
		double travel_rate = 0;
		double feed_rate = 0;

		//Parses through Skeinforge options and grabs the two feed speeds
		for (SkeinforgePreference preference: parentGenerator.getPreferences()) {
			List<SkeinforgeOption> options = preference.getOptions("Skeinforge (35) - Legacy");
			if (options != null) {
				for(SkeinforgeOption option: options) {
					if(option.getPreference().equals("Travel Feed Rate (mm/s):")){
						travel_rate = Double.parseDouble(option.getValue());
					}
					if(option.getPreference().equals("Feed Rate (mm/s):")){
							feed_rate = Double.parseDouble(option.getValue());
					}
				}
			}
		}
			System.out.println("\n**FEED_RATES**\n" + "desired:" + feed_rate + "travel:" + travel_rate);

			if((feed_rate > 40) || (travel_rate > 55))
			{ 
        if(Base.preferences.getBoolean("build.speed_warning", true)){
				JOptionPane.showMessageDialog(parent,"您现在按加速的设置来生成G代码。\n" +
            		"如果您没有打开加速开关，请不要按这个设置来生成。\n" +
            		"在关闭加速开关的情况下，生成高速的G代码对机器有损害。\n\n" +
					"您可以在打印机->电路板参数菜单项中打开或关闭加速功能。\n\n" +
          "可以在文件->参数菜单项中，取消'显示加速警告'选项来关闭此警告对话框",
					"加速警告", JOptionPane.WARNING_MESSAGE);
        }
			}
	}
	
	/**
	 * Does pre-skeinforge generation tasks
	 */
	protected boolean configureGenerator()
	{
		if(!parentGenerator.runSanityChecks()) {
			return false;
		}
		
		int idx = prefPulldown.getSelectedIndex();
		
		if(idx == -1) {
			return false;
		}
		
		Profile p = ProfileUtils.getListedProfile(
				prefPulldown.getModel(), profiles, idx);
		Base.preferences.put("lastGeneratorProfileSelected",p.toString());
		parentGenerator.profile = p.getFullPath();
		SkeinforgeGenerator.setSelectedProfile(p.toString());
		return true;
	}
};
