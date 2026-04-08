package pcd.assignment01.view;

import java.awt.event.KeyListener;

public class View {

	private ViewFrame frame;
	private ViewModel viewModel;

	public View(ViewModel model, int w, int h) {
		frame = new ViewFrame(model, w, h);
		frame.setVisible(true);
		this.viewModel = model;
	}

	public void render() {
		frame.render();
	}

	public ViewModel getViewModel() {
		return viewModel;
	}

	// Permette a Sketch01 di ascoltare la tastiera
	public void addKeyListener(KeyListener listener) {
		frame.addKeyListener(listener);
		frame.setFocusable(true);
		frame.requestFocusInWindow();
	}
}