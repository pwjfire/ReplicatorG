package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.j3d.Transform3D;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class MoveTool implements Tool, MouseMotionListener, MouseListener, MouseWheelListener {
	final ToolPanel parent;
	public MoveTool(ToolPanel parent) {
		this.parent = parent;
	}
	
	Transform3D vt;

	public Icon getButtonIcon() {
		return null;
	}

	public String getButtonName() {
		return "Move";
	}

	public JPanel getControls() {
		return null;
	}

	public String getInstructions() {
		return null;
	}

	public String getTitle() {
		return "Move Object";
	}

	Point startPoint = null;
	int button = 0;

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.ROTATE_VIEW; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.TRANSLATE_OBJECT; }
			else if (button == MouseEvent.BUTTON1 && e.isShiftDown()) { mode = DragMode.TRANSLATE_OBJECT; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.TRANSLATE_OBJECT; }
			else if (button == MouseEvent.BUTTON3) { mode = DragMode.TRANSLATE_OBJECT; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		switch (mode) {
		case ROTATE_OBJECT:
			break;
		case TRANSLATE_OBJECT:
			doTranslate(xd,yd);
			break;
		}
		startPoint = p;
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	
	double objectDistance;
	
	public void mousePressed(MouseEvent e) {
		startPoint = e.getPoint();
		button = e.getButton();
		// Set up view transform
		vt = parent.preview.getViewTransform();
		// Scale view transform to account for object distance
		Point3d centroid = parent.getModel().getCentroid();
		vt.transform(centroid);
		objectDistance = centroid.distance(new Point3d());
	}
	public void mouseReleased(MouseEvent e) {
		startPoint = null;
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		parent.preview.adjustZoom(0.10 * notches);
	}
	
	void doTranslate(double deltaX, double deltaY) {
		Vector3d v = new Vector3d(deltaX,deltaY,0d);
		vt.transform(v);
		parent.getModel().translateObject(v.x,v.y,v.z);
	}
}
