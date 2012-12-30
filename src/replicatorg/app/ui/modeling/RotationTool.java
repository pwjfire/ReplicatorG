package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.vecmath.AxisAngle4d;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class RotationTool extends Tool {
	public RotationTool(ToolPanel parent) {
		super(parent);
	}
	
	@Override
	Icon getButtonIcon() {
		return null;
	}

	@Override
	String getButtonName() {
		return "旋转";
	}

	JCheckBox lockZ;
	
	@Override
	JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,gap 0,wrap 2","[50%]0[50%]"));
		JButton b;

		b = createToolButton("Z+","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(0d, 0d, 1d, Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("Z-","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(0d, 0d, 1d, -Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("X+","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(1d, 0d, 0d, Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("X-","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(1d, 0d, 0d, -Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("Y+","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(0d, 1d, 0d, Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("Y-","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().rotateObject(new AxisAngle4d(0d, 1d, 0d, -Math.PI/2));
			}
		});
		p.add(b,"growx");

		b = createToolButton("放平","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().layFlat();
			}
		});
		p.add(b,"growx,spanx");

		lockZ = new JCheckBox("绕Z轴旋转");
		p.add(lockZ,"growx,spanx");

		return p;
	}

	@Override
	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>拖拽来旋转视图<br>按住Shift的同时拖拽来旋转视图<br>滚轮来缩放视图</body></html>":
				"<html><body>左键拖拽来旋转物体<br>右键拖拽来旋转视图<br>滚轮来缩放视图</body></html>";
	}

	@Override
	String getTitle() {
		return "旋转物体";
	}
	
	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.NONE;
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.ROTATE_OBJECT; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.ROTATE_OBJECT; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		if (lockZ.isSelected()) { yd = 0; }
		switch (mode) {
		case ROTATE_OBJECT:
			parent.getModel().rotateObject(0.05*xd, -0.05*yd);
			break;
		case NONE:
			super.mouseDragged(e);
			break;
		}
		startPoint = p;
	}

}
